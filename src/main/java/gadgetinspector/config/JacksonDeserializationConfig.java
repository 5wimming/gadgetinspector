package gadgetinspector.config;

import gadgetinspector.ImplementationFinder;
import gadgetinspector.SerializableDecider;
import gadgetinspector.SlinkDiscovery;
import gadgetinspector.SourceDiscovery;
import gadgetinspector.data.ClassReference;
import gadgetinspector.data.InheritanceMap;
import gadgetinspector.data.MethodReference;
import gadgetinspector.data.MethodReference.Handle;
import gadgetinspector.jackson.JacksonImplementationFinder;
import gadgetinspector.jackson.JacksonSerializableDecider;
import gadgetinspector.jackson.JacksonSourceDiscovery;

import java.util.Map;
import java.util.Set;

public class JacksonDeserializationConfig implements GIConfig {

    @Override
    public String getName() {
        return "jackson";
    }

    @Override
    public SerializableDecider getSerializableDecider(Map<MethodReference.Handle, MethodReference> methodMap, InheritanceMap inheritanceMap) {
        return new JacksonSerializableDecider(methodMap);
    }

    @Override
    public ImplementationFinder getImplementationFinder(
        Map<Handle, MethodReference> methodMap,
        Map<Handle, Set<Handle>> methodImplMap,
        InheritanceMap inheritanceMap,
        Map<ClassReference.Handle, Set<Handle>> methodsByClass) {
        return new JacksonImplementationFinder(getSerializableDecider(methodMap, inheritanceMap));
    }

    @Override
    public SourceDiscovery getSourceDiscovery() {
        return new JacksonSourceDiscovery();
    }

    @Override
    public SlinkDiscovery getSlinkDiscovery() {
        return null;
    }
}
