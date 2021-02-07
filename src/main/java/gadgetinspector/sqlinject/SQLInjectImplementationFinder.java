package gadgetinspector.sqlinject;

import gadgetinspector.ImplementationFinder;
import gadgetinspector.SerializableDecider;
import gadgetinspector.data.MethodReference;
import gadgetinspector.data.MethodReference.Handle;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SQLInjectImplementationFinder implements ImplementationFinder {

    private final Map<Handle, Set<Handle>> methodImplMap;

    public SQLInjectImplementationFinder(Map<MethodReference.Handle, Set<MethodReference.Handle>> methodImplMap) {
        this.methodImplMap = methodImplMap;
    }

    @Override
    public Set<MethodReference.Handle> getImplementations(MethodReference.Handle target) {
        Set<MethodReference.Handle> allImpls = new HashSet<>();

        // Assume that the target method is always available, even if not serializable; the target may just be a local
        // instance rather than something an attacker can control.
        allImpls.add(target);

        Set<MethodReference.Handle> subClassImpls = methodImplMap.get(target);
        if (subClassImpls != null) {
            for (MethodReference.Handle subClassImpl : subClassImpls) {
                allImpls.add(subClassImpl);
            }
        }

        return allImpls;
    }
}
