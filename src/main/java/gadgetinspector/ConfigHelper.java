package gadgetinspector;

import gadgetinspector.config.GIConfig;
import java.util.HashSet;
import java.util.Set;

/**
 * @author xuanyh
 */
public class ConfigHelper {

  public static GIConfig giConfig;

  public static String mybatisMapperXMLPath;

  public static boolean taintTrack = true;

  public static int opLevel = 0;

  public static boolean history = false;
  public static String historyRecordFile = "history/";

  public static int maxJarCount = Integer.MAX_VALUE;

  public static boolean onlyJDK = false;

  public static int maxChainLength = Integer.MAX_VALUE;
  public static int maxRepeatBranchesTimes = 20;
  public static int similarLevel = 0;

  public static boolean craw = false;
  public static int crawMin = 0;
  public static boolean crawMaven = false;
  public static boolean onlyCrawMavenPopular = false;
  public static String crawMavenJarPath = "/tmp";
  public static boolean onlyCrawMaven = false;
  public static boolean onlyCrawNexus = false;

  public static Set<String> slinks = new HashSet<>();

  public static String skipSourcesFile = "";
  public static String slinksFile = "";
}
