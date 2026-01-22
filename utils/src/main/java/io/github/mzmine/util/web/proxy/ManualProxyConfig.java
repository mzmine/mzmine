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

package io.github.mzmine.util.web.proxy;

import static java.util.Objects.requireNonNullElse;

import io.github.mzmine.util.web.ProxyDefinition;
import io.github.mzmine.util.web.ProxyType;
import java.util.Arrays;
import java.util.InputMismatchException;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 *
 * @param type          HTTP, HTTPS, or SOCKS
 * @param host          name
 * @param port          port
 * @param nonProxyHosts hosts that should not be proxied
 */
public record ManualProxyConfig(@NotNull ProxyType type, @NotNull String host, int port,
                                @NotNull List<String> nonProxyHosts) {

  public ManualProxyConfig(ProxyDefinition p) {
    int port = 80;
    try {
      if (p.port() != null && !p.port().isBlank()) {
        port = Integer.parseInt(p.port());
      }
    } catch (Exception exception) {
    }
    this(p.type(), requireNonNullElse(p.address(), ""), port, List.of());
  }

  public ManualProxyConfig(String fullProxy) {
    var portIndex = fullProxy.lastIndexOf(":");
    if (portIndex == -1) {
      throw new InputMismatchException(
          "Full proxy format did not contain a port. Define proxy like http://myproxy:port");
    }
    String port = fullProxy.substring(portIndex + 1);
    String address = fullProxy.substring(0, portIndex);

    // proxy definition finds the type
    final ProxyDefinition proxyDefinition = new ProxyDefinition(true, address, port);
    this(proxyDefinition);
  }

  /**
   * Uses default if null
   */
  public ManualProxyConfig(@Nullable ProxyType type, @Nullable String host,
      @Nullable String portString, @Nullable String nonProxyHosts) {
    int port = 80;
    try {
      if (portString != null && !portString.isBlank()) {
        port = Integer.parseInt(portString);
      }
    } catch (Exception exception) {
    }

    List<String> nonProxyHostsList = nonProxyHosts == null ? List.of()
        : Arrays.stream(nonProxyHosts.split("[,|\\s]+")).filter(s -> !s.isBlank()).toList();

    this(requireNonNullElse(type, ProxyType.HTTP), requireNonNullElse(host, ""), port,
        nonProxyHostsList);
  }

  public static ManualProxyConfig defaultConfig() {
    return new ManualProxyConfig(ProxyType.HTTP, "", 80, List.of());
  }

  public @NotNull String portString() {
    return Integer.toString(port);
  }

  @NotNull
  public String getFullProxyString() {
    return "%s://%s:%d".formatted(type.toString(), host, port);
  }

  @Override
  public @NotNull String toString() {
    return getFullProxyString();
  }

  public String fullDefinitionString() {
    if (nonProxyHosts.isEmpty()) {
      return getFullProxyString();
    } else {
      return "%s (non-proxy hosts: %s)".formatted(getFullProxyString(),
          String.join(", ", nonProxyHosts));
    }
  }
}
