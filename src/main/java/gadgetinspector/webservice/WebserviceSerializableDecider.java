package gadgetinspector.webservice;

import gadgetinspector.SerializableDecider;
import gadgetinspector.data.ClassReference;
import gadgetinspector.data.MethodReference;
import gadgetinspector.jackson.JacksonSourceDiscovery;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class WebserviceSerializableDecider implements SerializableDecider {
    //类是否通过决策的缓存集合
    private final Map<ClassReference.Handle, Boolean> cache = new HashMap<>();
    //类名-方法集合

    private final Map<ClassReference.Handle, Set<MethodReference.Handle>> methodsByClassMap;

    public WebserviceSerializableDecider(Map<MethodReference.Handle, MethodReference> methodMap){
        this.methodsByClassMap  = new HashMap<>();
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
        return Boolean.TRUE;
    }
    private boolean isNoGadgetClass(ClassReference.Handle clazz) {
        if (WebserviceSourceDiscovery.skipList.contains(clazz.getName())) {
            return true;
        }
        return false;
    }
}
