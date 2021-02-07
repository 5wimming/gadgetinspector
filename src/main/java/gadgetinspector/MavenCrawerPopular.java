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
public class MavenCrawerPopular {
  private static final Logger LOGGER = LoggerFactory.getLogger(MavenCrawerPopular.class);

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
    crawBusExecutor.execute(MavenCrawerPopular::crawJar);
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      downloadExecutor.shutdown();
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
    Path filePath = Paths.get("history/popular-craw-history.dat");
    //读爬取历史jar
    try {
      if (ConfigHelper.onlyCrawMavenPopular && Files
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
      craw(newScanJarHistoryAppend, scanJarHistory);
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
  private static ExecutorService crawBusExecutor = Executors.newFixedThreadPool(2);
  //下载线程池
  private static ExecutorService downloadExecutor = Executors.newFixedThreadPool(8);

  /**
   * 核心爬取逻辑
   *
   * @param newScanJarHistoryAppend
   * @param scanJarHistory
   */
  private static void craw(Queue<String> newScanJarHistoryAppend, Set<String> scanJarHistory) {
    String root = "https://mvnrepository.com/popular";
    for (int i = 1; i <= 10; i++) {
      String url = root + "?p=" + i;
      LOGGER.info("craw " + url + " ... ");
      try {
        String content = crawHtmlContent(url);
        if (content == null || content.length() <= 0)
          continue;
        Document doc = Jsoup.parse(content);
        if (doc == null)
          continue;
        Elements imTitles = doc.getElementsByClass("im-title");
        if (imTitles == null || imTitles.size() <= 0)
          continue;
        for (Element imTitle : imTitles) {
          Elements hrefs = imTitle.select("a[href]");
          if (hrefs.size() <= 0)
            continue;
          String href = hrefs.first().attr("href");
          if (!href.startsWith("/") || href.endsWith("/"))
            continue;

          content = crawHtmlContent("https://mvnrepository.com" + href);
          if (content == null || content.length() <= 0)
            continue;
          doc = Jsoup.parse(content);
          if (doc == null)
            continue;
          Elements vbtns = doc.getElementsByClass("vbtn");
          if (vbtns == null || vbtns.size() <= 0)
            continue;
          Element vbtn = vbtns.first();
          if (vbtn == null)
            continue;
          String href2 = vbtn.attr("href");
          if (href2.startsWith("/") || href.endsWith("/"))
            continue;
          href2 = href2.substring(href2.indexOf("/"));

          content = crawHtmlContent("https://mvnrepository.com" + href + href2);
          if (content == null || content.length() <= 0)
            continue;
          doc = Jsoup.parse(content);
          if (doc == null)
            continue;
          vbtns = doc.getElementsByClass("vbtn");
          if (vbtns == null || vbtns.size() <= 0)
            continue;
          for (Element element : vbtns) {
            href = element.attr("href");
            if (href != null && href.length() > 0 && href.endsWith(".jar")) {
              String jar = href.substring(href.lastIndexOf("/") + 1);
                String name = null;
                int index = jar.indexOf(".");
                if (index != -1) {
                  name = jar.substring(0, index);
                  index = name.lastIndexOf("-");
                  if (index != -1) {
                    name = name.substring(0, index);
                  }
                }
                if (!jar.contains("/") && !jar.contains("javadoc") && !jar.contains("sources") && !jar.contains("test")) {
                  if (name != null && !scanJarHistory.contains(name) && !scanJarHistory.contains(jar)) {
                    scanJarHistory.add(name);
                    String finalHref = href;
                    downloadExecutor.execute(() -> {
                      if (download(finalHref, jar)) {
                        newScanJarHistoryAppend
                            .offer(jar);
                      }
                    });
                  }
                  break;
                }
              }
            }
          }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
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
    LOGGER.info("download jar:" + href + " ... ");
    String dir = ConfigHelper.crawMavenJarPath;
    RequestConfig timeoutConfig = RequestConfig.custom()
        .setConnectTimeout(60000).setConnectionRequestTimeout(60000)
        .setSocketTimeout(60000).build();
    HttpGet httpGet = new HttpGet(href);
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
          LOGGER.error("download jar:" + href + " fail!");
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
