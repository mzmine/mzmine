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

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProxyTestUtils {

  private static final Logger logger = Logger.getLogger(ProxyTestUtils.class.getName());

  public static void logProxyState(String msg) {
    List<String> testUrls = List.of("https://auth.mzio.io/", "https://mzio.io",
        "https://zenodo.org/");

    logger.info(msg);
    logSystemProperties();
    logProxyDetails(testUrls);
  }

  /**
   * Logs the standard Java networking system properties related to proxies.
   */
  private static void logSystemProperties() {
    logger.info("Checking Java System Properties for Proxies...");

    String[] properties = {"java.net.useSystemProxies", "http.proxyHost", "http.proxyPort",
        "http.nonProxyHosts", "https.proxyHost", "https.proxyPort", "ftp.proxyHost",
        "ftp.proxyPort", "socksProxyHost", "socksProxyPort"};

    boolean found = false;
    for (String prop : properties) {
      String value = System.getProperty(prop);
      if (value != null && !value.trim().isEmpty()) {
        // 3. Use the logger for informational output
        logger.info("System Property: " + prop + " = " + value);
        found = true;
      }
    }

    if (!found) {
      logger.info("No standard proxy system properties are set.");
    }
  }

  private static void logProxyDetails(List<String> urls) {
    ProxySelector defaultSelector = ProxySelector.getDefault();
    if (defaultSelector == null) {
      logger.info("No default proxy selector is set.");
      return;
    }
    for (String url : urls) {
      try {
        URI testUri = new URI(url);
        List<Proxy> proxies = defaultSelector.select(testUri);

        if (proxies == null || proxies.isEmpty()) {
          logger.info("No proxies returned from the selector.");
          return;
        }

        logger.info("Detected " + proxies.size() + " proxy setting(s) via ProxySelector:");

        for (Proxy proxy : proxies) {
          logProxyDetails(url, proxy);
        }

      } catch (Exception e) {
        // 2. Log exceptions at a SEVERE level, including the stack trace
        logger.log(Level.SEVERE, "An error occurred while detecting proxies", e);
      }
    }
  }

  private static void logProxyDetails(String url, Proxy proxy) {
    logger.info("--> Proxy Type: " + proxy.type());

    switch (proxy.type()) {
      case DIRECT -> logger.info("    Connection will be made directly, without a proxy.");
      case HTTP -> {
        InetSocketAddress addr = (InetSocketAddress) proxy.address();
        logger.info("    For target URL: " + url);
        logger.info("    HTTP Proxy Name: " + addr.getHostName());
        logger.info("    HTTP Proxy Server: " + addr.getHostString());
        logger.info("    Port: " + addr.getPort());
      }
      case SOCKS -> {
        InetSocketAddress addr = (InetSocketAddress) proxy.address();
        logger.info("    For target URL: " + url);
        logger.info("    SOCKS Proxy Name: " + addr.getHostName());
        logger.info("    SOCKS Proxy Server: " + addr.getHostString());
        logger.info("    Port: " + addr.getPort());
      }
      default -> logger.warning("    An unknown proxy type was detected.");
    }
  }
}
