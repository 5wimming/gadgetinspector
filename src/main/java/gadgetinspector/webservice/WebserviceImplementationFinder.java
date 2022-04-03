package gadgetinspector.webservice;

import gadgetinspector.ImplementationFinder;
import gadgetinspector.SerializableDecider;
import gadgetinspector.data.ClassReference;
import gadgetinspector.data.ClassReference;
import gadgetinspector.data.MethodReference;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class WebserviceImplementationFinder implements ImplementationFinder {
    private final SerializableDecider serializableDecider;
    private final Map<MethodReference.Handle, Set<MethodReference.Handle>> methodImplMap;
    private final Map<ClassReference.Handle, Set<MethodReference.Handle>> methodsByClass;
    public WebserviceImplementationFinder(SerializableDecider serializableDecider,
                                          Map<MethodReference.Handle, Set<MethodReference.Handle>> methodImplMap,
                                          Map<ClassReference.Handle, Set<MethodReference.Handle>> methodsByClass){
        this.serializableDecider = serializableDecider;
        this.methodImplMap = methodImplMap;
        this.methodsByClass = methodsByClass;
    }
    @Override
    public Set<MethodReference.Handle> getImplementations(MethodReference.Handle target) {
        Set<MethodReference.Handle> allImpls = new HashSet<>();

        if (Boolean.TRUE.equals(serializableDecider.apply(target.getClassReference()))) {
            Set<MethodReference.Handle> methods = methodsByClass.get(target.getClassReference());
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
