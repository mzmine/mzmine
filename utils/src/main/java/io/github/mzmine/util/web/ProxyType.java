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

import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum ProxyType {
  HTTP, HTTPS, SOCKS;

  @Nullable
  public static ProxyType parse(final String type) {
    return switch (type.toLowerCase()) {
      case "http" -> HTTP;
      case "https" -> HTTPS;
      case "socks" -> SOCKS;
      case null, default -> null;
    };
  }

  @Override
  public String toString() {
    return switch (this) {
      case HTTP -> "http";
      case HTTPS -> "https";
      case SOCKS -> "socks";
    };
  }

  @NotNull
  public ProxySystemVar getHostKey() {
    return switch (this) {
      case HTTP -> ProxySystemVar.HTTP_HOST;
      case HTTPS -> ProxySystemVar.HTTPS_HOST;
      case SOCKS -> ProxySystemVar.SOCKS_HOST;
    };
  }

  @NotNull
  public ProxySystemVar getPortKey() {
    return switch (this) {
      case HTTP -> ProxySystemVar.HTTP_PORT;
      case HTTPS -> ProxySystemVar.HTTPS_PORT;
      case SOCKS -> ProxySystemVar.SOCKS_PORT;
    };
  }

  @NotNull
  public ProxySystemVar getSelectedKey() {
    return switch (this) {
      case HTTP -> ProxySystemVar.HTTP_SELECTED;
      case HTTPS -> ProxySystemVar.HTTPS_SELECTED;
      case SOCKS -> ProxySystemVar.SOCKS_SELECTED;
    };
  }

  public boolean isSelectedManually() {
    return "true".equals(getSelectedKey().getSystemValue());
  }

  @Nullable
  public String getHost() {
    return getHostKey().getSystemValue();
  }

  @Nullable
  public String getPort() {
    return getPortKey().getSystemValue();
  }

  @NotNull
  public Optional<ProxyDefinition> createSystemProxyDefinition() {
    String address = getHost();
    if (address == null) {
      return Optional.empty();
    }

    boolean active = isSelectedManually();
    String port = getPort();
    try {
      return Optional.of(new ProxyDefinition(active, address, port, this));
    } catch (Exception exception) {
    }
    return Optional.empty();
  }
}