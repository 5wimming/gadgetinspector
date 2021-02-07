package gadgetinspector.data;

import gadgetinspector.data.ClassReference.Handle;
import java.util.Set;

/**
 * @author xuanyh
 */
public class SlinkReference {

  private ClassReference.Handle classReference;
  private Set<MethodReference> methodReferences;

  public SlinkReference(Handle classReference,
      Set<MethodReference> methodReferences) {
    this.classReference = classReference;
    this.methodReferences = methodReferences;
  }

  public Handle getClassReference() {
    return classReference;
  }

  public void setClassReference(Handle classReference) {
    this.classReference = classReference;
  }

  public Set<MethodReference> getMethodReferences() {
    return methodReferences;
  }

  public void setMethodReferences(Set<MethodReference> methodReferences) {
    this.methodReferences = methodReferences;
  }
}
