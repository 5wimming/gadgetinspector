package gadgetinspector;

import gadgetinspector.data.ClassReference;
import gadgetinspector.data.DataLoader;
import gadgetinspector.data.InheritanceDeriver;
import gadgetinspector.data.MethodReference;
import java.util.HashSet;
import java.util.Set;
import org.objectweb.asm.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MethodDiscovery {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodDiscovery.class);

    private final List<ClassReference> discoveredClasses = new ArrayList<>();
    private final List<MethodReference> discoveredMethods = new ArrayList<>();

    public void save() throws IOException {
        //保存和读取使用Factory实现

        //classes.dat数据格式：
        //类名(例：java/lang/String) 父类 接口A,接口B,接口C 是否接口 字段1!字段1access!字段1类型!字段2!字段2access!字段1类型
        DataLoader.saveData(Paths.get("classes.dat"), new ClassReference.Factory(), discoveredClasses);

        //methods.dat数据格式：
        //类名 方法名 方法描述 是否静态方法
        DataLoader.saveData(Paths.get("methods.dat"), new MethodReference.Factory(), discoveredMethods);

        //形成 类名(ClassReference.Handle)->类(ClassReference) 的映射关系
        Map<ClassReference.Handle, ClassReference> classMap = new HashMap<>();
        for (ClassReference clazz : discoveredClasses) {
            classMap.put(clazz.getHandle(), clazz);
        }
        //保存classes.dat和methods.dat的同时，对所有的class进行递归整合，得到集合{class:[subclass]}，
        // class为subclass父类、超类或实现的接口类，保存至inheritanceMap.dat
        InheritanceDeriver.derive(classMap).save();
    }

    public void discover(final ClassResourceEnumerator classResourceEnumerator) throws Exception {
        for (ClassResourceEnumerator.ClassResource classResource : classResourceEnumerator.getAllClasses()) {
            try (InputStream in = classResource.getInputStream()) {
                ClassReader cr = new ClassReader(in);
                try {
                    //使用asm的ClassVisitor、MethodVisitor，利用观察模式去扫描所有的class和method并记录
                    cr.accept(new MethodDiscoveryClassVisitor(), ClassReader.EXPAND_FRAMES);
                } catch (Exception e) {
                    LOGGER.error("Exception analyzing: " + classResource.getName(), e);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    class MethodAnnotationScanner extends MethodVisitor{
        private ClassReference.Handle myClassHandle;
        private String name;
        private String desc;
        private boolean isStatic;
        private String parameterAnnotationDesc = "";
        private String methodAnnotationDesc = "";

        public MethodAnnotationScanner() {
            super(Opcodes.ASM6);
        }

        public MethodAnnotationScanner(ClassReference.Handle myClassHandle, String name, String desc, boolean isStatic) {
            super(Opcodes.ASM6);
            this.myClassHandle = myClassHandle;
            this.name = name;
            this.desc = desc;
            this.isStatic = isStatic;
        }

        @Override
        public AnnotationVisitor visitParameterAnnotation(final int parameter, final String descriptor, final boolean visible) {
            if (mv != null) {
                return mv.visitParameterAnnotation(parameter, descriptor, visible);
            }
            this.parameterAnnotationDesc += descriptor;
            return null;
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            this.methodAnnotationDesc += desc;
            return super.visitAnnotation(desc, visible);
        }

        @Override
        public void visitEnd() {
            if (mv != null) {
                mv.visitEnd();
            }
            this.methodAnnotationDesc = this.methodAnnotationDesc.length()==0?"none":this.methodAnnotationDesc;
            this.parameterAnnotationDesc = this.parameterAnnotationDesc.length()==0?"none":this.parameterAnnotationDesc;
            discoveredMethods.add(new MethodReference(
                    this.myClassHandle,//类名
                    this.name,
                    this.desc,
                    this.isStatic,
                    this.methodAnnotationDesc,
                    this.parameterAnnotationDesc));
        }
    }

    private class MethodDiscoveryClassVisitor extends ClassVisitor {

        private String name;
        private String superName;
        private String[] interfaces;
        boolean isInterface;
        private List<ClassReference.Member> members;//类的所有字段
        private ClassReference.Handle classHandle;
        private Set<String> annotations;

        private MethodDiscoveryClassVisitor() throws SQLException {
            super(Opcodes.ASM6);
        }

        @Override
        public void visit ( int version, int access, String name, String signature, String superName, String[]interfaces)
        {
            this.name = name;
            this.superName = superName;
            this.interfaces = interfaces;
            this.isInterface = (access & Opcodes.ACC_INTERFACE) != 0;
            this.members = new ArrayList<>();
            this.classHandle = new ClassReference.Handle(name);//类名
            annotations = new HashSet<>();
            super.visit(version, access, name, signature, superName, interfaces);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            annotations.add(descriptor);
            return super.visitAnnotation(descriptor, visible);
        }

        public FieldVisitor visitField(int access, String name, String desc,
                                       String signature, Object value) {
            if ((access & Opcodes.ACC_STATIC) == 0) {
                Type type = Type.getType(desc);
                String typeName;
                if (type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY) {
                    typeName = type.getInternalName();
                } else {
                    typeName = type.getDescriptor();
                }
                members.add(new ClassReference.Member(name, access, new ClassReference.Handle(typeName)));
            }
            return super.visitField(access, name, desc, signature, value);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            boolean isStatic = (access & Opcodes.ACC_STATIC) != 0;

            //return super.visitMethod(access, name, desc, signature, exceptions);
            return new MethodAnnotationScanner(classHandle, name, desc, isStatic);
        }

        @Override
        public void visitEnd() {
            ClassReference classReference = new ClassReference(
                    name,
                    superName,
                    interfaces,
                    isInterface,
                    members.toArray(new ClassReference.Member[members.size()]),
                    annotations);//把所有找到的字段封装
            //找到一个方法遍历完成后，添加类到缓存
            discoveredClasses.add(classReference);

            super.visitEnd();
        }

    }

    public static void main(String[] args) throws Exception {
        ClassLoader classLoader = Util.getWarClassLoader(Paths.get(args[0]));

        MethodDiscovery methodDiscovery = new MethodDiscovery();
        methodDiscovery.discover(new ClassResourceEnumerator(classLoader));
        methodDiscovery.save();
    }
}
