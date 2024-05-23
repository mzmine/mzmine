/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

import io.mzio.events.EventService;
import java.util.InputMismatchException;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public class ProxyUtils {

  private static boolean allowChanges = true;

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

    Proxy old = getSelectedSystemProxy();
    System.clearProperty("proxyType"); // needed for login service
    System.clearProperty("http.proxySet");
    System.clearProperty("http.proxyHost");
    System.clearProperty("http.proxyPort");

    System.clearProperty("https.proxySet");
    System.clearProperty("https.proxyHost");
    System.clearProperty("https.proxyPort");

    if (!Objects.equals(old, Proxy.EMPTY)) {
      EventService.post(new ProxyChangedEvent(Proxy.EMPTY));
    }
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
  public static synchronized Proxy setSystemProxy(String address, String port,
      ProxyType proxyType) {
    return setSystemProxy(new Proxy(true, address, port, proxyType));
  }

  /**
   * Setting system properties to use proxy. Fires {@link ProxyChangedEvent} in {@link EventService}
   * if proxy changed
   *
   * @return proxy object with the final address
   */
  @NotNull
  public static synchronized Proxy setSystemProxy(Proxy proxy) {
    if (!allowChanges) {
      return getSystemProxy(proxy.type());
    }

    if (!proxy.active()) {
      clearSystemProxy();
      return Proxy.EMPTY;
    }
    // if any is null active == false
    assert proxy.address() != null;
    assert proxy.port() != null;
    assert proxy.type() != null;

    Proxy old = getSelectedSystemProxy();

    System.setProperty("http.proxySet", "true");
    System.setProperty("http.proxyHost", proxy.address());
    System.setProperty("http.proxyPort", proxy.port());
    System.setProperty("https.proxySet", "true");
    System.setProperty("https.proxyHost", proxy.address());
    System.setProperty("https.proxyPort", proxy.port());
    System.setProperty("proxyType", proxy.type().toString()); // needed for login service

    if (!Objects.equals(old, proxy)) {
      EventService.post(new ProxyChangedEvent(proxy));
    }
    return proxy;
  }

  /**
   * The proxy of a specific type
   */
  @NotNull
  public static synchronized Proxy getSelectedSystemProxy() {
    String type = System.getProperty("proxyType");
    if (type == null) {
      return Proxy.EMPTY;
    }
    try {
      return getSystemProxy(ProxyType.parse(type));
    } catch (Exception ex) {
      return Proxy.EMPTY;
    }
  }

  /**
   * The proxy of a specific type
   */
  @NotNull
  public static synchronized Proxy getSystemProxy(ProxyType type) {
    boolean active = "true".equals(System.getProperty(type + ".proxySet"));
    String address = System.getProperty(type + ".proxyHost");
    String port = System.getProperty(type + ".proxyPort");
    try {
      return new Proxy(active, address, port, type);
    } catch (Exception exception) {
      return Proxy.EMPTY;
    }
  }

  /**
   * @param fullProxy Define proxy like http://myproxy:port
   * @return the now set proxy
   */
  @NotNull
  public static synchronized Proxy setSystemProxy(final String fullProxy)
      throws InputMismatchException {
    var portIndex = fullProxy.lastIndexOf(":");
    if (portIndex == -1) {
      throw new InputMismatchException(
          "Full proxy format did not contain a port. Define proxy like http://myproxy:port");
    }
    String port = fullProxy.substring(portIndex + 1);
    String address = fullProxy.substring(0, portIndex);

    var proxy = new Proxy(true, address, port);
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
  public static synchronized Proxy setSystemProxyAndBlockLaterChanges(final String fullProxy)
      throws InputMismatchException {
    setAllowChanges(true);
    var proxy = setSystemProxy(fullProxy);
    setAllowChanges(false);
    return proxy;
  }
}
