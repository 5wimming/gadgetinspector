package gadgetinspector.fastjson;

import gadgetinspector.SerializableDecider;
import gadgetinspector.data.ClassReference;
import gadgetinspector.data.MethodReference;
import gadgetinspector.hessian.HessianSourceDiscovery;
import java.util.Map;

public class FastjsonSerializableDecider implements SerializableDecider {
    public FastjsonSerializableDecider(Map<MethodReference.Handle, MethodReference> methodMap) {
    }

    @Override
    public Boolean apply(ClassReference.Handle handle) {
        if (isNoGadgetClass(handle)) {
            return false;
        }
        return Boolean.TRUE;
    }

    private boolean isNoGadgetClass(ClassReference.Handle clazz) {
        if (FastjsonSourceDiscovery.skipList.contains(clazz.getName())) {
            return true;
        }
        return false;
    }
}
