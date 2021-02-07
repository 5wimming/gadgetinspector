package gadgetinspector.data;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author xuanyh
 */
public class SlinkFactory implements DataFactory<SlinkReference> {

  @Override
  public SlinkReference parse(String[] fields) {
    Set<MethodReference> methodReferences = new HashSet<>();
    String[] tmp = fields[1].split("&");
    for (int i = 0; i < tmp.length; i++) {
      String[] methods = tmp[i].split("!");
      methodReferences.add(new MethodReference(
          new ClassReference.Handle(methods[0]),
          methods[1],
          methods[2],
          Boolean.parseBoolean(methods[3])));
    }
    ClassReference.Handle handle = new ClassReference.Handle(fields[0]);
    return new SlinkReference(handle, methodReferences);
  }

  @Override
  public String[] serialize(SlinkReference obj) {
    String methodReferenceString = obj.getMethodReferences().stream().map(
        methodReference -> methodReference.getClassReference().getName() + "!" + methodReference
            .getName() + "!" + methodReference.getDesc() + "!" + methodReference.isStatic())
        .collect(
            Collectors.joining("&"));
    return new String[]{
        obj.getClassReference().getName(),
        methodReferenceString,
    };
  }
}