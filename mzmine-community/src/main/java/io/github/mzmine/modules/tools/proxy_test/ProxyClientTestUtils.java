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

package io.github.mzmine.modules.tools.proxy_test;

import io.mzio.links.MzioMZmineLinks;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Builder;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.logging.Logger;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.SystemDefaultRoutePlanner;

public class ProxyClientTestUtils {

  private static final Logger logger = Logger.getLogger(ProxyClientTestUtils.class.getName());

  public static void testAll() {
    List<String> testUrls = List.of("https://auth.mzio.io/", MzioMZmineLinks.MZIO.getUrl(),
        "https://zenodo.org/", "https://pubchem.ncbi.nlm.nih.gov/");

//    testUrls(testUrls);
  }

//  public static void testUrls(List<String> urls) {
//    testJdkClient(urls, "JDK NULL selector", null, Redirect.NORMAL);
//    testJdkClient(urls, "JDK default selector", MZmineCore.JVM_PROXY_SELECTOR, Redirect.NORMAL);
//    testJdkClient(urls, "JDK default selector", MZmineCore.JVM_PROXY_SELECTOR, Redirect.NEVER);
//    testJdkClient(urls, "JDK default selector", MZmineCore.JVM_PROXY_SELECTOR, Redirect.ALWAYS);
//    testJdkClient(urls, "Auto proxy selector",
//        MZmineCore.createAutoProxySelector(ProxyOptions.AUTO_PROXY), Redirect.NORMAL);
//    testJdkClient(urls, "Auto proxy selector",
//        MZmineCore.createAutoProxySelector(ProxyOptions.AUTO_PROXY), Redirect.NEVER);
//    testJdkClient(urls, "Auto proxy selector",
//        MZmineCore.createAutoProxySelector(ProxyOptions.AUTO_PROXY), Redirect.ALWAYS);
//    testJdkClient(urls, "Auto Browser proxy selector",
//        MZmineCore.createAutoProxySelector(ProxyOptions.BROWSER_PROXY), Redirect.NORMAL);
//    testJdkClient(urls, "Auto SYSTEM proxy selector",
//        MZmineCore.createAutoProxySelector(ProxyOptions.SYSTEM_PROXY), Redirect.NORMAL);
//    testJdkClient(urls, "Auto WIN proxy selector",
//        MZmineCore.createAutoProxySelector(ProxyOptions.WIN), Redirect.NORMAL);
//    testJdkClient(urls, "Auto WPAD proxy selector",
//        MZmineCore.createAutoProxySelector(ProxyOptions.WPAD), Redirect.NORMAL);
//
//    // test apache client
//    testApacheClient(urls, "Apache NULL SYSTEM selector", null, true);
//    testApacheClient(urls, "Apache NULL selector", null);
//    testApacheClient(urls, "Apache default selector", MZmineCore.JVM_PROXY_SELECTOR);
//    testApacheClient(urls, "Apache auto proxy selector",
//        MZmineCore.createAutoProxySelector(ProxyOptions.AUTO_PROXY));
//    testApacheClient(urls, "Apache browser proxy selector",
//        MZmineCore.createAutoProxySelector(ProxyOptions.BROWSER_PROXY));
//    testApacheClient(urls, "Apache system proxy selector",
//        MZmineCore.createAutoProxySelector(ProxyOptions.SYSTEM_PROXY));
//
//  }

  private static void testJdkClient(List<String> urls, String title, ProxySelector selector,
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
    logger.info(sb.toString());
  }

  private static void testApacheClient(List<String> urls, String title, ProxySelector selector) {
    testApacheClient(urls, title, selector, false);
  }

  private static void testApacheClient(List<String> urls, String title, ProxySelector selector,
      boolean useSystemProxy) {
    StringBuilder sb = new StringBuilder();
    sb.append(title).append(" useSystemProxy: ").append(useSystemProxy).append("; results: \n");

    final HttpClientBuilder clientBuilder = HttpClients.custom();
    if (useSystemProxy) {
      clientBuilder.useSystemProperties();
    }

    if (selector != null) {
      clientBuilder.setRoutePlanner(new SystemDefaultRoutePlanner(selector));
    }

    try (org.apache.http.impl.client.CloseableHttpClient client = clientBuilder.build()) {

      for (String url : urls) {
        try {
          HttpGet request = new HttpGet(url);
          var response = client.execute(request);

          if (response.getStatusLine().getStatusCode() >= 200
              && response.getStatusLine().getStatusCode() < 300) {
            sb.append("success (%s); ".formatted(url));
          } else {
            sb.append("failed %d (%s); ".formatted(response.getStatusLine().getStatusCode(), url));
          }
        } catch (Exception e) {
          sb.append("error connecting (%s; message: %s); ".formatted(url, e.getMessage()));
        }
      }
    } catch (Exception e) {
      sb.append("error creating client: ").append(e.getMessage()).append("; ");
    }
    logger.info(sb.toString());
  }

}
