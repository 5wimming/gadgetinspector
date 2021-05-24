package gadgetinspector.webservice;

import gadgetinspector.ConfigHelper;
import gadgetinspector.SourceDiscovery;
import gadgetinspector.data.*;
import gadgetinspector.jackson.JacksonSerializableDecider;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class WebserviceSourceDiscovery extends SourceDiscovery {
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
        final WebserviceSerializableDecider webserviceDecider = new WebserviceSerializableDecider(methodMap);

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

            if (webserviceDecider.apply(method.getClassReference())) {
//                if(method.getClassReference().getName().contains("org/joychou/controller/PathTraversal")){
//                    System.out.println("test by 5wimming");
//                }
                if (!method.getName().contains("<init>")
                        && (method.getDesc().contains("Ljavax/servlet/http/HttpServletRequest")
                        || method.getDesc().contains("Ljavax/servlet/ServletRequest")
                        || method.getDesc().contains("Ljavax/xml/ws/handler/soap/SOAPMessageContext")
                        || method.getDesc().contains("Ljavax/xml/ws/handler/MessageContext")
                        || method.getDesc().contains("Ljavax/xml/rpc/handler/soap/SOAPMessageContext")
                        || method.getDesc().contains("Lorg/apache/cxf/message/Message")
                        || method.getDesc().contains("Lorg/aopalliance/intercept/MethodInvocation")
                        || methodValue.getMethodAnnotationDesc().contains("Lorg/springframework/web/bind/annotation/RequestMapping")
                        || methodValue.getMethodAnnotationDesc().contains("Ljavax/ws/rs/Path")
                        || methodValue.getMethodAnnotationDesc().contains("Lorg/springframework/web/bind/annotation/GetMapping")
                        || methodValue.getMethodAnnotationDesc().contains("Lorg/springframework/web/bind/annotation/PostMapping")
                        || methodValue.getParameterAnnotationDesc().contains("Lorg/springframework/web/bind/annotation/RequestParam")
                        || methodValue.getMethodAnnotationDesc().contains("Lorg/springframework/web/bind/annotation/PutMapping")
                        || methodValue.getMethodAnnotationDesc().contains("Lorg/springframework/web/bind/annotation/PatchMapping")
                        || methodValue.getParameterAnnotationDesc().contains("Lorg/springframework/web/bind/annotation/DeleteMapping")
                        || methodValue.getParameterAnnotationDesc().contains("Ljavax/ws/rs/QueryParam")
                        || methodValue.getParameterAnnotationDesc().contains("Ljavax/ws/PathParam")))
                {
                    addDiscoveredSource(new Source(method, 0));
                }
            }
        }
    }
}
