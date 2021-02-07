package gadgetinspector.fastjson;

import gadgetinspector.ConfigHelper;
import gadgetinspector.SourceDiscovery;
import gadgetinspector.data.ClassReference;
import gadgetinspector.data.GraphCall;
import gadgetinspector.data.InheritanceMap;
import gadgetinspector.data.MethodReference;
import gadgetinspector.data.Source;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FastjsonSourceDiscovery extends SourceDiscovery {

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
  public void discover(Map<ClassReference.Handle, ClassReference> classMap,
      Map<MethodReference.Handle, MethodReference> methodMap,
      InheritanceMap inheritanceMap, Map<MethodReference.Handle, Set<GraphCall>> graphCallMap) {

    final FastjsonSerializableDecider serializableDecider = new FastjsonSerializableDecider(
        methodMap);

    for (MethodReference.Handle method : methodMap.keySet()) {
      if (serializableDecider.apply(method.getClassReference())) {
        if (skipList.contains(method.getClassReference().getName())) {
          continue;
        }
        if (!checkFastjsonBlackList(method.getClassReference().getName().replace("/","."))) {
          continue;
        }
        if (method.getClassReference().getName().startsWith("javafx"))
          continue;
        if (method.getClassReference().getName().startsWith("javax"))
          continue;
        if (method.getName().startsWith("get") && method.getName().length() > 3 && method.getDesc().startsWith("()")) {
          if (method.getDesc().matches("\\(\\)Ljava/util/Collection;") ||
              method.getDesc().matches("\\(\\)Ljava/util/Map;") ||
              method.getDesc().matches("\\(\\)Ljava/util/concurrent/atomic/AtomicBoolean;") ||
              method.getDesc().matches("\\(\\)Ljava/util/concurrent/atomic/AtomicInteger;") ||
              method.getDesc().matches("\\(\\)Ljava/util/concurrent/atomic/AtomicInteger;") ||
              method.getDesc().matches("\\(\\)Ljava/lang/Object;")) {
            String setterName = "set" + method.getName().charAt(3) + method.getName().substring(4);
            String desc = "(L" + method.getDesc().substring(method.getDesc().indexOf(")L") + 2, method.getDesc().length() - 1) + ";)V";
            MethodReference.Handle handle = new MethodReference.Handle(method.getClassReference(), setterName, desc);
            if (!methodMap.containsKey(handle)) {
              addDiscoveredSource(new Source(method, 0));
            }
          }
        }
        if (method.getName().startsWith("set") && method.getDesc().matches("\\(L[\\w/$]+?;\\)V")) {
//        if (method.getName().startsWith("set") && (method.getDesc().contains("(Ljava/lang/String;)V"))) {
          addDiscoveredSource(new Source(method, 1));
        }
      }
    }
  }

  private boolean checkFastjsonBlackList(String className) {
    final long BASIC = 0xcbf29ce484222325L;
    final long PRIME = 0x100000001b3L;

    final long h1 = (BASIC ^ className.charAt(0)) * PRIME;
    if (h1 == 0xaf64164c86024f1aL) { // [
      return false;
    }

    if ((h1 ^ className.charAt(className.length() - 1)) * PRIME == 0x9198507b5af98f0L) {
      return false;
    }

    final long h3 = (((((BASIC ^ className.charAt(0))
        * PRIME)
        ^ className.charAt(1))
        * PRIME)
        ^ className.charAt(2))
        * PRIME;

    long hash = h3;
    for (int i = 3; i < className.length(); ++i) {
      hash ^= className.charAt(i);
      hash *= PRIME;
      if (Arrays.binarySearch(denyHashCodes, hash) >= 0) {
        return false;
      }
    }
    return true;
  }

  private long[] denyHashCodes;

  {
    denyHashCodes = new long[]{
        0x80D0C70BCC2FEA02L,
        0x86FC2BF9BEAF7AEFL,
        0x87F52A1B07EA33A6L,
        0x8EADD40CB2A94443L,
        0x8F75F9FA0DF03F80L,
        0x9172A53F157930AFL,
        0x92122D710E364FB8L,
        0x92122D710E364FB8L,
        0x94305C26580F73C5L,
        0x9437792831DF7D3FL,
        0xA123A62F93178B20L,
        0xA85882CE1044C450L,
        0xAA3DAFFDB10C4937L,
        0xAFFF4C95B99A334DL,
        0xB40F341C746EC94FL,
        0xB7E8ED757F5D13A2L,
        0xBCDD9DC12766F0CEL,
        0xC00BE1DEBAF2808BL,
        0xC2664D0958ECFE4CL,
        0xC7599EBFE3E72406L,
        0xC963695082FD728EL,
        0xD1EFCDF4B3316D34L,
        0xD9C9DBF6BBD27BB1L,
        0xDF2DDFF310CDB375L,
        0xE09AE4604842582FL,
        0xE1919804D5BF468FL,
        0xE2EB3AC7E56C467EL,
        0xE603D6A51FAD692BL,
        0xE9184BE55B1D962AL,
        0xE9F20BAD25F60807L,
        0xFC773AE20C827691L,
        0xFD5BFC610056D720L,
        0xFFDD1A80F1ED3405L,
        0x10E067CD55C5E5L,
        0x761619136CC13EL,
        0x3085068CB7201B8L,
        0x45B11BC78A3ABA3L,
        0xB6E292FA5955ADEL,
        0xEE6511B66FD5EF0L,
        0x10B2BDCA849D9B3EL,
        0x144277B467723158L,
        0x14DB2E6FEAD04AF0L,
        0x154B6CB22D294CFAL,
        0x193B2697EAAED41AL,
        0x1E0A8C3358FF3DAEL,
        0x24D2F6048FEF4E49L,
        0x275D0732B877AF29L,
        0x2ADFEFBBFE29D931L,
        0x2B3A37467A344CDFL,
        0x2D308DBBC851B0D8L,
        0x313BB4ABD8D4554CL,
        0x332F0B5369A18310L,
        0x339A3E0B6BEEBEE9L,
        0x33C64B921F523F2FL,
        0x34A81EE78429FDF1L,
        0x3826F4B2380C8B9BL,
        0x398F942E01920CF0L,
        0x42D11A560FC9FBA9L,
        0x43320DC9D2AE0892L,
        0x440E89208F445FB9L,
        0x46C808A4B5841F57L,
        0x4A3797B30328202CL,
        0x4BA3E254E758D70DL,
        0x4EF08C90FF16C675L,
        0x4FD10DDC6D13821FL,
        0x527DB6B46CE3BCBCL,
        0x5728504A6D454FFCL,
        0x599B5C1213A099ACL,
        0x5A5BD85C072E5EFEL,
        0x5AB0CB3071AB40D1L,
        0x5D74D3E5B9370476L,
        0x5D92E6DDDE40ED84L,
        0x62DB241274397C34L,
        0x63A220E60A17C7B9L,
        0x6749835432E0F0D2L,
        0x746BD4A53EC195FBL,
        0x74B50BB9260E31FFL,
        0x75CC60F5871D0FD3L,
        0x767A586A5107FEEFL,
        0x7AA7EE3627A19CF3L
    };
  }
}
