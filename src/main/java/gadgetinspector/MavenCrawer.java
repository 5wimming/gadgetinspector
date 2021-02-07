package gadgetinspector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Queue;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import javax.net.ssl.SSLContext;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author threedr3am
 */
public class MavenCrawer {
  private static final Logger LOGGER = LoggerFactory.getLogger(MavenCrawer.class);

  private static SSLConnectionSocketFactory sslsf;
  private static HttpClientBuilder httpClientBuilder;
  static {
    try {
      SSLContext sslContext = new SSLContextBuilder()
          .loadTrustMaterial(null, new TrustStrategy() {
            // 信任所有
            public boolean isTrusted(X509Certificate[] chain,
                String authType)
                throws CertificateException {
              return true;
            }
          }).build();
      sslsf = new SSLConnectionSocketFactory(
          sslContext);
    } catch (Exception e) {
      e.printStackTrace();
    }
    httpClientBuilder = HttpClients
        .custom()
        .setProxy(new HttpHost("127.0.0.1", 7890))
        .disableRedirectHandling()
        .disableCookieManagement()
    ;
    httpClientBuilder.setSSLSocketFactory(sslsf);
  }

  public static void start() {
    crawBusExecutor.execute(MavenCrawer::crawJar);
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      downloadExecutor.shutdown();
      crawWorkerExecutor.shutdown();
      crawBusExecutor.shutdown();
      LOGGER.info("程序关闭，shutdown all thread!");
    }));
  }

  /**
   * 从maven爬取jar包
   */
  private static void crawJar() {
    int count = 0;
    Queue<String> newScanJarHistoryAppend = new LinkedBlockingDeque<>();
    Set<String> scanJarHistory = new HashSet<>();
    Path filePath = Paths.get("history/craw-history.dat");
    //读爬取历史jar
    try {
      if (ConfigHelper.crawMaven && Files
          .exists(filePath)) {
        try (InputStream inputStream = Files
            .newInputStream(filePath);
            Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8.name())) {
          while (scanner.hasNext()) {
            String jar = scanner.nextLine();
            String name = null;
            int index = jar.indexOf(".");
            if (index != -1) {
              name = jar.substring(0, index);
              index = name.lastIndexOf("-");
              if (index != -1) {
                name = name.substring(0, index);
              }
            }
            if (name != null) {
              scanJarHistory.add(name);
            }
            if (jar.length() > 0) {
              scanJarHistory.add(jar.trim());
            }
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    //爬虫根线程
    crawBusExecutor.execute(() -> {
      String parentUrl = "https://repo1.maven.org/maven2/org/springframework/";
      craw(newScanJarHistoryAppend, scanJarHistory, parentUrl, 0);
    });

    //写爬取历史jar
    try {
      if (!Files.exists(filePath))
        Files.createFile(filePath);
      try (OutputStream outputStream = Files.newOutputStream(filePath, StandardOpenOption.APPEND);
          Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
        while (true) {
          String jar = newScanJarHistoryAppend.poll();
          if (jar != null && jar.length() > 0) {
            writer.write(jar);
            writer.write("\n");
            writer.flush();
            if (++count % 10 == 0) {
              LOGGER.info("-------------------this task already craw " + count
                  + " jars------------------");
            }
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  //爬虫根线程池（只爬level=0的根）
  private static ExecutorService crawBusExecutor = Executors.newFixedThreadPool(8);
  //下载线程池
  private static ExecutorService downloadExecutor = Executors.newFixedThreadPool(8);
  //爬虫线程池
  private static ExecutorService crawWorkerExecutor = Executors.newFixedThreadPool(8);

  /**
   * 核心爬取逻辑
   *
   * @param newScanJarHistoryAppend
   * @param scanJarHistory
   * @param parentUrl
   * @param level
   */
  private static void craw(Queue<String> newScanJarHistoryAppend, Set<String> scanJarHistory,
      String parentUrl, int level) {
    try {
      //检测该分支是否已爬取过
      if (checkDir(parentUrl))
        return;
      String content = crawHtmlContent(parentUrl);
      if (content != null || content.length() >= 0) {
        Document doc = Jsoup.parse(content);
        if (doc != null) {
          Elements contents = doc.select("pre");
          if (contents != null || contents.size() >= 0) {
            Elements as = contents.first().select("a");
            for (int i = as.size() - 1; i > 0; i--) {
              Element a = as.get(i);
              String href = a.attr("href");
              if (href.equals("../")) {
                continue;
              }
              if (href.endsWith(".jar")) {
                String jar = href;
                String name = null;
                int index = jar.indexOf(".");
                if (index != -1) {
                  name = jar.substring(0, index);
                  index = name.lastIndexOf("-");
                  if (index != -1) {
                    name = name.substring(0, index);
                  }
                }
                if (!jar.contains("/") && !jar.contains("javadoc") && !jar
                    .contains("sources") && !jar.contains("test")) {
                  if (name != null && !scanJarHistory.contains(name) && !scanJarHistory.contains(jar)) {
                    scanJarHistory.add(name);
                    downloadExecutor.execute(() -> {
                      if (download(parentUrl, href)) {
                        newScanJarHistoryAppend
                            .offer(jar);
                      }
                    });
                    continue;
                  }
                }
              } else if (href.endsWith("/")) {
                if (level == 0) {
                  //level=1的节点，都用多worker线程处理，每个线程处理一个1级分支
                  crawWorkerExecutor.execute(() -> {
                    craw(newScanJarHistoryAppend, scanJarHistory, parentUrl + href,
                        level + 1);
                  });
                } else {
                  //非根非1级节点，都继续用当前线程递归，直到jar包为止
                  craw(newScanJarHistoryAppend, scanJarHistory, parentUrl + href,
                      level + 1);
                }
              }
            }
          }
        }
      }
      //记录符合层级的分支目录，下次启动程序时，不用爬取也可以快速跳过
      over(parentUrl, level);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * 已被爬取过的目录历史
   */
  private static Set<String> mavenDirHistory;

  private static void over(String parentUrl, int level) {
    String saveDir = parentUrl.replace("https://repo1.maven.org/maven2", "");
    String[] tmp = saveDir.split("/");
    if (tmp == null || tmp.length == 2 ? level != 2 : level != 1) {
      return;
    }
    if (ConfigHelper.crawMaven) {
      if (!mavenDirHistory.contains(saveDir)) {
        mavenDirHistory.add(saveDir);
        try {
          Path filePath = Paths.get("history/craw-dir-history.dat");
          if (!Files.exists(filePath))
            Files.createFile(filePath);
          try (OutputStream outputStream = Files.newOutputStream(filePath, StandardOpenOption.APPEND);
              Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
            writer.write(saveDir);
            writer.write("\n");
            writer.flush();
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
    return;
  }

  private static boolean checkDir(String parentUrl) {
    String checkDir = parentUrl.replace("https://repo1.maven.org/maven2", "");
    if (mavenDirHistory == null) {
      mavenDirHistory = new HashSet<>();
      Path filePath = Paths.get("history/craw-dir-history.dat");
      if (Files.exists(filePath)) {
        try (InputStream inputStream = Files
            .newInputStream(filePath);
            Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8.name())) {
          while (scanner.hasNext()) {
            String dir = scanner.nextLine();
            if (dir.length() > 0) {
              mavenDirHistory.add(dir.trim());
            }
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
    return mavenDirHistory.contains(checkDir);
  }

  /**
   * 爬取html内容
   *
   * @param url
   * @return
   */
  private static String crawHtmlContent(String url) {
    LOGGER.info("craw html content:" + url + " ... ");
    RequestConfig timeoutConfig = RequestConfig.custom()
        .setConnectTimeout(60000).setConnectionRequestTimeout(60000)
        .setSocketTimeout(60000).build();
    HttpGet httpGet = new HttpGet(url);
    httpGet.setConfig(timeoutConfig);
    try {
      CloseableHttpClient httpClient = null;
      CloseableHttpResponse response = null;
      try {
        httpClient = httpClientBuilder.build();
        response = httpClient.execute(httpGet);
        int status = response.getStatusLine().getStatusCode();
        if (status == 200) {
          StringBuilder stringBuilder = new StringBuilder(1024);
          BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
          String line;
          while ((line = bufferedReader.readLine()) != null)
            stringBuilder.append(line);
          return stringBuilder.toString();
        } else {
          LOGGER.error("craw html content url:" + url + " fail!");
        }
      } finally {
        response.close();
        httpClient.close();
        httpGet.releaseConnection();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  /**
   * 下载jar包
   *
   * @param href
   * @param file
   */
  private static boolean download(String href, String file) {
    LOGGER.info("download jar:" + href + file + " ... ");
    String dir = ConfigHelper.crawMavenJarPath;
    RequestConfig timeoutConfig = RequestConfig.custom()
        .setConnectTimeout(60000).setConnectionRequestTimeout(60000)
        .setSocketTimeout(60000).build();
    HttpGet httpGet = new HttpGet(href + file);
    httpGet.setConfig(timeoutConfig);
    try {
      CloseableHttpClient httpClient = null;
      CloseableHttpResponse response = null;
      try {
        httpClient = httpClientBuilder.build();
        response = httpClient.execute(httpGet);
        int status = response.getStatusLine().getStatusCode();
        if (status == 200) {
          Path path = Paths.get(dir + file);
          if (!Files.exists(path)) {
            Files.copy(response.getEntity().getContent(), path);
          }
        } else {
          LOGGER.error("download jar:" + href + file + " fail!");
          return false;
        }
      } finally {
        response.close();
        httpClient.close();
        httpGet.releaseConnection();
      }
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }
}
