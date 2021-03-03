package gadgetinspector;

import gadgetinspector.config.GIConfig;
import gadgetinspector.config.JavaDeserializationConfig;
import gadgetinspector.data.ClassReference;
import gadgetinspector.data.CustomSlink;
import gadgetinspector.data.DataLoader;
import gadgetinspector.data.GraphCall;
import gadgetinspector.data.InheritanceDeriver;
import gadgetinspector.data.InheritanceMap;
import gadgetinspector.data.MethodReference;
import gadgetinspector.data.MethodReference.Handle;
import gadgetinspector.data.Source;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.TreeSet;
import java.util.Comparator;

public class GadgetChainDiscovery {

  private static final Logger LOGGER = LoggerFactory.getLogger(GadgetChainDiscovery.class);

  private final GIConfig config;

  public GadgetChainDiscovery(GIConfig config) {
    this.config = config;
  }

  private static List<CustomSlink> customSlinks = new ArrayList<>();

  static {
    if (!ConfigHelper.slinksFile.isEmpty()) {
      try(BufferedReader bufferedReader = Files.newBufferedReader(Paths.get(ConfigHelper.slinksFile))) {
        String line;
        while ((line = bufferedReader.readLine()) != null) {
          String c;
          if (!(c = line.split("#")[0].trim()).isEmpty()) {
            String[] slinks = c.split(" ");
            CustomSlink customSlink = new CustomSlink();
            if (slinks.length > 0) {
              customSlink.setClassName(slinks[0]);
            }
            if (slinks.length > 1) {
              customSlink.setMethod(slinks[1]);
            }
            if (slinks.length > 2) {
              customSlink.setDesc(slinks[2]);
            }
            customSlinks.add(customSlink);
          }
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public void discover(List<Path> pathList) throws Exception {
    Map<MethodReference.Handle, MethodReference> methodMap = DataLoader.loadMethods();
    InheritanceMap inheritanceMap = InheritanceMap.load();
    Map<MethodReference.Handle, Set<MethodReference.Handle>> methodImplMap = InheritanceDeriver
        .getAllMethodImplementations(
            inheritanceMap, methodMap);
    Map<ClassReference.Handle, Set<MethodReference.Handle>> methodsByClass = InheritanceDeriver.getMethodsByClass(methodMap);

    final ImplementationFinder implementationFinder = config.getImplementationFinder(
        methodMap, methodImplMap, inheritanceMap, methodsByClass);

    try (Writer writer = Files.newBufferedWriter(Paths.get("methodimpl.dat"))) {
      for (Map.Entry<MethodReference.Handle, Set<MethodReference.Handle>> entry : methodImplMap
          .entrySet()) {
        writer.write(entry.getKey().getClassReference().getName());
        writer.write("\t");
        writer.write(entry.getKey().getName());
        writer.write("\t");
        writer.write(entry.getKey().getDesc());
        writer.write("\n");
        for (MethodReference.Handle method : entry.getValue()) {
          writer.write("\t");
          writer.write(method.getClassReference().getName());
          writer.write("\t");
          writer.write(method.getName());
          writer.write("\t");
          writer.write(method.getDesc());
          writer.write("\n");
        }
      }
    }

    Map<MethodReference.Handle, Set<GraphCall>> graphCallMap = new HashMap<>();
    for (GraphCall graphCall : DataLoader
        .loadData(Paths.get("callgraph.dat"), new GraphCall.Factory())) {
      MethodReference.Handle caller = graphCall.getCallerMethod();
      if (!graphCallMap.containsKey(caller)) {
        Set<GraphCall> graphCalls = new HashSet<>();
        graphCalls.add(graphCall);
        graphCallMap.put(caller, graphCalls);
      } else {
        graphCallMap.get(caller).add(graphCall);
      }
    }

    Map<GadgetChainLink, Integer> exploredMethodsMap = new HashMap<>(); //分支反问次数
    Set<GadgetChainLink> exploredMethods = new HashSet<>();
    LinkedList<GadgetChain> methodsToExplore = new LinkedList<>();
    LinkedList<GadgetChain> methodsToExploreRepeat = new LinkedList<>();
    for (Source source : DataLoader.loadData(Paths.get("sources.dat"), new Source.Factory())) {
      GadgetChainLink srcLink = new GadgetChainLink(source.getSourceMethod(),
          source.getTaintedArgIndex());
      if (exploredMethods.contains(srcLink)) {
        continue;
      }
      methodsToExplore.add(new GadgetChain(Arrays.asList(srcLink)));
      exploredMethods.add(srcLink);
    }

    long iteration = 0;
    Set<GadgetChain> discoveredGadgets = new HashSet<>();

    while (methodsToExplore.size() > 0) {
      if ((iteration % 1000) == 0) {
        LOGGER.info("Iteration " + iteration + ", Search space: " + methodsToExplore.size());
      }
      iteration += 1;

      GadgetChain chain = methodsToExplore.pop();
      GadgetChainLink lastLink = chain.links.get(chain.links.size() - 1);

      //限定链长度
      if (chain.links.size() >= ConfigHelper.maxChainLength) {
        continue;
      }

      Set<GraphCall> methodCalls = graphCallMap.get(lastLink.method);
      if (methodCalls != null) {
        for (GraphCall graphCall : methodCalls) {
          //使用污点分析才会进行数据流判断
          if (graphCall.getCallerArgIndex() != lastLink.taintedArgIndex
              && ConfigHelper.taintTrack) {
            continue;
          }

          Set<MethodReference.Handle> allImpls = implementationFinder
              .getImplementations(graphCall.getTargetMethod());

          //todo gadgetinspector bug 没记录继承父类的方法，导致不可能找到
          if (allImpls.isEmpty()) {
            Set<ClassReference.Handle> parents = inheritanceMap.getSuperClasses(graphCall.getTargetMethod().getClassReference());
            if (parents == null)
              continue;
            for (ClassReference.Handle parent : parents) {
              Set<MethodReference.Handle> methods = methodsByClass.get(parent);
              if (methods == null)
                continue;
              for (MethodReference.Handle method : methods) {
                if (method.getName().equals(graphCall.getTargetMethod().getName()) && method.getDesc().equals(graphCall.getTargetMethod().getDesc())) {
                  allImpls.add(method);
                }
              }
            }
          }

          for (MethodReference.Handle methodImpl : allImpls) {
            GadgetChainLink newLink = new GadgetChainLink(methodImpl,
                graphCall.getTargetArgIndex());
            if (exploredMethods.contains(newLink)) {
              if (chain.links.size() <= ConfigHelper.opLevel) {
                GadgetChain newChain = new GadgetChain(chain, newLink);
                methodsToExploreRepeat.add(newChain);
                continue;
              }
              // 增加超参数maxRepeatBranchesTimes，解决分支多用问题
              if (exploredMethodsMap.containsKey(newLink)){
                if(exploredMethodsMap.get(newLink) > ConfigHelper.maxRepeatBranchesTimes){
                  continue;
                }
                exploredMethodsMap.put(newLink, exploredMethodsMap.get(newLink)+1);
              }
              else {
                exploredMethodsMap.put(newLink, 1);
              }
            }

            GadgetChain newChain = new GadgetChain(chain, newLink);
            if (isSink(methodImpl, graphCall.getTargetArgIndex(), inheritanceMap)) {
              discoveredGadgets.add(newChain);
            } else {
              methodsToExplore.add(newChain);
              exploredMethods.add(newLink);
            }
          }
        }
      }
    }

    //链聚合优化
    Set<GadgetChain> tmpDiscoveredGadgets = new HashSet<>();
    for (GadgetChain gadgetChain : methodsToExploreRepeat) {
      GadgetChainLink lastLink = gadgetChain.links.get(gadgetChain.links.size() - 1);
      for (GadgetChain discoveredGadgetChain : discoveredGadgets) {
        boolean exist = false;
        for (GadgetChainLink gadgetChainLink : discoveredGadgetChain.links) {
          if (exist) {
            gadgetChain = new GadgetChain(gadgetChain, gadgetChainLink);
          }
          if (lastLink.equals(gadgetChainLink)) {
            exist = true;
          }
        }
        if (exist) {
          tmpDiscoveredGadgets.add(gadgetChain);
        }
      }
    }
    discoveredGadgets.addAll(tmpDiscoveredGadgets);

    // 解决路径爆炸，即中间路径重复率过高的问题
    if (ConfigHelper.similarLevel > 0){
      TreeSet<GadgetChain> treeSimilar = new TreeSet<>(new Comparator<GadgetChain>() {
        @Override
        public int compare(GadgetChain o1, GadgetChain o2) {
          int compareResult = o1.links.size() - o2.links.size();
          if (compareResult == 0){
            return -1;
          }
          return compareResult;

        }
      });
      for (GadgetChain chain : discoveredGadgets) {
        if (chain.links.size() <= ConfigHelper.similarLevel){
          continue;
        }
        treeSimilar.add(chain);
      }
      if (!treeSimilar.isEmpty()){
        Set<ArrayList<GadgetChainLink>> repeatSim = new HashSet<>();
        for (GadgetChain chain : treeSimilar){
          ArrayList<GadgetChainLink> temp = new ArrayList<>(chain.links.subList(0,ConfigHelper.similarLevel));
          temp.add(chain.links.get(chain.links.size()-1));
          if(repeatSim.contains(temp)){
            discoveredGadgets.remove(chain);
          }
          else {
            repeatSim.add(temp);
          }
        }
      }
    }


    if (!discoveredGadgets.isEmpty()) {
      SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm");
      try (OutputStream outputStream = Files
          .newOutputStream(
              Paths.get("gadget-result/gadget-chains-" + simpleDateFormat.format(new Date())
                  + ".txt"));
          Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
        if (pathList != null) {
          writer.write("Using classpath: " + Arrays.toString(pathList.toArray()) + "\n");
        }
        for (GadgetChain chain : discoveredGadgets) {
          printGadgetChain(writer, chain);
        }
      }
    }

    LOGGER.info("Found {} gadget chains.", discoveredGadgets.size());
  }

  private static void printGadgetChain(Writer writer, GadgetChain chain) throws IOException {
    writer.write(String.format("%s.%s%s (%d)%n",
        chain.links.get(0).method.getClassReference().getName(),
        chain.links.get(0).method.getName(),
        chain.links.get(0).method.getDesc(),
        chain.links.get(0).taintedArgIndex));
    for (int i = 1; i < chain.links.size(); i++) {
      writer.write(String.format("  %s.%s%s (%d)%n",
          chain.links.get(i).method.getClassReference().getName(),
          chain.links.get(i).method.getName(),
          chain.links.get(i).method.getDesc(),
          chain.links.get(i).taintedArgIndex));
    }
    writer.write("\n");
  }

  private static class GadgetChain {

    private final List<GadgetChainLink> links;

    private GadgetChain(List<GadgetChainLink> links) {
      this.links = links;
    }

    private GadgetChain(GadgetChain gadgetChain, GadgetChainLink link) {
      List<GadgetChainLink> links = new ArrayList<GadgetChainLink>(gadgetChain.links);
      links.add(link);
      this.links = links;
    }
  }

  private static class GadgetChainLink {

    private final MethodReference.Handle method;
    private final int taintedArgIndex;

    private GadgetChainLink(MethodReference.Handle method, int taintedArgIndex) {
      this.method = method;
      this.taintedArgIndex = taintedArgIndex;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      GadgetChainLink that = (GadgetChainLink) o;

      if (taintedArgIndex != that.taintedArgIndex) {
        return false;
      }
      return method != null ? method.equals(that.method) : that.method == null;
    }

    @Override
    public int hashCode() {
      int result = method != null ? method.hashCode() : 0;
      result = 31 * result + taintedArgIndex;
      return result;
    }
  }


  /**
   * Represents a collection of methods in the JDK that we consider to be "interesting". If a gadget
   * chain can successfully exercise one of these, it could represent anything as mundade as causing
   * the target to make a DNS query to full blown RCE.
   */
  // TODO: Parameterize this as a configuration option
  private boolean isSink(MethodReference.Handle method, int argIndex,
      InheritanceMap inheritanceMap) {
    if (!customSlinks.isEmpty()) {
      for (CustomSlink customSlink:customSlinks) {
        boolean flag = false;
        if (customSlink.getClassName() != null)
          flag &= customSlink.getClassName().equals(method.getClassReference().getName());
        if (customSlink.getMethod() != null)
          flag &= customSlink.getMethod().equals(method.getName());
        if (customSlink.getDesc() != null)
          flag &= customSlink.getDesc().equals(method.getDesc());
        if (flag)
          return flag;
      }
      return false;
    }
    if (config.getName().equals("sqlinject")) {
      //SQLInject只能检测注入
      return isSQLInjectSink(method, argIndex, inheritanceMap);
    }
    if (config.getName().equals("hessian")) {
      //仅hessian可选BCEL slink
      if (ConfigHelper.slinks.contains("BCEL") && BCELSlink(method, argIndex, inheritanceMap)) {
        return true;
      }
    }

    //通用slink，不设定slink则全部都挖掘
    if ((ConfigHelper.slinks.isEmpty() || ConfigHelper.slinks.contains("JNDI")) && JNDISlink(method, inheritanceMap)) {
      return true;
    }
    if ((ConfigHelper.slinks.isEmpty() || ConfigHelper.slinks.contains("SSRFAndXXE")) && SSRFAndXXESlink(method, inheritanceMap)) {
      return true;
    }
    if ((ConfigHelper.slinks.isEmpty() || ConfigHelper.slinks.contains("EXEC")) && EXECSlink(method, argIndex)) {
      return true;
    }
    if ((ConfigHelper.slinks.isEmpty() || ConfigHelper.slinks.contains("FileIO")) && FileIOSlink(method)) {
      return true;
    }
    if ((ConfigHelper.slinks.isEmpty() || ConfigHelper.slinks.contains("Reflect")) && ReflectSlink(method, argIndex, inheritanceMap)) {
      return true;
    }
    if ((ConfigHelper.slinks.isEmpty() || ConfigHelper.slinks.contains("JDBC")) && JDBCSlink(method, argIndex, inheritanceMap)) {
      return true;
    }
    if ((ConfigHelper.slinks.isEmpty() || ConfigHelper.slinks.contains("EL")) && ELSlink(method, argIndex, inheritanceMap)) {
      return true;
    }
    if ((ConfigHelper.slinks.isEmpty() || ConfigHelper.slinks.contains("SQLInject")) && isSQLInjectSink(method, argIndex, inheritanceMap)) {
      return true;
    }
    if ((ConfigHelper.slinks.isEmpty() || ConfigHelper.slinks.contains("deserialize")) && deserializeSlink(method)) {
      return true;
    }
    if ((ConfigHelper.slinks.isEmpty() || ConfigHelper.slinks.contains("Xpath")) && XpathSlink(method)) {
      return true;
    }
    if ((ConfigHelper.slinks.isEmpty() || ConfigHelper.slinks.contains("expression")) && ExpressionSlink(method)) {
      return true;
    }
    return false;
  }
  private boolean ExpressionSlink(Handle method) {
    if (method.getClassReference().getName().equals("org/springframework/beans/factory/config/BeanExpressionResolver")
            && method.getName().equals("evaluate")) {
      return true;
    }
    if (method.getClassReference().getName().equals("org/springframework/expression/ExpressionParser")
            && method.getName().equals("parseExpression")) {
      return true;
    }
    if (method.getClassReference().getName().equals("org/opensymphony/xwork2/util/ValueStack")
            && method.getName().equals("setValue")) {
      return true;
    }
    if (method.getClassReference().getName().equals("org/opensymphony/xwork2/util/ValueStack")
            && method.getName().equals("findValue")) {
      return true;
    }
    if (method.getClassReference().getName().equals("ognl/Ognl")
            && method.getName().equals("parseExpression")) {
      return true;
    }
    if (method.getClassReference().getName().equals("ognl/Ognl")
            && method.getName().equals("getValue")) {
      return true;
    }
    if (method.getClassReference().getName().equals("ognl/Ognl")
            && method.getName().equals("setValue")) {
      return true;
    }
    return false;
  }
  private boolean XpathSlink(Handle method) {
    if (method.getClassReference().getName().equals("javax/xml/xpath/XPath")
            && method.getName().equals("evaluate")) {
      return true;
    }
    if (method.getClassReference().getName().equals("javax/xml/xpath/XPathExpression")
            && method.getName().equals("evaluate")) {
      return true;
    }
    return false;
  }
  private boolean deserializeSlink(Handle method){
    if (method.getClassReference().getName().equals("java/io/ObjectInputStream")
            && method.getName().equals("readObject")) {
      return true;
    }
    if (method.getClassReference().getName().equals("org/ymal/snakeyaml/Yaml")
            && method.getName().equals("<init>")) {
      return true;
    }
    if (method.getClassReference().getName().equals("org/ho/yaml/Yaml")
            && method.getName().equals("load")) {
      return true;
    }
    if (method.getClassReference().getName().equals("org/ho/yaml/YamlDecoder")
            && method.getName().equals("<init>")) {
      return true;
    }
    if (method.getClassReference().getName().equals("com/esotericsoftware/yamlbeans/YamlReader")
            && method.getName().equals("read")) {
      return true;
    }
    if (method.getClassReference().getName().equals("java/beans/XMLDecoder")
            && method.getName().equals("readObject")) {
      return true;
    }
    if (method.getClassReference().getName().equals("com/thoughtworks/xstream/XStream")
            && method.getName().equals("fromXML")) {
      return true;
    }
    if (method.getClassReference().getName().equals("java/net/URLClassLoader")
            && method.getName().equals("findResource")) {
      return true;
    }
    if (method.getClassReference().getName().equals("java/net/URLClassLoader")
            && method.getName().equals("findClass")) {
      return true;
    }
    if (method.getClassReference().getName().equals("java/net/URLClassLoader")
            && method.getName().equals("findResources")) {
      return true;
    }
    return false;
  }

  private boolean JDBCSlink(Handle method, int argIndex, InheritanceMap inheritanceMap) {
    if (method.getClassReference().getName().equals("javax/sql/DataSource")
        && method.getName().equals("getConnection")) {
      return true;
    }
    return false;
  }

  private boolean ReflectSlink(Handle method, int argIndex, InheritanceMap inheritanceMap) {
    if (method.getClassReference().getName().equals("java/lang/reflect/Method")
        && method.getName().equals("invoke") && argIndex == 0) {
      return true;
    }
    if (method.getClassReference().getName().equals("java/net/URLClassLoader")
        && method.getName().equals("newInstance")) {
      return true;
    }

    if (inheritanceMap.isSubclassOf(method.getClassReference(),
        new ClassReference.Handle("java/lang/ClassLoader"))
        && method.getName().equals("<init>")) {
      return true;
    }

    // Some groovy-specific sinks
    if (method.getClassReference().getName().equals("org/codehaus/groovy/runtime/InvokerHelper")
        && method.getName().equals("invokeMethod") && argIndex == 1) {
      return true;
    }

    if (inheritanceMap.isSubclassOf(method.getClassReference(),
        new ClassReference.Handle("groovy/lang/MetaClass"))
        && Arrays.asList("invokeMethod", "invokeConstructor", "invokeStaticMethod")
        .contains(method.getName())) {
      return true;
    }
    return false;
  }

  private boolean FileIOSlink(Handle method) {
    if (method.getClassReference().getName().equals("java/io/FileInputStream")
        && method.getName().equals("<init>")) {
      return true;
    }
    if (method.getClassReference().getName().equals("java/io/FileOutputStream")
        && method.getName().equals("<init>")) {
      return true;
    }
    if (method.getClassReference().getName().equals("java/nio/file/Files")
        && (method.getName().equals("newInputStream")
        || method.getName().equals("newOutputStream")
        || method.getName().equals("newBufferedReader")
        || method.getName().equals("newBufferedWriter"))) {
      return true;
    }
    if (method.getClassReference().getName().equals("java/nio/file/Files") && method.getName()
            .equals("newOutputStream")) {
      return true;
    }
    if (method.getClassReference().getName().equals("java/net/URL") && method.getName()
        .equals("openStream")) {
      return true;
    }
    if (method.getClassReference().getName().equals("org/springframework/web/multipart/MultipartFile") && method.getName()
            .equals("transferTo")) {
      return true;
    }
    return false;
  }

  private boolean dosSlink(Handle method) {
    if (method.getClassReference().getName().equals("java/lang/System")
        && method.getName().equals("exit")) {
      return true;
    }
    if (method.getClassReference().getName().equals("java/lang/Shutdown")
        && method.getName().equals("exit")) {
      return true;
    }
    if (method.getClassReference().getName().equals("java/lang/Runtime")
        && method.getName().equals("exit")) {
      return true;
    }
    return false;
  }

  private boolean EXECSlink(Handle method, int argIndex) {
    if (method.getClassReference().getName().equals("java/lang/Runtime")
        && method.getName().equals("exec")) {
      return true;
    }
    if (method.getClassReference().getName().equals("java/lang/ProcessBuilder")
        && method.getName().equals("<init>") && argIndex > 0) {
      return true;
    }
    return false;
  }

  private boolean BCELSlink(Handle method, int argIndex, InheritanceMap inheritanceMap) {
    if (method.getClassReference().getName().equals("java/lang/Class")
        && method.getName().equals("forName")
        && method.getDesc().equals("(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;")) {
        return true;
    }
    return false;
  }

  private boolean isFastjsonSink(Handle method, int argIndex, InheritanceMap inheritanceMap) {


    return false;
  }

  private boolean SSRFAndXXESlink(Handle method, InheritanceMap inheritanceMap) {
    if ((
        inheritanceMap.isSubclassOf(method.getClassReference(),
            new ClassReference.Handle("javax/xml/parsers/DocumentBuilder"))
    )
        && method.getName().equals("parse")) {
      return true;
    }
    if ((
        inheritanceMap.isSubclassOf(method.getClassReference(),
            new ClassReference.Handle("org/jdom/input/SAXBuilder"))
    )
        && method.getName().equals("build")) {
      return true;
    }
    if ((
        inheritanceMap.isSubclassOf(method.getClassReference(),
            new ClassReference.Handle("javax/xml/parsers/SAXParser"))
    )
        && method.getName().equals("parse")) {
      return true;
    }
    if ((
        inheritanceMap.isSubclassOf(method.getClassReference(),
            new ClassReference.Handle("org/dom4j/io/SAXReader"))
    )
        && method.getName().equals("read")) {
      return true;
    }
    if ((
        inheritanceMap.isSubclassOf(method.getClassReference(),
            new ClassReference.Handle("javax/xml/transform/sax/SAXTransformerFactory"))
    )
        && method.getName().equals("newTransformerHandler")) {
      return true;
    }
    if ((
        inheritanceMap.isSubclassOf(method.getClassReference(),
            new ClassReference.Handle("javax/xml/validation/SchemaFactory"))
    )
        && method.getName().equals("newSchema")) {
      return true;
    }
    if ((
        inheritanceMap.isSubclassOf(method.getClassReference(),
            new ClassReference.Handle("javax/xml/transform/Transformer"))
    )
        && method.getName().equals("transform")) {
      return true;
    }
    if ((
        inheritanceMap.isSubclassOf(method.getClassReference(),
            new ClassReference.Handle("javax/xml/bind/Unmarshaller"))
    )
        && method.getName().equals("unmarshal")) {
      return true;
    }
    if ((
        inheritanceMap.isSubclassOf(method.getClassReference(),
            new ClassReference.Handle("javax/xml/validation/Validator"))
    )
        && method.getName().equals("validate")) {
      return true;
    }
    if ((
        inheritanceMap.isSubclassOf(method.getClassReference(),
            new ClassReference.Handle("org/xml/sax/XMLReader"))
    )
        && method.getName().equals("parse")) {
      return true;
    }
    return false;
  }

  private boolean JNDISlink(Handle method, InheritanceMap inheritanceMap) {
    if ((inheritanceMap.isSubclassOf(method.getClassReference(),
        new ClassReference.Handle("java/rmi/registry/Registry")) ||
        inheritanceMap.isSubclassOf(method.getClassReference(),
            new ClassReference.Handle("javax/naming/Context")) ||
            method.getClassReference().getName().equals("javax/naming/Context") ||
            method.getClassReference().getName().equals("java/rmi/registry/Registry"))
        && method.getName().equals("lookup")) {
      return true;
    }
    return false;
  }

  private boolean ELSlink(Handle method, int argIndex, InheritanceMap inheritanceMap) {
    if ((inheritanceMap.isSubclassOf(method.getClassReference(),
            new ClassReference.Handle("javax/validation/ConstraintValidatorContext")) ||
        inheritanceMap.isSubclassOf(method.getClassReference(),
            new ClassReference.Handle("org/hibernate/validator/internal/engine/constraintvalidation/ConstraintValidatorContextImpl"))) &&
        argIndex == 1 &&
        method.getName().equals("buildConstraintViolationWithTemplate")) {
      return true;
    }
    if ((inheritanceMap.isSubclassOf(method.getClassReference(),
            new ClassReference.Handle("org/springframework/expression/ExpressionParser")) ||
        inheritanceMap.isSubclassOf(method.getClassReference(),
            new ClassReference.Handle("org/springframework/expression/spel/standard/SpelExpressionParser"))) &&
        argIndex == 1 &&
        (method.getName().equals("parseExpression") || method.getName().equals("parseRaw"))) {
      return true;
    }
    if ((inheritanceMap.isSubclassOf(method.getClassReference(),
            new ClassReference.Handle("javax/el/ELProcessor")) &&
        argIndex == 1 && method.getName().equals("eval"))
        ||
        inheritanceMap.isSubclassOf(method.getClassReference(),
            new ClassReference.Handle("javax/el/ExpressionFactory")) &&
            argIndex == 2 && method.getName().equals("createValueExpression")) {
      return true;
    }
    return false;
  }

  private Map<ClassReference.Handle, Set<MethodReference>> slinksMapCache = null;

  private boolean isSQLInjectSink(MethodReference.Handle method, int argIndex,
      InheritanceMap inheritanceMap) {
    if (config.getName().equals("sqlinject")) {
      if (slinksMapCache == null) {
        Map<ClassReference.Handle, Set<MethodReference>> slinksMap = DataLoader.loadSlinks();
        slinksMapCache = slinksMap;
      }
      if (slinksMapCache.containsKey(method.getClassReference()) &&
          slinksMapCache.get(method.getClassReference()).stream()
              .filter(methodReference -> methodReference.equals(method)).count() > 0) {
        return true;
      }
    }
    if (inheritanceMap.isSubclassOf(method.getClassReference(),
        new ClassReference.Handle("org/springframework/jdbc/core/StatementCallback")) &&
        method.getName().equals("doInStatement")) {
      return true;
    }
    return false;
  }

  public static void main(String[] args) throws Exception {
    GadgetChainDiscovery gadgetChainDiscovery = new GadgetChainDiscovery(
        new JavaDeserializationConfig());
    gadgetChainDiscovery.discover(null);
  }
}
