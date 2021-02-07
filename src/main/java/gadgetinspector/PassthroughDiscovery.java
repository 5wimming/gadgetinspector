package gadgetinspector;

import gadgetinspector.config.GIConfig;
import gadgetinspector.config.JavaDeserializationConfig;
import gadgetinspector.data.*;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.JSRInlinerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.*;

public class PassthroughDiscovery {

    private static final Logger LOGGER = LoggerFactory.getLogger(PassthroughDiscovery.class);

    //{{sourceClass,sourceMethod}:[{targetClass,targetMethod}]}，收集哪个class的method调用了哪一个class的method关系集合
    private final Map<MethodReference.Handle, Set<MethodReference.Handle>> methodCalls = new HashMap<>();
    private Map<MethodReference.Handle, Set<Integer>> passthroughDataflow;

    public void discover(final ClassResourceEnumerator classResourceEnumerator, final GIConfig config) throws IOException {
        //加载文件记录的所有方法信息
        Map<MethodReference.Handle, MethodReference> methodMap = DataLoader.loadMethods();
        //加载文件记录的所有类信息
        Map<ClassReference.Handle, ClassReference> classMap = DataLoader.loadClasses();
        //加载文件记录的所有类继承、实现关联信息
        InheritanceMap inheritanceMap = InheritanceMap.load();

        //搜索方法间的调用关系，缓存至methodCalls集合，返回 类名->类资源 映射集合
        Map<String, ClassResourceEnumerator.ClassResource> classResourceByName = discoverMethodCalls(classResourceEnumerator);
        //对方法调用关系进行字典排序
        List<MethodReference.Handle> sortedMethods = topologicallySortMethodCalls();
        /**
         * classResourceByName：类资源集合
         * classMap：类信息集合
         * inheritanceMap：继承、实现关系集合
         * sortedMethods：方法集合
         * SerializableDecider：决策者
         */
        passthroughDataflow = calculatePassthroughDataflow(classResourceByName, classMap, inheritanceMap, sortedMethods,
                config.getSerializableDecider(methodMap, inheritanceMap));
    }

    /**
     * 搜索method调用关联信息
     *
     * @param classResourceEnumerator
     * @return
     * @throws IOException
     */
    private Map<String, ClassResourceEnumerator.ClassResource> discoverMethodCalls(final ClassResourceEnumerator classResourceEnumerator) throws IOException {
        Map<String, ClassResourceEnumerator.ClassResource> classResourcesByName = new HashMap<>();
        for (ClassResourceEnumerator.ClassResource classResource : classResourceEnumerator.getAllClasses()) {
            try (InputStream in = classResource.getInputStream()) {
                ClassReader cr = new ClassReader(in);
                try {
                    MethodCallDiscoveryClassVisitor visitor = new MethodCallDiscoveryClassVisitor(Opcodes.ASM6);
                    cr.accept(visitor, ClassReader.EXPAND_FRAMES);
                    classResourcesByName.put(visitor.getName(), classResource);
                } catch (Exception e) {
                    LOGGER.error("Error analyzing: " + classResource.getName(), e);
                }
            }
        }
        return classResourcesByName;
    }

    /**
     * 对收集到的method关联信息，进行method名称的字典排序
     *
     * @return
     */
    private List<MethodReference.Handle> topologicallySortMethodCalls() {
        Map<MethodReference.Handle, Set<MethodReference.Handle>> outgoingReferences = new HashMap<>();
        for (Map.Entry<MethodReference.Handle, Set<MethodReference.Handle>> entry : methodCalls.entrySet()) {
            MethodReference.Handle method = entry.getKey();
            outgoingReferences.put(method, new HashSet<>(entry.getValue()));
        }

        // Topological sort methods
        LOGGER.debug("Performing topological sort...");
        Set<MethodReference.Handle> dfsStack = new HashSet<>();
        Set<MethodReference.Handle> visitedNodes = new HashSet<>();
        List<MethodReference.Handle> sortedMethods = new ArrayList<>(outgoingReferences.size());
        for (MethodReference.Handle root : outgoingReferences.keySet()) {
            //遍历集合中的起始方法，进行递归搜索DFS，通过逆拓扑排序，调用链的最末端排在最前面，
            // 这样才能实现入参、返回值、函数调用链之间的污点影响
            dfsTsort(outgoingReferences, sortedMethods, visitedNodes, dfsStack, root);
        }
        LOGGER.debug(String.format("Outgoing references %d, sortedMethods %d", outgoingReferences.size(), sortedMethods.size()));

        return sortedMethods;
    }

    /**
     * 发现方法返回值，也即和入参有关联的返回值，用于分析污染链路
     *
     * @param classResourceByName 类资源集合
     * @param classMap            类信息集合
     * @param inheritanceMap      继承、实现关系集合
     * @param sortedMethods       方法集合
     * @param serializableDecider 决策者
     * @return
     * @throws IOException
     */
    private static Map<MethodReference.Handle, Set<Integer>> calculatePassthroughDataflow(Map<String, ClassResourceEnumerator.ClassResource> classResourceByName,
                                                                                          Map<ClassReference.Handle, ClassReference> classMap,
                                                                                          InheritanceMap inheritanceMap,
                                                                                          List<MethodReference.Handle> sortedMethods,
                                                                                          SerializableDecider serializableDecider) throws IOException {
        final Map<MethodReference.Handle, Set<Integer>> passthroughDataflow = new HashMap<>();
        //遍历所有方法，然后asm观察所属类，经过前面DFS的排序，调用链最末端的方法在最前面
        for (MethodReference.Handle method : sortedMethods) {
            //跳过static静态初始化代码
            if (method.getName().equals("<clinit>")) {
                continue;
            }
            //获取所属类进行观察
            ClassResourceEnumerator.ClassResource classResource = classResourceByName.get(method.getClassReference().getName());
            try (InputStream inputStream = classResource.getInputStream()) {
                ClassReader cr = new ClassReader(inputStream);
                try {
                    PassthroughDataflowClassVisitor cv = new PassthroughDataflowClassVisitor(classMap, inheritanceMap,
                            passthroughDataflow, serializableDecider, Opcodes.ASM6, method);
                    cr.accept(cv, ClassReader.EXPAND_FRAMES);
                    passthroughDataflow.put(method, cv.getReturnTaint());//缓存方法返回值与哪个参数有关系
                } catch (Exception e) {
                    LOGGER.error("Exception analyzing " + method.getClassReference().getName(), e);
                }
            } catch (IOException e) {
                LOGGER.error("Unable to analyze " + method.getClassReference().getName(), e);
            }
        }
        return passthroughDataflow;
    }

    private class MethodCallDiscoveryClassVisitor extends ClassVisitor {
        public MethodCallDiscoveryClassVisitor(int api) {
            super(api);
        }

        private String name = null;

        @Override
        public void visit(int version, int access, String name, String signature,
                          String superName, String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces);
            if (this.name != null) {
                throw new IllegalStateException("ClassVisitor already visited a class!");
            }
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc,
                                         String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            //在visit每个method的时候，创建MethodVisitor对method进行观察
            MethodCallDiscoveryMethodVisitor modelGeneratorMethodVisitor = new MethodCallDiscoveryMethodVisitor(
                    api, mv, this.name, name, desc);

            return new JSRInlinerAdapter(modelGeneratorMethodVisitor, access, name, desc, signature, exceptions);
        }

        @Override
        public void visitEnd() {
            super.visitEnd();
        }
    }

    private class MethodCallDiscoveryMethodVisitor extends MethodVisitor {
        private final Set<MethodReference.Handle> calledMethods;

        /**
         *
         * @param api
         * @param mv
         * @param owner 上一步ClassVisitor在visitMethod时，传入的当前class
         * @param name visit的方法名
         * @param desc visit的方法描述
         */
        public MethodCallDiscoveryMethodVisitor(final int api, final MethodVisitor mv,
                                           final String owner, String name, String desc) {
            super(api, mv);

            //创建calledMethod收集调用到的method，最后形成集合{{sourceClass,sourceMethod}:[{targetClass,targetMethod}]}
            this.calledMethods = new HashSet<>();
            methodCalls.put(new MethodReference.Handle(new ClassReference.Handle(owner), name, desc), calledMethods);
        }

        /**
         * 方法内，每一个方法调用都会执行该方法
         *
         * @param opcode 调用操作码：INVOKEVIRTUAL, INVOKESPECIAL, INVOKESTATIC or INVOKEINTERFACE.
         * @param owner 被调用的类名
         * @param name 被调用的方法
         * @param desc 被调用方法的描述
         * @param itf 被调用的类是否接口
         */
        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            calledMethods.add(new MethodReference.Handle(new ClassReference.Handle(owner), name, desc));
            super.visitMethodInsn(opcode, owner, name, desc, itf);
        }
    }

    public void save() throws IOException {
        if (passthroughDataflow == null) {
            throw new IllegalStateException("Save called before discover()");
        }

        DataLoader.saveData(Paths.get("passthrough.dat"), new PassThroughFactory(), passthroughDataflow.entrySet());
    }

    public static Map<MethodReference.Handle, Set<Integer>> load() throws IOException {
        Map<MethodReference.Handle, Set<Integer>> passthroughDataflow = new HashMap<>();
        for (Map.Entry<MethodReference.Handle, Set<Integer>> entry : DataLoader.loadData(Paths.get("passthrough.dat"), new PassThroughFactory())) {
            passthroughDataflow.put(entry.getKey(), entry.getValue());
        }
        return passthroughDataflow;
    }

    public static class PassThroughFactory implements DataFactory<Map.Entry<MethodReference.Handle, Set<Integer>>> {

        @Override
        public Map.Entry<MethodReference.Handle, Set<Integer>> parse(String[] fields) {
            ClassReference.Handle clazz = new ClassReference.Handle(fields[0]);
            MethodReference.Handle method = new MethodReference.Handle(clazz, fields[1], fields[2]);

            Set<Integer> passthroughArgs = new HashSet<>();
            for (String arg : fields[3].split(",")) {
                if (arg.length() > 0) {
                    passthroughArgs.add(Integer.parseInt(arg));
                }
            }
            return new AbstractMap.SimpleEntry<>(method, passthroughArgs);
        }

        @Override
        public String[] serialize(Map.Entry<MethodReference.Handle, Set<Integer>> entry) {
            if (entry.getValue().size() == 0) {
                return null;
            }

            final String[] fields = new String[4];
            fields[0] = entry.getKey().getClassReference().getName();
            fields[1] = entry.getKey().getName();
            fields[2] = entry.getKey().getDesc();

            StringBuilder sb = new StringBuilder();
            for (Integer arg : entry.getValue()) {
                sb.append(Integer.toString(arg));
                sb.append(",");
            }
            fields[3] = sb.toString();

            return fields;
        }
    }

    private static void dfsTsort(Map<MethodReference.Handle, Set<MethodReference.Handle>> outgoingReferences,
                                    List<MethodReference.Handle> sortedMethods, Set<MethodReference.Handle> visitedNodes,
                                    Set<MethodReference.Handle> stack, MethodReference.Handle node) {

        if (stack.contains(node)) {
            return;
        }
        if (visitedNodes.contains(node)) {
            return;
        }
        //根据起始方法，取出被调用的方法集
        Set<MethodReference.Handle> outgoingRefs = outgoingReferences.get(node);
        if (outgoingRefs == null) {
            return;
        }

        //入栈，以便于递归不造成类似循环引用的死循环整合
        stack.add(node);
        for (MethodReference.Handle child : outgoingRefs) {
            dfsTsort(outgoingReferences, sortedMethods, visitedNodes, stack, child);
        }
        stack.remove(node);
        visitedNodes.add(node);//记录已被探索过的方法，用于在上层调用遇到重复方法时可以跳过
        sortedMethods.add(node);//递归完成的探索，会添加进来
    }

    private static class PassthroughDataflowClassVisitor extends ClassVisitor {

        Map<ClassReference.Handle, ClassReference> classMap;//类信息集合
        private final MethodReference.Handle methodToVisit;//要观察的方法
        private final InheritanceMap inheritanceMap;//继承、实现关系集合
        private final Map<MethodReference.Handle, Set<Integer>> passthroughDataflow;
        private final SerializableDecider serializableDecider;//决策者

        private String name;
        private PassthroughDataflowMethodVisitor passthroughDataflowMethodVisitor;

        public PassthroughDataflowClassVisitor(Map<ClassReference.Handle, ClassReference> classMap,
                InheritanceMap inheritanceMap, Map<MethodReference.Handle, Set<Integer>> passthroughDataflow,
                SerializableDecider serializableDecider, int api, MethodReference.Handle methodToVisit) {
            super(api);
            this.classMap = classMap;
            this.inheritanceMap = inheritanceMap;
            this.methodToVisit = methodToVisit;
            this.passthroughDataflow = passthroughDataflow;
            this.serializableDecider = serializableDecider;
        }

        @Override
        public void visit(int version, int access, String name, String signature,
                          String superName, String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces);
            this.name = name;
            //不是目标观察的class跳过
            if (!this.name.equals(methodToVisit.getClassReference().getName())) {
                throw new IllegalStateException("Expecting to visit " + methodToVisit.getClassReference().getName() + " but instead got " + this.name);
            }
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc,
                                         String signature, String[] exceptions) {
            //不是目标观察的method需要跳过，上一步得到的method都是有调用关系的method才需要数据流分析
            if (!name.equals(methodToVisit.getName()) || !desc.equals(methodToVisit.getDesc())) {
                return null;
            }
            if (passthroughDataflowMethodVisitor != null) {
                throw new IllegalStateException("Constructing passthroughDataflowMethodVisitor twice!");
            }

            //对method进行观察
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            passthroughDataflowMethodVisitor = new PassthroughDataflowMethodVisitor(
                    classMap, inheritanceMap, this.passthroughDataflow, serializableDecider,
                    api, mv, this.name, access, name, desc, signature, exceptions);

            return new JSRInlinerAdapter(passthroughDataflowMethodVisitor, access, name, desc, signature, exceptions);
        }

        public Set<Integer> getReturnTaint() {
            if (passthroughDataflowMethodVisitor == null) {
                throw new IllegalStateException("Never constructed the passthroughDataflowmethodVisitor!");
            }
            return passthroughDataflowMethodVisitor.returnTaint;
        }
    }

    private static class PassthroughDataflowMethodVisitor extends TaintTrackingMethodVisitor<Integer> {

        private final Map<ClassReference.Handle, ClassReference> classMap;//类信息集合
        private final InheritanceMap inheritanceMap;//继承、实现关系集合
        private final Map<MethodReference.Handle, Set<Integer>> passthroughDataflow;
        private final SerializableDecider serializableDecider;//决策者

        private final int access;
        private final String desc;
        private final Set<Integer> returnTaint;//被污染的返回数据

        public PassthroughDataflowMethodVisitor(Map<ClassReference.Handle, ClassReference> classMap,
                InheritanceMap inheritanceMap, Map<MethodReference.Handle,
                Set<Integer>> passthroughDataflow, SerializableDecider serializableDeciderMap, int api, MethodVisitor mv,
                String owner, int access, String name, String desc, String signature, String[] exceptions) {
            super(inheritanceMap, passthroughDataflow, api, mv, owner, access, name, desc, signature, exceptions);
            this.classMap = classMap;
            this.inheritanceMap = inheritanceMap;
            this.passthroughDataflow = passthroughDataflow;
            this.serializableDecider = serializableDeciderMap;
            this.access = access;
            this.desc = desc;
            returnTaint = new HashSet<>();
        }

        @Override
        public void visitCode() {
            super.visitCode();

            int localIndex = 0;
            int argIndex = 0;
            if ((this.access & Opcodes.ACC_STATIC) == 0) {
                //非静态方法，第一个局部变量应该为对象实例this
                //添加到本地变量表集合
                setLocalTaint(localIndex, argIndex);
                localIndex += 1;
                argIndex += 1;
            }
            for (Type argType : Type.getArgumentTypes(desc)) {
                //判断参数类型，得出变量占用空间大小，然后存储
                setLocalTaint(localIndex, argIndex);
                localIndex += argType.getSize();
                argIndex += 1;
            }
        }

        @Override
        public void visitInsn(int opcode) {
            switch(opcode) {
                case Opcodes.IRETURN://从当前方法返回int
                case Opcodes.FRETURN://从当前方法返回float
                case Opcodes.ARETURN://从当前方法返回对象引用
                    returnTaint.addAll(getStackTaint(0));//栈空间从内存高位到低位分配空间
                    break;
                case Opcodes.LRETURN://从当前方法返回long
                case Opcodes.DRETURN://从当前方法返回double
                    returnTaint.addAll(getStackTaint(1));
                    break;
                case Opcodes.RETURN://从当前方法返回void
                    break;
                default:
                    break;
            }

            super.visitInsn(opcode);
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String desc) {

            switch (opcode) {
                case Opcodes.GETSTATIC:
                    break;
                case Opcodes.PUTSTATIC:
                    break;
                case Opcodes.GETFIELD:
                    Type type = Type.getType(desc);//获取字段类型
                    if (type.getSize() == 1) {
                        //size=1可能为引用类型
                        Boolean isTransient = null;

                        // If a field type could not possibly be serialized, it's effectively transient
                        //判断调用的字段类型是否可序列化
                        if (!couldBeSerialized(serializableDecider, inheritanceMap, new ClassReference.Handle(type.getInternalName()))) {
                            isTransient = Boolean.TRUE;
                        } else {
                            //若调用的字段可被序列化，则取当前类实例的所有字段，找出调用的字段，去判断是否被标识了transient
                            ClassReference clazz = classMap.get(new ClassReference.Handle(owner));
                            while (clazz != null) {
                                //遍历字段，判断是否是transient类型，以确定是否可被序列化
                                for (ClassReference.Member member : clazz.getMembers()) {
                                    if (member.getName().equals(name)) {
                                        isTransient = (member.getModifiers() & Opcodes.ACC_TRANSIENT) != 0;
                                        break;
                                    }
                                }
                                if (isTransient != null) {
                                    break;
                                }
                                //若找不到字段，则向上父类查找，继续遍历
                                clazz = classMap.get(new ClassReference.Handle(clazz.getSuperClass()));
                            }
                        }

                        Set<Integer> taint;
                        if (!Boolean.TRUE.equals(isTransient)) {
                            //若不是Transient字段，则从栈顶取出它，取出的是this或某实例变量，即字段所属实例
                            taint = getStackTaint(0);
                        } else {
                            taint = new HashSet<>();
                        }

                        super.visitFieldInsn(opcode, owner, name, desc);
                        setStackTaint(0, taint);
                        return;
                    }
                    break;
                case Opcodes.PUTFIELD:
                    break;
                default:
                    throw new IllegalStateException("Unsupported opcode: " + opcode);
            }

            super.visitFieldInsn(opcode, owner, name, desc);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            //获取method参数类型
            Type[] argTypes = Type.getArgumentTypes(desc);
            if (opcode != Opcodes.INVOKESTATIC) {
                //如果执行的非静态方法，则把数组第一个元素类型设置为该实例对象的类型，类比局部变量表
                Type[] extendedArgTypes = new Type[argTypes.length+1];
                System.arraycopy(argTypes, 0, extendedArgTypes, 1, argTypes.length);
                extendedArgTypes[0] = Type.getObjectType(owner);
                argTypes = extendedArgTypes;
            }
            //获取返回值类型大小
            int retSize = Type.getReturnType(desc).getSize();

            Set<Integer> resultTaint;
            switch (opcode) {
                case Opcodes.INVOKESTATIC://调用静态方法
                case Opcodes.INVOKEVIRTUAL://调用实例方法
                case Opcodes.INVOKESPECIAL://调用超类构造方法，实例初始化方法，私有方法
                case Opcodes.INVOKEINTERFACE://调用接口方法
                    //todo 1 构造污染参数集合，方法调用前先把操作数入栈
                    final List<Set<Integer>> argTaint = new ArrayList<Set<Integer>>(argTypes.length);
                    for (int i = 0; i < argTypes.length; i++) {
                        argTaint.add(null);
                    }

                    int stackIndex = 0;
                    for (int i = 0; i < argTypes.length; i++) {
                        Type argType = argTypes[i];
                        if (argType.getSize() > 0) {
                            //根据参数类型大小，从栈底获取入参，参数入栈是从右到左的
                            argTaint.set(argTypes.length - 1 - i, getStackTaint(stackIndex + argType.getSize() - 1));
                        }
                        stackIndex += argType.getSize();
                    }

                    //todo 2 构造方法的调用，意味参数0可以污染返回值
                    if (name.equals("<init>")) {
                        // Pass result taint through to original taint set; the initialized object is directly tainted by
                        // parameters
                        resultTaint = argTaint.get(0);
                    } else {
                        resultTaint = new HashSet<>();
                    }

                    //todo 3 前面已做逆拓扑，调用链最末端最先被visit，因此，调用到的方法必然已被visit分析过
                    Set<Integer> passthrough = passthroughDataflow.get(new MethodReference.Handle(new ClassReference.Handle(owner), name, desc));
                    if (passthrough != null) {
                        for (Integer passthroughDataflowArg : passthrough) {
                            //判断是否和同一方法体内的其它方法返回值关联，有关联则添加到栈底，等待执行return时保存
                            resultTaint.addAll(argTaint.get(passthroughDataflowArg));
                        }
                    }
                    break;
                default:
                    throw new IllegalStateException("Unsupported opcode: " + opcode);
            }

            super.visitMethodInsn(opcode, owner, name, desc, itf);

            if (retSize > 0) {
                getStackTaint(retSize-1).addAll(resultTaint);
            }
        }
    }


    public static void main(String[] args) throws Exception {
        ClassLoader classLoader = Util.getJarClassLoader(Paths.get(args[0]));

        PassthroughDiscovery passthroughDiscovery = new PassthroughDiscovery();
        passthroughDiscovery.discover(new ClassResourceEnumerator(classLoader), new JavaDeserializationConfig());
        passthroughDiscovery.save();
    }
}
