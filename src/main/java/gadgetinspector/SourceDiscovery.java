package gadgetinspector;

import gadgetinspector.data.*;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/* FIXME: This source discovery is limited to standard serializable objects; doesn't do proper source discovery for
 * non-standard Xstream cases. */
public abstract class SourceDiscovery {

    private final List<Source> discoveredSources = new ArrayList<>();

    protected final void addDiscoveredSource(Source source) {
        discoveredSources.add(source);
    }

    public void discover() throws IOException {
        Map<ClassReference.Handle, ClassReference> classMap = DataLoader.loadClasses();
        Map<MethodReference.Handle, MethodReference> methodMap = DataLoader.loadMethods();
        InheritanceMap inheritanceMap = InheritanceMap.load();

        Map<MethodReference.Handle, Set<GraphCall>> graphCallMap = new HashMap<>();
        for (GraphCall graphCall : DataLoader.loadData(Paths.get("callgraph.dat"), new GraphCall.Factory())) {
            MethodReference.Handle caller = graphCall.getCallerMethod();
            if (!graphCallMap.containsKey(caller)) {
                Set<GraphCall> graphCalls = new HashSet<>();
                graphCalls.add(graphCall);
                graphCallMap.put(caller, graphCalls);
            } else {
                graphCallMap.get(caller).add(graphCall);
            }
        }

        discover(classMap, methodMap, inheritanceMap, graphCallMap);
    }

    public abstract void discover(Map<ClassReference.Handle, ClassReference> classMap,
        Map<MethodReference.Handle, MethodReference> methodMap,
        InheritanceMap inheritanceMap, Map<MethodReference.Handle, Set<GraphCall>> graphCallMap);

    public void save() throws IOException {
        DataLoader.saveData(Paths.get("sources.dat"), new Source.Factory(), discoveredSources);
    }
}
