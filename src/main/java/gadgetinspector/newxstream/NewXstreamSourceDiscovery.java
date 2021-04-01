package gadgetinspector.newxstream;

import gadgetinspector.ConfigHelper;
import gadgetinspector.SourceDiscovery;
import gadgetinspector.data.*;
import gadgetinspector.webservice.WebserviceSerializableDecider;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class NewXstreamSourceDiscovery extends SourceDiscovery {
    public static final Set<String> skipList = new HashSet<>();

    static {
        if (!ConfigHelper.skipSourcesFile.isEmpty()) {
            try(BufferedReader bufferedReader = Files.newBufferedReader(Paths.get(ConfigHelper.skipSourcesFile))) {
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    String c;
                    if (!(c = line.split("#")[0].trim()).isEmpty()) {
                        skipList.add(line.trim());
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void discover(Map<ClassReference.Handle, ClassReference> classMap, Map<MethodReference.Handle, MethodReference> methodMap, InheritanceMap inheritanceMap, Map<MethodReference.Handle, Set<GraphCall>> graphCallMap) {
        final NewXstremSerializableDecider newXstreamDecider = new NewXstremSerializableDecider(methodMap);

        for (MethodReference.Handle method : methodMap.keySet()) {
            MethodReference methodValue = methodMap.get(method);
            boolean skipFlag = false;
            for (String skipClass:skipList){
                if (method.getClassReference().getName().contains(skipClass)){
                    skipFlag = true;
                    break;
                }
            }
            if (skipFlag){
                continue;
            }
            if (newXstreamDecider.apply(method.getClassReference())) {
                if (method.getName().equals("hashCode") && method.getDesc().contains("()"))
                {
                    addDiscoveredSource(new Source(method, 0));
                }
            }

        }
    }
}
