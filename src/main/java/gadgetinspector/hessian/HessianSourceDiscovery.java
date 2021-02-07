package gadgetinspector.hessian;

import gadgetinspector.SerializableDecider;
import gadgetinspector.SourceDiscovery;
import gadgetinspector.data.ClassReference;
import gadgetinspector.data.GraphCall;
import gadgetinspector.data.InheritanceMap;
import gadgetinspector.data.MethodReference;
import gadgetinspector.data.Source;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.objectweb.asm.Type;

public class HessianSourceDiscovery extends SourceDiscovery {

  public static final Set<String> skipList = new HashSet<>();

  static {
    //一些在非污点模式下发现无法利用的类
    skipList.add("javax/naming/directory/BasicAttributes");
    skipList.add("net/bytebuddy/description/annotation/AnnotationDescription$AbstractBase");
    skipList.add("javax/naming/Reference");
    skipList.add("");
    skipList.add("");
    skipList.add("");
    skipList.add("");
  }

  @Override
  public void discover(Map<ClassReference.Handle, ClassReference> classMap,
      Map<MethodReference.Handle, MethodReference> methodMap,
      InheritanceMap inheritanceMap, Map<MethodReference.Handle, Set<GraphCall>> graphCallMap) {

    final SerializableDecider serializableDecider = new HessianSerializableDecider(inheritanceMap);

    // hashCode() or equals() are accessible entry points using standard tricks of putting those objects
    // into a HashMap.
    for (MethodReference.Handle method : methodMap.keySet()) {
      if (skipList.contains(method.getClassReference().getName())) {
        continue;
      }
      if (method.getName().equals("hashCode") && method.getDesc().equals("()I")) {
        addDiscoveredSource(new Source(method, 0));
      }
      if (method.getName().equals("equals") && method.getDesc().equals("(Ljava/lang/Object;)Z")) {
        addDiscoveredSource(new Source(method, 0));
        addDiscoveredSource(new Source(method, 1));
      }
      if (method.getName().equals("toString") && method.getDesc().equals("()Ljava/lang/String;")) {
        addDiscoveredSource(new Source(method, 0));
      }
    }
  }

  public static void main(String[] args) throws Exception {
    SourceDiscovery sourceDiscovery = new HessianSourceDiscovery();
    sourceDiscovery.discover();
    sourceDiscovery.save();
  }
}
