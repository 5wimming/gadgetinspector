package gadgetinspector.config;

import gadgetinspector.ImplementationFinder;
import gadgetinspector.SerializableDecider;
import gadgetinspector.SlinkDiscovery;
import gadgetinspector.SourceDiscovery;
import gadgetinspector.data.ClassReference;
import gadgetinspector.data.InheritanceMap;
import gadgetinspector.data.MethodReference;

import gadgetinspector.data.MethodReference.Handle;
import java.util.Map;
import java.util.Set;

public interface GIConfig {

    String getName();
    SerializableDecider getSerializableDecider(Map<MethodReference.Handle, MethodReference> methodMap, InheritanceMap inheritanceMap);
    ImplementationFinder getImplementationFinder(
        Map<Handle, MethodReference> methodMap,
        Map<Handle, Set<Handle>> methodImplMap,
        InheritanceMap inheritanceMap,
        Map<ClassReference.Handle, Set<Handle>> methodsByClass);
    SourceDiscovery getSourceDiscovery();
    SlinkDiscovery getSlinkDiscovery();
}
