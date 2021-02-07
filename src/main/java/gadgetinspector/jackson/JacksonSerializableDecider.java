package gadgetinspector.jackson;

import gadgetinspector.SerializableDecider;
import gadgetinspector.data.ClassReference;
import gadgetinspector.data.MethodReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class JacksonSerializableDecider implements SerializableDecider {
    //类是否通过决策的缓存集合
    private final Map<ClassReference.Handle, Boolean> cache = new HashMap<>();
    //类名-方法集合 映射集合
    private final Map<ClassReference.Handle, Set<MethodReference.Handle>> methodsByClassMap;

    public JacksonSerializableDecider(Map<MethodReference.Handle, MethodReference> methodMap) {
        this.methodsByClassMap = new HashMap<>();
        for (MethodReference.Handle method : methodMap.keySet()) {
            Set<MethodReference.Handle> classMethods = methodsByClassMap.get(method.getClassReference());
            if (classMethods == null) {
                classMethods = new HashSet<>();
                methodsByClassMap.put(method.getClassReference(), classMethods);
            }
            classMethods.add(method);
        }
    }

    @Override
    public Boolean apply(ClassReference.Handle handle) {
        if (isNoGadgetClass(handle)) {
            return false;
        }
        Boolean cached = cache.get(handle);
        if (cached != null) {
            return cached;
        }

        Set<MethodReference.Handle> classMethods = methodsByClassMap.get(handle);
        if (classMethods != null) {
            for (MethodReference.Handle method : classMethods) {
                //该类，只要有无参构造方法，就通过决策
                if (method.getName().equals("<init>") && method.getDesc().equals("()V")) {
                    cache.put(handle, Boolean.TRUE);
                    return Boolean.TRUE;
                }
            }
        }

        cache.put(handle, Boolean.FALSE);
        return Boolean.FALSE;
    }

    private boolean isNoGadgetClass(ClassReference.Handle clazz) {
        if (JacksonSourceDiscovery.skipList.contains(clazz.getName())) {
            return true;
        }
        return false;
    }
}
