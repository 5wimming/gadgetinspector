package gadgetinspector.hessian;

import gadgetinspector.SerializableDecider;
import gadgetinspector.data.ClassReference;
import gadgetinspector.data.ClassReference.Handle;
import gadgetinspector.data.InheritanceMap;
import java.util.HashMap;
import java.util.Map;

public class HessianSerializableDecider implements SerializableDecider {
    private final Map<ClassReference.Handle, Boolean> cache = new HashMap<>();
    private final InheritanceMap inheritanceMap;

    public HessianSerializableDecider(InheritanceMap inheritanceMap) {
        this.inheritanceMap = inheritanceMap;
    }

    /**
     * 用于判断class是否可以被序列化
     *
     * @param handle
     * @return
     */
    @Override
    public Boolean apply(ClassReference.Handle handle) {
        Boolean cached = cache.get(handle);
        if (cached != null) {
            return cached;
        }

        Boolean result = applyNoCache(handle);

        cache.put(handle, result);
        return result;
    }

    private Boolean applyNoCache(ClassReference.Handle handle) {

        if (isBlacklistedClass(handle)) {
            return false;
        }

        if (isNoGadgetClass(handle)) {
            return false;
        }

        return true;
    }

    /**
     * 判断class是否在黑名单内
     *
     * @param clazz
     * @return
     */
    private boolean isBlacklistedClass(ClassReference.Handle clazz) {
//        if (inheritanceMap.isSubclassOf(clazz, new Handle("java/io/InputStream"))) {
//            return true;
//        }
        return false;
    }

    private boolean isNoGadgetClass(ClassReference.Handle clazz) {
        if (HessianSourceDiscovery.skipList.contains(clazz.getName())) {
            return true;
        }
        return false;
    }
}
