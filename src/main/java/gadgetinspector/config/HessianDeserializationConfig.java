package gadgetinspector.config;

import gadgetinspector.ImplementationFinder;
import gadgetinspector.SerializableDecider;
import gadgetinspector.SlinkDiscovery;
import gadgetinspector.SourceDiscovery;
import gadgetinspector.data.ClassReference;
import gadgetinspector.data.InheritanceMap;
import gadgetinspector.data.MethodReference;
import gadgetinspector.data.MethodReference.Handle;
import gadgetinspector.hessian.HessianImplementationFinder;
import gadgetinspector.hessian.HessianSerializableDecider;
import gadgetinspector.hessian.HessianSourceDiscovery;
import java.util.Map;
import java.util.Set;

public class HessianDeserializationConfig implements GIConfig {

    @Override
    public String getName() {
        return "hessian";
    }

    @Override
    public SerializableDecider getSerializableDecider(Map<MethodReference.Handle, MethodReference> methodMap, InheritanceMap inheritanceMap) {
        return new HessianSerializableDecider(inheritanceMap);
    }

    @Override
    public ImplementationFinder getImplementationFinder(
        Map<Handle, MethodReference> methodMap,
        Map<Handle, Set<Handle>> methodImplMap,
        InheritanceMap inheritanceMap,
        Map<ClassReference.Handle, Set<Handle>> methodsByClass) {
        return new HessianImplementationFinder(getSerializableDecider(methodMap, inheritanceMap), methodImplMap);
    }

    @Override
    public SourceDiscovery getSourceDiscovery() {
        return new HessianSourceDiscovery();
    }

    @Override
    public SlinkDiscovery getSlinkDiscovery() {
        return null;
    }
}
