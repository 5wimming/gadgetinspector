package gadgetinspector.config;

import gadgetinspector.ImplementationFinder;
import gadgetinspector.SerializableDecider;
import gadgetinspector.SlinkDiscovery;
import gadgetinspector.SourceDiscovery;
import gadgetinspector.data.ClassReference;
import gadgetinspector.data.InheritanceMap;
import gadgetinspector.data.MethodReference;
import gadgetinspector.newxstream.*;

import java.util.Map;
import java.util.Set;

public class NewXstreamDeserializationConfig implements GIConfig {
    @Override
    public String getName() {
        return "newXstream";
    }

    @Override
    public SerializableDecider getSerializableDecider(Map<MethodReference.Handle, MethodReference> methodMap, InheritanceMap inheritanceMap) {
        return new NewXstremSerializableDecider(methodMap);
    }

    @Override
    public ImplementationFinder getImplementationFinder(Map<MethodReference.Handle, MethodReference> methodMap, Map<MethodReference.Handle, Set<MethodReference.Handle>> methodImplMap, InheritanceMap inheritanceMap, Map<ClassReference.Handle, Set<MethodReference.Handle>> methodsByClass) {
        return new NewXstremImplementationFinder(getSerializableDecider(methodMap, inheritanceMap));

    }

    @Override
    public SourceDiscovery getSourceDiscovery() {
        return new NewXstreamSourceDiscovery();
    }

    @Override
    public SlinkDiscovery getSlinkDiscovery() {
        return null;
    }
}
