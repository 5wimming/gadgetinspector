package gadgetinspector.fastjson;

import gadgetinspector.ImplementationFinder;
import gadgetinspector.SerializableDecider;
import gadgetinspector.data.ClassReference;
import gadgetinspector.data.MethodReference;
import gadgetinspector.data.MethodReference.Handle;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FastjsonImplementationFinder implements ImplementationFinder {

    private final SerializableDecider serializableDecider;
    private final Map<Handle, Set<Handle>> methodImplMap;
    private final Map<ClassReference.Handle, Set<Handle>> methodsByClass;

    public FastjsonImplementationFinder(SerializableDecider serializableDecider,
        Map<Handle, Set<Handle>> methodImplMap,
        Map<ClassReference.Handle, Set<Handle>> methodsByClass) {
        this.serializableDecider = serializableDecider;
        this.methodImplMap = methodImplMap;
        this.methodsByClass = methodsByClass;
    }

    @Override
    public Set<MethodReference.Handle> getImplementations(MethodReference.Handle target) {
        Set<MethodReference.Handle> allImpls = new HashSet<>();

        // Fastjson可以指定接口实现类
        if (Boolean.TRUE.equals(serializableDecider.apply(target.getClassReference()))) {
            Set<Handle> methods = methodsByClass.get(target.getClassReference());
            if (methods == null)
                return allImpls;
            if (methods.contains(target)) {
                allImpls.add(target);
            }
        }

        Set<MethodReference.Handle> subClassImpls = methodImplMap.get(target);
        if (subClassImpls != null) {
            for (MethodReference.Handle subClassImpl : subClassImpls) {
                if (Boolean.TRUE.equals(serializableDecider.apply(subClassImpl.getClassReference()))) {
                    allImpls.add(subClassImpl);
                }
            }
        }

        return allImpls;
    }
}
