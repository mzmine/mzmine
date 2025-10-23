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
import io.github.mzmine.util.web.proxy.ProxyConfigOption;
import io.mzio.events.EventService;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ProxyUtils {

  private static final Logger logger = Logger.getLogger(ProxyUtils.class.getName());
  private static volatile boolean allowChanges = true;
  private static volatile FullProxyConfig currentConfig;

  // keep track of all initial proxy values from the startup
  private static final String DEFAULT_HTTP_NON_PROXY_HOSTS = HTTP_NON_PROXY_HOSTS.getSystemValue();
  private static final String DEFAULT_HTTP_HOST = HTTP_HOST.getSystemValue();
  private static final String DEFAULT_HTTP_PORT = HTTP_PORT.getSystemValue();
  private static final String DEFAULT_HTTPS_HOST = HTTPS_HOST.getSystemValue();
  private static final String DEFAULT_HTTPS_PORT = HTTPS_PORT.getSystemValue();
  private static final String DEFAULT_SOCKS_HOST = SOCKS_HOST.getSystemValue();
  private static final String DEFAULT_SOCKS_PORT = SOCKS_PORT.getSystemValue();


  public static synchronized void setAllowChanges(final boolean allowChanges) {
    ProxyUtils.allowChanges = allowChanges;
  }

  public static boolean isAllowChanges() {
    return allowChanges;
  }


  /**
   * Apply config and create {@link EventService} {@link ProxyChangedEvent}
   *
   * @param value new config to apply
   */
  public static synchronized void applyConfig(@NotNull FullProxyConfig value) {
    if (!allowChanges) {
      return;
    }

    if (Objects.equals(currentConfig, value)) {
      return;
    }

    if (value.option() == ProxyConfigOption.MANUAL_PROXY) {
      final ManualProxyConfig m = value.manualConfig();
      logger.info("Applying proxy config: %s; Manual is: %s".formatted(value.option().toString(),
          m.toString()));
    } else {
      logger.info("Applying proxy config: %s".formatted(value.option().toString()));
    }

    switch (value.option()) {
      case SYSTEM -> {
        // use system defaults and only use selector for those that are missing
        applySystemDefaults(false);
      }
      case AUTO_PROXY -> {
        // use system defaults and force selector even if default is not missing
        applySystemDefaults(true);
      }
      case NO_PROXY -> {
        clearSystemProxy();
      }
      case MANUAL_PROXY -> {
        applyManualProxyConfig(value.manualConfig());
      }
    }

    // send event that proxy has changed
    currentConfig = value;
    EventService.post(new ProxyChangedEvent(value, getSelectedSystemProxy()));
  }


  private static synchronized void clearSystemProxy() {
    if (!allowChanges) {
      return;
    }

    for (ProxySystemVar sysVar : ProxySystemVar.values()) {
      sysVar.setSystemValue(null);
    }

    logger.info("Proxy system properties cleared");
  }

  /**
   * Synchronizes system properties with the default ProxySelector for a given URI. This allows
   * JavaFX WebView to use the same proxy as other Java HTTP clients.
   *
   * @param uri The URI to determine proxy settings for
   * @return true if proxy was configured, false otherwise
   */
  private static boolean syncProxySelectorToSystemProperties(String uri) {
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
  private static boolean syncProxySelectorToSystemProperties(String uri,
      @Nullable ProxySelector proxySelector) {
    if (proxySelector == null) {
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
        logger.info("Proxy selector found a direct proxy. clearing Proxies now.");
        clearSystemProxy();
        return false;
      }

      if (proxy.address() instanceof InetSocketAddress socketAddress) {
        String host = socketAddress.getHostString();
        int port = socketAddress.getPort();

        switch (proxy.type()) {
          case HTTP -> {
            HTTP_HOST.setSystemValue(host);
            HTTP_PORT.setSystemValue(Integer.toString(port));
            HTTPS_HOST.setSystemValue(host);
            HTTPS_PORT.setSystemValue(Integer.toString(port));
            logger.info("HTTP/HTTPS proxy configured: " + host + ":" + port);
          }
          case SOCKS -> {
            SOCKS_HOST.setSystemValue(host);
            SOCKS_PORT.setSystemValue(Integer.toString(port));
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
  public static void configureNonProxyHosts(List<String> hosts) {
    if (hosts == null || hosts.isEmpty()) {
      HTTP_NON_PROXY_HOSTS.setSystemValue(null);
      return;
    }

    String nonProxyHosts = String.join("|", hosts);
    HTTP_NON_PROXY_HOSTS.setSystemValue(nonProxyHosts);
    logger.info("Non-proxy hosts configured: " + nonProxyHosts);
  }

  /**
   * Setting system properties to use proxy. Fires {@link ProxyChangedEvent} in {@link EventService}
   * if proxy changed
   *
   * @return proxy object with the final address
   */
  @NotNull
  public static synchronized ProxyDefinition setManualProxy(ProxyDefinition proxy) {
    applyConfig(new FullProxyConfig(ProxyConfigOption.MANUAL_PROXY, new ManualProxyConfig(proxy)));
    return getSelectedSystemProxy();
  }

  /**
   * This is only for manually selected proxy and sets the type and selected
   *
   */
  private static void setManuallySelected(@NotNull ProxyType type) {
    HTTP_SELECTED.setSystemValue(null);
    HTTPS_SELECTED.setSystemValue(null);
    SOCKS_SELECTED.setSystemValue(null);
    type.getSelectedKey().setSystemValue("true");
    TYPE.setSystemValue(type.name()); // setting the name of type to type system var
  }

  /**
   * The proxy of a specific type
   */
  @NotNull
  public static synchronized ProxyDefinition getSelectedSystemProxy() {
    // type is only set for manual proxy selection
    if (currentConfig != null && currentConfig.option() == ProxyConfigOption.MANUAL_PROXY) {
      final ProxyType type = currentConfig.manualConfig().type();
      final ProxyDefinition def = type.createSystemProxyDefinition().orElse(null);
      if (def != null) {
        return def;
      }
    }

    // then just find the first that is set
    for (ProxyType pt : ProxyType.values()) {
      final Optional<ProxyDefinition> def = pt.createSystemProxyDefinition();
      if (def.isPresent()) {
        return def.get();
      }
    }

    return ProxyDefinition.EMPTY;
  }

  /**
   * @param fullProxy Define proxy like http://myproxy:port
   * @return the now set proxy
   */
  @NotNull
  public static synchronized ProxyDefinition setManualProxy(final String fullProxy)
      throws InputMismatchException {
    applyConfig(
        new FullProxyConfig(ProxyConfigOption.MANUAL_PROXY, new ManualProxyConfig(fullProxy)));
    return getSelectedSystemProxy();
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
    var proxy = setManualProxy(fullProxy);
    setAllowChanges(false);
    return proxy;
  }

  private static void applyManualProxyConfig(@NotNull ManualProxyConfig proxy) {
    if (!allowChanges) {
      return;
    }

    // do not trigger change event
    clearSystemProxy();
    configureNonProxyHosts(proxy.nonProxyHosts());

    final boolean hasAddress = !proxy.host().isBlank();
    if (hasAddress) {
      setManuallySelected(proxy.type());
    }

    // if HTTP selected then also set HTTPS and flipped
    switch (proxy.type()) {
      case HTTP, HTTPS -> {
        HTTP_HOST.setSystemValue(proxy.host());
        HTTP_PORT.setSystemValue(proxy.portString());
        HTTPS_HOST.setSystemValue(proxy.host());
        HTTPS_PORT.setSystemValue(proxy.portString());
      }
      case SOCKS -> {
        SOCKS_HOST.setSystemValue(proxy.host());
        SOCKS_PORT.setSystemValue(proxy.portString());
      }
    }
  }

  /**
   * Applies system defaults from startup of JVM or if those are empty tries to find the proxy with
   * {@link ProxySelector#getDefault()} by checking https://auth.mzio.io/.
   */
  private static void applySystemDefaults(boolean alwaysApplyProxySelector) {
    if (!allowChanges) {
      return;
    }

    // clear fields that are only for manual selection
    TYPE.setSystemValue(null);
    HTTP_SELECTED.setSystemValue(null);
    HTTPS_SELECTED.setSystemValue(null);
    SOCKS_SELECTED.setSystemValue(null);

    // set to system defaults and see if HTTP or HTTPS is found
    boolean anyHttpHostDefined = //
        HTTP_HOST.setSystemValue(DEFAULT_HTTP_HOST) || //
            HTTPS_HOST.setSystemValue(DEFAULT_HTTPS_HOST);

    HTTP_NON_PROXY_HOSTS.setSystemValue(DEFAULT_HTTP_NON_PROXY_HOSTS);
    HTTP_PORT.setSystemValue(DEFAULT_HTTP_PORT);

    HTTPS_PORT.setSystemValue(DEFAULT_HTTPS_PORT);

    SOCKS_HOST.setSystemValue(DEFAULT_SOCKS_HOST);
    SOCKS_PORT.setSystemValue(DEFAULT_SOCKS_PORT);

    logger.info("Proxy settings restored to system defaults");

    if (alwaysApplyProxySelector || !anyHttpHostDefined) {
      final ProxySelector selector = ProxySelector.getDefault();
      if (selector != null) {
        syncProxySelectorToSystemProperties("https://auth.mzio.io/", selector);
      }
    }
  }

}
