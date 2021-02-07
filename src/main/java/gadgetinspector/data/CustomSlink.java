package gadgetinspector.data;

/**
 * @author threedr3am
 */
public class CustomSlink {
  private String className;
  private String method;
  private String desc;

  public CustomSlink() {
  }

  public CustomSlink(String className, String method, String desc) {
    this.className = className;
    this.method = method;
    this.desc = desc;
  }

  public String getClassName() {
    return className;
  }

  public void setClassName(String className) {
    this.className = className;
  }

  public String getMethod() {
    return method;
  }

  public void setMethod(String method) {
    this.method = method;
  }

  public String getDesc() {
    return desc;
  }

  public void setDesc(String desc) {
    this.desc = desc;
  }
}
