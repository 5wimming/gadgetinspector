package gadgetinspector.config;

import gadgetinspector.ImplementationFinder;
import gadgetinspector.SerializableDecider;
import gadgetinspector.SlinkDiscovery;
import gadgetinspector.SourceDiscovery;
import gadgetinspector.data.ClassReference;
import gadgetinspector.data.InheritanceMap;
import gadgetinspector.data.MethodReference;
import gadgetinspector.fastjson.FastjsonImplementationFinder;
import gadgetinspector.webservice.WebserviceImplementationFinder;
import gadgetinspector.webservice.WebserviceSerializableDecider;
import gadgetinspector.webservice.WebserviceSourceDiscovery;

import java.util.Map;
import java.util.Set;

public class WebserviceDeserializationConfig implements GIConfig {
    @Override
    public String getName() {
        return "webservice";
    }

    @Override
    public SerializableDecider getSerializableDecider(Map<MethodReference.Handle, MethodReference> methodMap, InheritanceMap inheritanceMap) {
        return new WebserviceSerializableDecider(methodMap);
    }

    @Override
    public ImplementationFinder getImplementationFinder(Map<MethodReference.Handle, MethodReference> methodMap,
                                                        Map<MethodReference.Handle, Set<MethodReference.Handle>> methodImplMap,
                                                        InheritanceMap inheritanceMap, Map<ClassReference.Handle, Set<MethodReference.Handle>> methodsByClass) {
        return new WebserviceImplementationFinder(getSerializableDecider(methodMap, inheritanceMap), methodImplMap, methodsByClass);
    }

    @Override
    public SourceDiscovery getSourceDiscovery() {
        return new WebserviceSourceDiscovery();
    }

    @Override
    public SlinkDiscovery getSlinkDiscovery() {
        return null;
    }
}
