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

import static io.github.mzmine.util.web.ProxySystemVar.HTTPS_HOST;
import static io.github.mzmine.util.web.ProxySystemVar.HTTPS_PORT;
import static io.github.mzmine.util.web.ProxySystemVar.HTTPS_SELECTED;
import static io.github.mzmine.util.web.ProxySystemVar.HTTP_HOST;
import static io.github.mzmine.util.web.ProxySystemVar.HTTP_NON_PROXY_HOSTS;
import static io.github.mzmine.util.web.ProxySystemVar.HTTP_PORT;
import static io.github.mzmine.util.web.ProxySystemVar.HTTP_SELECTED;
import static io.github.mzmine.util.web.ProxySystemVar.SOCKS_HOST;
import static io.github.mzmine.util.web.ProxySystemVar.SOCKS_PORT;
import static io.github.mzmine.util.web.ProxySystemVar.SOCKS_SELECTED;
import static io.github.mzmine.util.web.ProxySystemVar.TYPE;

import io.github.mzmine.util.web.proxy.FullProxyConfig;
import io.github.mzmine.util.web.proxy.ManualProxyConfig;
import io.mzio.events.EventService;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ProxyUtils {

  private static final Logger logger = Logger.getLogger(ProxyUtils.class.getName());
  private static boolean allowChanges = true;

  // keep track of all initial proxy values from the startup
  private static final String DEFAULT_HTTP_NON_PROXY_HOSTS = System.getProperty(
      "http.nonProxyHosts");
  private static final String DEFAULT_HTTP_HOST = System.getProperty("http.proxyHost");
  private static final String DEFAULT_HTTP_PORT = System.getProperty("http.proxyPort");
  private static final String DEFAULT_HTTPS_HOST = System.getProperty("https.proxyHost");
  private static final String DEFAULT_HTTPS_PORT = System.getProperty("https.proxyPort");
  private static final String DEFAULT_SOCKS_HOST = System.getProperty("socksProxyHost");
  private static final String DEFAULT_SOCKS_PORT = System.getProperty("socksProxyPort");


  public static synchronized void setAllowChanges(final boolean allowChanges) {
    ProxyUtils.allowChanges = allowChanges;
  }

  public static boolean isAllowChanges() {
    return allowChanges;
  }

  /**
   * Remove system properties
   */
  public static synchronized void clearSystemProxy() {
    if (!allowChanges) {
      return;
    }

    ProxyDefinition old = getSelectedSystemProxy();
    System.clearProperty("proxyType"); // needed for login service
    System.clearProperty("http.proxySet");
    System.clearProperty("http.proxyHost");
    System.clearProperty("http.proxyPort");
    System.clearProperty("http.nonProxyHosts");

    System.clearProperty("https.proxySet");
    System.clearProperty("https.proxyHost");
    System.clearProperty("https.proxyPort");
    System.clearProperty("socksProxyHost");
    System.clearProperty("socksProxyPort");

    logger.info("Proxy system properties cleared");

    if (!Objects.equals(old, ProxyDefinition.EMPTY)) {
      EventService.post(new ProxyChangedEvent(ProxyDefinition.EMPTY));
    }
  }


  /**
   * Synchronizes system properties with the default ProxySelector for a given URI. This allows
   * JavaFX WebView to use the same proxy as other Java HTTP clients.
   *
   * @param uri The URI to determine proxy settings for
   * @return true if proxy was configured, false otherwise
   */
  public static boolean syncProxySelectorToSystemProperties(String uri) {
    ProxySelector proxySelector = ProxySelector.getDefault();
    return syncProxySelectorToSystemProperties(uri, proxySelector);
  }

  /**
   * Synchronizes system properties with the default ProxySelector for a given URI. This allows
   * JavaFX WebView to use the same proxy as other Java HTTP clients.
   *
   * @param uri           The URI to determine proxy settings for
   * @param proxySelector the selector to use
   * @return true if proxy was configured, false otherwise
   */
  public static boolean syncProxySelectorToSystemProperties(String uri,
      @Nullable ProxySelector proxySelector) {
    if (proxySelector == null) {
      clearSystemProxy();
      return false;
    }

    try {
      List<Proxy> proxies = proxySelector.select(URI.create(uri));

      if (proxies == null || proxies.isEmpty()) {
        clearSystemProxy();
        return false;
      }

      // Use the first proxy in the list
      Proxy proxy = proxies.getFirst();

      if (proxy.type() == Proxy.Type.DIRECT) {
        // No proxy needed
        clearSystemProxy();
        return false;
      }

      if (proxy.address() instanceof InetSocketAddress socketAddress) {
        String host = socketAddress.getHostString();
        int port = socketAddress.getPort();

        switch (proxy.type()) {
          case HTTP -> {
            System.setProperty("http.proxyHost", host);
            System.setProperty("http.proxyPort", String.valueOf(port));
            System.setProperty("https.proxyHost", host);
            System.setProperty("https.proxyPort", String.valueOf(port));
            logger.info("HTTP/HTTPS proxy configured: " + host + ":" + port);
          }
          case SOCKS -> {
            System.setProperty("socksProxyHost", host);
            System.setProperty("socksProxyPort", String.valueOf(port));
            logger.info("SOCKS proxy configured: " + host + ":" + port);
          }
          default -> {
            logger.warning("Unsupported proxy type: " + proxy.type());
            return false;
          }
        }
        return true;
      }
    } catch (Exception e) {
      logger.log(Level.WARNING, "Failed to sync ProxySelector to system properties", e);
    }

    return false;
  }

  /**
   * Configures non-proxy hosts from system property
   */
  public static void configureNonProxyHosts(String... hosts) {
    if (hosts == null || hosts.length == 0) {
      System.clearProperty("http.nonProxyHosts");
      return;
    }

    String nonProxyHosts = String.join("|", hosts);
    System.setProperty("http.nonProxyHosts", nonProxyHosts);
    logger.info("Non-proxy hosts configured: " + nonProxyHosts);
  }

  /**
   * Setting system properties to use proxy. Fires {@link ProxyChangedEvent} in {@link EventService}
   * if proxy changed
   *
   * @param address   proxy address prefix of http:// or https:// will be removed
   * @param port      proxy port
   * @param proxyType proxy type
   * @return proxy object with the final address
   */
  @NotNull
  public static synchronized ProxyDefinition setSystemProxy(String address, String port,
      ProxyType proxyType) {
    return setSystemProxy(new ProxyDefinition(true, address, port, proxyType));
  }

  /**
   * Setting system properties to use proxy. Fires {@link ProxyChangedEvent} in {@link EventService}
   * if proxy changed
   *
   * @return proxy object with the final address
   */
  @NotNull
  public static synchronized ProxyDefinition setSystemProxy(ProxyDefinition proxy) {
    if (!allowChanges) {
      return getSystemProxy(proxy.type());
    }

    if (!proxy.active()) {
      // do not clear just because one proxy is set to off
//      clearSystemProxy();
      return ProxyDefinition.EMPTY;
    }
    // if any is null active == false
    assert proxy.address() != null;
    assert proxy.port() != null;
    assert proxy.type() != null;

    ProxyDefinition old = getSelectedSystemProxy();

    if (proxy.type() == ProxyType.SOCKS) {
      setOrClear(SOCKS_SELECTED, "true");
      setOrClear(SOCKS_HOST, proxy.address());
      setOrClear(SOCKS_PORT, proxy.port());
    } else {
      setOrClear(HTTP_SELECTED, "true");
      setOrClear(HTTP_HOST, proxy.address());
      setOrClear(HTTP_PORT, proxy.port());
      setOrClear(HTTPS_SELECTED, "true");
      setOrClear(HTTPS_HOST, proxy.address());
      setOrClear(HTTPS_PORT, proxy.port());
    }

    setOrClear(TYPE, proxy.type().name());

    if (!Objects.equals(old, proxy)) {
      EventService.post(new ProxyChangedEvent(proxy));
    }
    return proxy;
  }

  /**
   * The proxy of a specific type
   */
  @NotNull
  public static synchronized ProxyDefinition getSelectedSystemProxy() {
    String type = System.getProperty("proxyType");
    if (type == null) {
      // then just find the first set
      String host = get(HTTP_HOST);
      find http or https or socks and return first

      return ProxyDefinition.EMPTY;
    }
    try {
      return getSystemProxy(ProxyType.parse(type));
    } catch (Exception ex) {
      return ProxyDefinition.EMPTY;
    }
  }

  /**
   * The proxy of a specific type
   */
  @NotNull
  public static synchronized ProxyDefinition getSystemProxy(ProxyType type) {
    boolean active = "true".equals(System.getProperty(type + ".proxySet"));
    String address = System.getProperty(type + ".proxyHost");
    String port = System.getProperty(type + ".proxyPort");
    try {
      return new ProxyDefinition(active, address, port, type);
    } catch (Exception exception) {
      return ProxyDefinition.EMPTY;
    }
  }

  /**
   * @param fullProxy Define proxy like http://myproxy:port
   * @return the now set proxy
   */
  @NotNull
  public static synchronized ProxyDefinition setSystemProxy(final String fullProxy)
      throws InputMismatchException {
    var portIndex = fullProxy.lastIndexOf(":");
    if (portIndex == -1) {
      throw new InputMismatchException(
          "Full proxy format did not contain a port. Define proxy like http://myproxy:port");
    }
    String port = fullProxy.substring(portIndex + 1);
    String address = fullProxy.substring(0, portIndex);

    var proxy = new ProxyDefinition(true, address, port);
    return setSystemProxy(proxy);
  }

  /**
   * Changes the system proxy and sets the allow changes to false to block later changes. This is
   * useful when setting via command line
   *
   * @param fullProxy Define proxy like http://myproxy:port
   * @return the now set proxy
   */
  @NotNull
  public static synchronized ProxyDefinition setSystemProxyAndBlockLaterChanges(
      final String fullProxy) throws InputMismatchException {
    if (!allowChanges) {
      throw new IllegalStateException(
          "Cannot set proxy twice with this method as it was already disabled");
    }
    var proxy = setSystemProxy(fullProxy);
    setAllowChanges(false);
    return proxy;
  }

  public static void applyConfig(@NotNull FullProxyConfig value) {
    switch (value.option()) {
      case AUTO_PROXY -> {
        applySystemDefaults();
      }
      case NO_PROXY -> {
        clearSystemProxy();
      }
      case MANUAL_PROXY -> {
        applyManualProxyConfig(value.manualConfig());
      }
    }
  }

  private static void applyManualProxyConfig(@NotNull ManualProxyConfig config) {
    if (!allowChanges) {
      return;
    }

    clearSystemProxy();

    switch (config.type()) {

    }
  }

  /**
   * Applies system defaults from startup of JVM or if those are empty tries to find the proxy with
   * {@link ProxySelector#getDefault()} by checking https://auth.mzio.io/.
   */
  public static void applySystemDefaults() {
    if (!allowChanges) {
      return;
    }

    ProxyDefinition old = getSelectedSystemProxy();

    boolean anyHttpHostDefined = //
        setOrClear(HTTP_HOST, DEFAULT_HTTP_HOST) || //
            setOrClear(HTTPS_HOST, DEFAULT_HTTPS_HOST);

    setOrClear(HTTP_NON_PROXY_HOSTS, DEFAULT_HTTP_NON_PROXY_HOSTS);
    setOrClear(HTTP_PORT, DEFAULT_HTTP_PORT);

    setOrClear(HTTPS_PORT, DEFAULT_HTTPS_PORT);

    setOrClear(SOCKS_HOST, DEFAULT_SOCKS_HOST);
    setOrClear(SOCKS_PORT, DEFAULT_SOCKS_PORT);

    logger.info("Proxy settings restored to system defaults");

    if (!anyHttpHostDefined) {
      final ProxySelector selector = ProxySelector.getDefault();
      if (selector != null) {
        syncProxySelectorToSystemProperties("https://auth.mzio.io/", selector);
      }
    }

    ProxyDefinition current = getSelectedSystemProxy();
    if (!Objects.equals(old, current)) {
      EventService.post(new ProxyChangedEvent(current));
    }
  }

  @Nullable
  private static String get(ProxySystemVar key) {
    return System.getProperty(key.getKey());
  }

  /**
   *
   * @return true if value was set false if it was cleared (value was null)
   */
  private static boolean setOrClear(ProxySystemVar key, String value) {
    if (value != null) {
      System.setProperty(key.getKey(), value);
      return true;
    } else {
      System.clearProperty(key.getKey());
      return false;
    }
  }
}
