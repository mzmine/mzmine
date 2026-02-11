/*
 * Copyright (c) 2004-2025 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.util.web;

import io.github.mzmine.util.web.proxy.FullProxyConfig;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Builder;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.security.auth.login.ConfigurationSpi;

public class ProxyTestUtils {

  private static final Logger logger = Logger.getLogger(ProxyTestUtils.class.getName());

  /**
   * logs and returns the test results
   */
  public static String logProxyState(String msg) {
    logger.info(msg);

    List<String> testUrls = List.of("https://auth.mzio.io/health", "https://mzio.io",
        "https://zenodo.org/", "https://github.com/");

    StringBuilder sb = new StringBuilder();
    sb.append(msg).append("\n");
    logProxyConfig(sb);
    logSystemProperties(sb);
    sb.append("\n");
    logProxyDetails(sb, testUrls);

    final String message = sb.toString();
    logger.info(message);
    return message;
  }

  private static void logProxyConfig(StringBuilder sb) {
    final FullProxyConfig config = ProxyUtils.getConfig();
    sb.append("Current proxy config: ").append(config).append("\n");
  }

  /**
   * Logs the standard Java networking system properties related to proxies.
   */
  private static void logSystemProperties(StringBuilder sb) {
    sb.append("Checking Java System Properties for Proxies...\n");

    String[] properties = {"java.net.useSystemProxies", "http.proxyHost", "http.proxyPort",
        "http.nonProxyHosts", "https.proxyHost", "https.proxyPort", "ftp.proxyHost",
        "ftp.proxyPort", "socksProxyHost", "socksProxyPort"};

    boolean found = false;
    for (String prop : properties) {
      String value = System.getProperty(prop);
      if (value != null && !value.trim().isEmpty()) {
        // 3. Use the logger for informational output
        sb.append("System Property: ").append(prop).append(" = ").append(value).append("\n");
        found = true;
      }
    }

    if (!found) {
      sb.append("No standard proxy system properties are set.\n");
    }
  }

  private static void logProxyDetails(StringBuilder sb, List<String> urls) {
    ProxySelector defaultSelector = ProxySelector.getDefault();
    if (defaultSelector == null) {
      sb.append("No default proxy selector is set.\n");
      return;
    }
    for (String url : urls) {
      try {
        URI testUri = new URI(url);
        List<Proxy> proxies = defaultSelector.select(testUri);

        if (proxies == null || proxies.isEmpty()) {
          sb.append("No proxies returned from the selector for ").append(url).append(".\n");
          return;
        }

        sb.append("Detected ").append(proxies.size())
            .append(" proxy setting(s) via ProxySelector for ").append(url).append(":\n");

        for (Proxy proxy : proxies) {
          logProxyDetails(sb, url, proxy);
        }

      } catch (Exception e) {
        // 2. Log exceptions at a SEVERE level, including the stack trace
        logger.log(Level.SEVERE, "An error occurred while detecting proxies\n", e);
        sb.append("An error occurred while detecting proxies for ").append(url).append(": ")
            .append(e.getMessage()).append("\n");
      }
    }
  }

  private static void logProxyDetails(StringBuilder sb, String url, Proxy proxy) {
    sb.append("--> Proxy Type: ").append(proxy.type()).append("\n");

    switch (proxy.type()) {
      case DIRECT -> sb.append("    Connection will be made directly, without a proxy.\n");
      case HTTP -> {
        InetSocketAddress addr = (InetSocketAddress) proxy.address();
        sb.append("    For target URL: ").append(url).append("\n");
        sb.append("    HTTP Proxy Name: ").append(addr.getHostName()).append("\n");
        sb.append("    HTTP Proxy Server: ").append(addr.getHostString()).append("\n");
        sb.append("    Port: ").append(addr.getPort()).append("\n");
      }
      case SOCKS -> {
        InetSocketAddress addr = (InetSocketAddress) proxy.address();
        sb.append("    For target URL: ").append(url).append("\n");
        sb.append("    SOCKS Proxy Name: ").append(addr.getHostName()).append("\n");
        sb.append("    SOCKS Proxy Server: ").append(addr.getHostString()).append("\n");
        sb.append("    Port: ").append(addr.getPort()).append("\n");
      }
      default -> logger.warning("    An unknown proxy type was detected.\n");
    }
  }

  public static String testAll() {
    // use health endpoint as it does not use redirect
    List<String> testUrls = List.of("https://auth.mzio.io/health", "https://mzio.io",
        "https://zenodo.org/", "https://pubchem.ncbi.nlm.nih.gov/", "https://github.com/");

    return testUrls(testUrls);
  }

  public static String testUrls(List<String> urls) {
    StringBuilder sb = new StringBuilder();
    final ProxySelector selector = ProxySelector.getDefault();
    sb.append(testJdkClient(urls, "JDK NULL selector", null, Redirect.NORMAL));
    sb.append("\n");
    sb.append(testJdkClient(urls, "JDK default selector", selector, Redirect.NORMAL));
    sb.append("\n");
    sb.append(testJdkClient(urls, "JDK default selector", selector, Redirect.NEVER));
    sb.append("\n");
    sb.append(testJdkClient(urls, "JDK default selector", selector, Redirect.ALWAYS));
    sb.append("\n");
    // test apache client - not in this package and java client should work
//    sb.append(testApacheClient(urls, "Apache NULL SYSTEM selector", null, true));
//    sb.append(testApacheClient(urls, "Apache NULL selector", null));
//    sb.append(testApacheClient(urls, "Apache default selector", selector));

    return sb.toString();
  }

  /**
   * uses the default client with or without proxy selector that is usually used.
   *
   */
  public static boolean testDefaultClient(boolean useDefaultProxy) {
    final String url = "https://auth.mzio.io/health";
    HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET()
        .timeout(Duration.ofSeconds(10L)).build();
    try (HttpClient client = HttpUtils.createHttpClient(useDefaultProxy)) {
      HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());

      if (HttpUtils.isOk(response.statusCode())) {
        return true;
      }
    } catch (Exception e) {
    }
    return false;
  }

  public static String testJdkClient(List<String> urls, String title, ProxySelector selector,
      Redirect redirect) {
    StringBuilder sb = new StringBuilder();
    sb.append(title).append(" using redirect: ").append(redirect).append("; results: \n");

    final Builder builder = HttpClient.newBuilder().followRedirects(redirect);
    if (selector != null) {
      builder.proxy(selector);
    }
    try (var client = builder.build()) {
      for (String url : urls) {
        try {
          HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url))
              .timeout(Duration.ofSeconds(30)).GET().build();

          HttpResponse<Void> response = client.send(request,
              HttpResponse.BodyHandlers.discarding());

          if (response.statusCode() >= 200 && response.statusCode() < 300) {
            sb.append("success (%s); ".formatted(url));
          } else {
            sb.append("failed %d (%s); ".formatted(response.statusCode(), url));
          }
        } catch (Exception e) {
          sb.append("error connecting (%s; message: %s); ".formatted(url, e.getMessage()));
        }
      }
    } catch (Exception e) {
      sb.append("error creating client: ").append(e.getMessage()).append("; ");
    }
    final String message = sb.toString();
    logger.info(message);
    return message;
  }

  /**
   * APACHE CLIENT IS NOT AVAILABLE IN UTILS PACKAGE. This is only needed if the default http client
   * does not work then maybe we should use apache or other client.
   */
//  private static String testApacheClient(List<String> urls, String title, ProxySelector selector) {
//    return testApacheClient(urls, title, selector, false);
//  }

//  private static String testApacheClient(List<String> urls, String title, ProxySelector selector,
//      boolean useSystemProxy) {
//    StringBuilder sb = new StringBuilder();
//    sb.append(title).append(" useSystemProxy: ").append(useSystemProxy).append("; results: \n");
//
//    final HttpClientBuilder clientBuilder = HttpClients.custom();
//    if (useSystemProxy) {
//      clientBuilder.useSystemProperties();
//    }
//
//    if (selector != null) {
//      clientBuilder.setRoutePlanner(new SystemDefaultRoutePlanner(selector));
//    }
//
//    try (org.apache.http.impl.client.CloseableHttpClient client = clientBuilder.build()) {
//
//      for (String url : urls) {
//        try {
//          HttpGet request = new HttpGet(url);
//          var response = client.execute(request);
//
//          if (response.getStatusLine().getStatusCode() >= 200
//              && response.getStatusLine().getStatusCode() < 300) {
//            sb.append("success (%s); ".formatted(url));
//          } else {
//            sb.append("failed %d (%s); ".formatted(response.getStatusLine().getStatusCode(), url));
//          }
//        } catch (Exception e) {
//          sb.append("error connecting (%s; message: %s); ".formatted(url, e.getMessage()));
//        }
//      }
//    } catch (Exception e) {
//      sb.append("error creating client: ").append(e.getMessage()).append("; ");
//    }
//    final String message = sb.toString();
//    logger.info(message);
//    return message;
//  }
}
