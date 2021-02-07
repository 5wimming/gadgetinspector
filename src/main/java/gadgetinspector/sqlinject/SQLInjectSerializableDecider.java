package gadgetinspector.sqlinject;

import gadgetinspector.SerializableDecider;
import gadgetinspector.data.ClassReference;
import gadgetinspector.data.MethodReference;
import java.util.Map;

public class SQLInjectSerializableDecider implements SerializableDecider {
    public SQLInjectSerializableDecider(Map<MethodReference.Handle, MethodReference> methodMap) {
    }

    @Override
    public Boolean apply(ClassReference.Handle handle) {
        return Boolean.FALSE;
    }
}
