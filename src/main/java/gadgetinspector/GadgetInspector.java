package gadgetinspector;

import gadgetinspector.config.ConfigRepository;
import gadgetinspector.config.GIConfig;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point for running an end-to-end analysis. Deletes all data files before starting and writes discovered
 * gadget chains to gadget-chains.txt.
 */
public class GadgetInspector {
    private static final Logger LOGGER = LoggerFactory.getLogger(GadgetInspector.class);

    private static void printUsage() {
        System.out.println("Usage:\n  Pass either a single argument which will be interpreted as a WAR, or pass " +
                "any number of arguments which will be intepretted as a list of JARs forming a classpath.");

    }

    static {
        try {
            if (!Files.exists(Paths.get("gadget-result"))) {
                Files.createDirectory(Paths.get("gadget-result"));
            }
            if (!Files.exists(Paths.get("conf"))) {
                Files.createDirectory(Paths.get("conf"));
            }
            if (!Files.exists(Paths.get("history"))) {
                Files.createDirectory(Paths.get("history"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            printUsage();
            System.exit(1);
        }

        //配置log4j用于输出日志
        configureLogging();

        //是否不删除所有的dat文件
        boolean resume = false;
        //是否Spring-Boot jar项目
        boolean boot = false;
        //fuzz类型，默认java原生序列化
        GIConfig config = ConfigRepository.getConfig("jserial");

        int argIndex = 0;
        while (argIndex < args.length) {
            String arg = args[argIndex];
            if (!arg.startsWith("--")) {
                break;
            }
            if (arg.equals("--resume")) {
                //不删除dat文件
                resume = true;
            } else if (arg.equals("--config")) {
                //--config参数指定fuzz类型
                config = ConfigRepository.getConfig(args[++argIndex]);
                if (config == null) {
                    throw new IllegalArgumentException("Invalid config name: " + args[argIndex]);
                }
                ConfigHelper.giConfig = config;
            } else if (arg.equals("--boot")) {
                //指定为Spring-Boot jar项目
                boot = true;
            } else if (arg.equals("--mybatis.xml")) {
                //mybatis mapper xml目录位置
                ConfigHelper.mybatisMapperXMLPath = args[++argIndex];
            } else if (arg.equals("--noTaintTrack")) {
                //是否污点分析，若不使用污点分析，将会把所有链都搜索出来，好处是不会遗漏，坏处是需要大量的人工审计
                ConfigHelper.taintTrack = false;
            } else if (arg.equals("--opLevel")) {
                //链聚合优化等级，--OpLevel 1表示一层优化，默认0不优化
                ConfigHelper.opLevel = Integer.parseInt(args[++argIndex]);
            } else if (arg.equals("--history")) {
                //启用历史扫描jar包记录，方便大规模扫描时不重复扫描旧jar包，好处时减少工作时间，坏处是遇到依赖组合的gadget可能扫不出来
                ConfigHelper.history = true;
                ConfigHelper.historyRecordFile += args[++argIndex];
            } else if (arg.equals("--max")) {
                //大规模扫描jar时，指定最大数量
                ConfigHelper.maxJarCount = Integer.parseInt(args[++argIndex]);
            }else if (arg.equals("--similarLevel")) {
                //大规模扫描jar时，指定最大数量
                ConfigHelper.similarLevel = Integer.parseInt(args[++argIndex]);
            } else if (arg.equals("--onlyJDK")) {
                //仅扫描jdk
                ConfigHelper.onlyJDK = true;
            } else if (arg.equals("--maxChainLength")) {
                //仅输出小于该链长度的链
                ConfigHelper.maxChainLength = Integer.parseInt(args[++argIndex]);
            } else if (arg.equals("--craw")) {
                //启用爬虫功能，每*分钟分析一遍，出一次报告。若配置--crawMaven，则使用内置爬虫功能，爬取maven仓库
                ConfigHelper.craw = true;
                ConfigHelper.crawMin = Integer.parseInt(args[++argIndex]);
            } else if(arg.equals("--maxRepeatBranchesTimes")){
                ConfigHelper.maxRepeatBranchesTimes = Integer.parseInt(args[++argIndex]);
            } else if (arg.equals("--crawMaven")) {
                //爬取maven仓库
                ConfigHelper.crawMaven = true;
                ConfigHelper.crawMavenJarPath = args[++argIndex];
                if (!ConfigHelper.crawMavenJarPath.endsWith("/"))
                    ConfigHelper.crawMavenJarPath += "/";
            } else if (arg.equals("--onlyCrawMaven")) {
                //只爬取maven仓库，不挖掘gadget
                ConfigHelper.onlyCrawMaven = true;
            } else if (arg.equals("--onlyCrawNexus")) {
                //只爬取nexus本地仓库，不挖掘gadget
                ConfigHelper.onlyCrawNexus = true;
            } else if (arg.equals("--onlyCrawMavenPopular")) {
                //只爬取maven仓库popular，不挖掘gadget
                ConfigHelper.onlyCrawMavenPopular = true;
            } else if (arg.equals("--skipSourcesFile")) {
                //跳过的sources
                ConfigHelper.skipSourcesFile = args[++argIndex];
            } else if (arg.equals("--slinksFile")) {
                //自定义slinks，每行空格分离三段（
                // 类 方法名 方法描述，例：java/rmi/registry/Registry lookup (Ljava/lang/String;)Ljava/rmi/Remote;），
                // 缺少的不判断（例：java/rmi/registry/Registry lookup，不判断方法描述）
                ConfigHelper.slinksFile = args[++argIndex];
            } else if (arg.equals("--slink")) {
              for (int i = argIndex + 1; i < args.length - 1; i++) {
                if (!args[i].startsWith("--")) {
                  ConfigHelper.slinks.add(args[++argIndex]);
                } else {
                  break;
                }
              }
            } else {
                throw new IllegalArgumentException("Unexpected argument: " + arg);
            }

            argIndex += 1;
        }

        if (ConfigHelper.crawMaven) {
            if (!ConfigHelper.onlyCrawMavenPopular && !ConfigHelper.onlyCrawNexus) {
                MavenCrawer.start();
            }
            if (!ConfigHelper.onlyCrawMaven && !ConfigHelper.onlyCrawNexus) {
                MavenCrawerPopular.start();
            }
            if (!ConfigHelper.onlyCrawMaven && !ConfigHelper.onlyCrawMavenPopular) {
//                MavenCrawerNexus.start();
            }
        }

        for (int i = 0; (i < 1 || ConfigHelper.craw) && (!ConfigHelper.onlyCrawMaven && !ConfigHelper.onlyCrawNexus && !ConfigHelper.onlyCrawMavenPopular); i++) {
            AtomicBoolean haveNewJar = new AtomicBoolean(false);
            List<Path> pathList = new ArrayList<>();
            ClassLoader classLoader = initJarData(args, boot, argIndex, haveNewJar, pathList);
            if (ConfigHelper.craw && !haveNewJar.getAndSet(false)) {
                Thread.sleep(60 * 1000L);
                continue;
            }

            //类枚举加载器，具有两个方法
            //getRuntimeClasses获取rt.jar的所有class
            //getAllClasses获取rt.jar以及classLoader加载的class
            final ClassResourceEnumerator classResourceEnumerator = new ClassResourceEnumerator(
                    classLoader);

            //删除所有的dat文件
            if (!resume) {
                // Delete all existing dat files
                LOGGER.info("Deleting stale data...");
                for (String datFile : Arrays
                    .asList("classes.dat", "methods.dat", "inheritanceMap.dat",
                        "passthrough.dat", "callgraph.dat", "sources.dat", "methodimpl.dat",
                        "slinks.dat")) {
                    final Path path = Paths.get(datFile);
                    if (Files.exists(path)) {
                        Files.delete(path);
                    }
                }
            }

            try {
              //扫描java runtime所有的class（rt.jar）和指定的jar或war中的所有class
              beginDiscovery(config, classResourceEnumerator, pathList);
            } catch (Throwable t) {
              t.printStackTrace();
              //捕获异常，避免异常导致程序终止
            }

            saveHistory(haveNewJar, pathList);
            Thread.sleep(ConfigHelper.crawMin * 60 * 1000L);
            LOGGER.info("Analysis complete!");
        }
    }

    /**
     * 保存已被扫描的jar包记录
     *
     * @param haveNewJar
     * @param pathList
     * @throws IOException
     */
    private static void saveHistory(AtomicBoolean haveNewJar, List<Path> pathList)
        throws IOException {
        if (ConfigHelper.history && pathList.size() > 0) {
            haveNewJar.set(true);
            Path filePath = Paths.get(ConfigHelper.historyRecordFile);
            if (!Files.exists(filePath))
                Files.createFile(filePath);
            try (OutputStream outputStream = Files
                .newOutputStream(filePath,
                    StandardOpenOption.APPEND);
                Writer writer = new OutputStreamWriter(outputStream,
                    StandardCharsets.UTF_8)) {
                for (String jar : pathList.stream().map(path -> path.getFileName().toString()).collect(
                    Collectors.toSet())) {
                    writer.write(jar);
                    writer.write("\n");
                }
                writer.flush();
            }
        }
    }

    private static ClassLoader initJarData(String[] args, boolean boot, int argIndex,
        AtomicBoolean haveNewJar, List<Path> pathList)
        throws IOException {
        ClassLoader classLoader = null;
        if (!ConfigHelper.onlyJDK) {
            //程序参数的最后一部分，即最后一个具有前缀--的参数（例：--resume）后
            if (args.length == argIndex + 1 && args[argIndex].toLowerCase().endsWith(".war")) {
                //加载war文件
                Path path = Paths.get(args[argIndex]);
                LOGGER.info("Using WAR classpath: " + path);
                //实现为URLClassLoader，加载war包下的WEB-INF/lib和WEB-INF/classes
                classLoader = Util.getWarClassLoader(path);
            } else if (args.length == argIndex + 1 && args[argIndex].toLowerCase().endsWith(".jar")
                && boot) {
                Path path = Paths.get(args[argIndex]);
                LOGGER.info("Using JAR classpath: " + path);
                //实现为URLClassLoader，加载jar包下的BOOT-INF/lib和BOOT-INF/classes
                classLoader = Util.getJarAndLibClassLoader(path);
            } else {
                //加载jar文件，java命令后部，可配置多个
                Set<String> scanJarHistory = new HashSet<>();
                Path filePath = Paths.get(ConfigHelper.historyRecordFile);
                if (ConfigHelper.history && Files.exists(filePath)) {
                    try (InputStream inputStream = Files
                        .newInputStream(filePath);
                        Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8.name())) {
                        while (scanner.hasNext()) {
                            String jar = scanner.nextLine();
                            if (jar.length() > 0) {
                                scanJarHistory.add(jar.trim());
                            }
                        }
                    }
                }
                AtomicInteger jarCount = new AtomicInteger(0);
                for (int i = 0; i < args.length - argIndex; i++) {
                    String pathStr = args[argIndex + i];
                    if (!pathStr.endsWith(".jar")) {
                        //todo 主要用于大批量的挖掘链
                        //非.jar结尾，即目录，需要遍历目录找出所有jar文件
                        File file = Paths.get(pathStr).toFile();
                        if (file == null || !file.exists())
                            continue;
                        Files.walkFileTree(file.toPath(), new SimpleFileVisitor<Path>() {
                            @Override
                            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                                if (file.getFileName().toString().endsWith(".jar")) {
                                    File readFile = file.toFile();
                                    Path path = Paths.get(readFile.getAbsolutePath());
                                    if (Files.exists(path)) {
                                        if (ConfigHelper.history) {
                                            if (!scanJarHistory
                                                .contains(path.getFileName().toString())) {
                                                if (jarCount.incrementAndGet()
                                                    <= ConfigHelper.maxJarCount) {
                                                    pathList.add(path);
                                                }
                                            }
                                        } else {
                                            if (jarCount.incrementAndGet()
                                                <= ConfigHelper.maxJarCount) {
                                                pathList.add(path);
                                            }
                                        }
                                    }
                                }
                                return FileVisitResult.CONTINUE;
                            }
                        });

                        continue;
                    }
                    Path path = Paths.get(pathStr).toAbsolutePath();
                    if (!Files.exists(path)) {
                        throw new IllegalArgumentException("Invalid jar path: " + path);
                    }
                    pathList.add(path);
                }
                LOGGER.info("Using classpath: " + Arrays.toString(pathList.toArray()));
                //实现为URLClassLoader，加载所有指定的jar
                classLoader = Util.getJarClassLoader(pathList.toArray(new Path[0]));
            }
        }
        haveNewJar.set(pathList.size() != 0);
        return classLoader;
    }

    private static void beginDiscovery(GIConfig config,
        ClassResourceEnumerator classResourceEnumerator,
        List<Path> pathList) throws Exception {
        // Perform the various discovery steps
        if (!Files.exists(Paths.get("classes.dat")) || !Files.exists(Paths.get("methods.dat"))
                || !Files.exists(Paths.get("inheritanceMap.dat"))) {
            LOGGER.info("Running method discovery...");
            MethodDiscovery methodDiscovery = new MethodDiscovery();
            methodDiscovery.discover(classResourceEnumerator);
            //保存了类信息、方法信息、继承实现信息
            methodDiscovery.save();
        }

        if (!Files.exists(Paths.get("slinks.dat")) && config.getSlinkDiscovery() != null) {
            LOGGER.info("Running slink discovery...");
            SlinkDiscovery slinkDiscovery = config.getSlinkDiscovery();
            slinkDiscovery.discover();
            slinkDiscovery.save();
        }

        if (!Files.exists(Paths.get("passthrough.dat")) && ConfigHelper.taintTrack) {
            LOGGER.info("Analyzing methods for passthrough dataflow...");
            PassthroughDiscovery passthroughDiscovery = new PassthroughDiscovery();
            //记录参数在方法调用链中的流动关联（如：A、B、C、D四个方法，调用链为A->B B->C C->D，其中参数随着调用关系从A流向B，在B调用C过程中作为入参并随着方法结束返回，最后流向D）
            //该方法主要是追踪上面所说的"B调用C过程中作为入参并随着方法结束返回"，入参和返回值之间的关联
            passthroughDiscovery.discover(classResourceEnumerator, config);
            passthroughDiscovery.save();
        }

        if (!Files.exists(Paths.get("callgraph.dat"))) {
            LOGGER.info("Analyzing methods in order to build a call graph...");
            CallGraphDiscovery callGraphDiscovery = new CallGraphDiscovery();
            //记录参数在方法调用链中的流动关联（如：A、B、C三个方法，调用链为A->B B->C，其中参数随着调用关系从A流向B，最后流C）
            //该方法主要是追踪上面所说的参数流动，即A->B入参和B->C入参的关系，以确定参数可控
            callGraphDiscovery.discover(classResourceEnumerator, config);
            callGraphDiscovery.save();
        }

        if (!Files.exists(Paths.get("sources.dat"))) {
            LOGGER.info("Discovering gadget chain source methods...");
            SourceDiscovery sourceDiscovery = config.getSourceDiscovery();
            //查找利用链的入口（例：java原生反序列化的readObject）
            sourceDiscovery.discover();
            sourceDiscovery.save();
        }

        {
            LOGGER.info("Searching call graph for gadget chains...");
            GadgetChainDiscovery gadgetChainDiscovery = new GadgetChainDiscovery(config);
            //根据上面的数据收集，最终分析利用链
            gadgetChainDiscovery.discover(pathList);
        }
    }

    private static void configureLogging() {
        ConsoleAppender console = new ConsoleAppender();
        String PATTERN = "%d %c [%p] %m%n";
        console.setLayout(new PatternLayout(PATTERN));
        console.setThreshold(Level.INFO);
        console.activateOptions();
        org.apache.log4j.Logger.getRootLogger().addAppender(console);
    }
}
