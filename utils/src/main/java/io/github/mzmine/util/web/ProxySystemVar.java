/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

import org.jetbrains.annotations.Nullable;

/**
 * Easier to find system variables and custom system vars
 */
public enum ProxySystemVar {
  // custom fields introduced by mzmine
  TYPE("proxyType"), // needed for login service
  HTTP_SELECTED("http.proxySet"), //
  HTTPS_SELECTED("https.proxySet"), //
  SOCKS_SELECTED("socksProxySet"), //
  /**
   * Seems to be used be all others as well
   */
  HTTP_NON_PROXY_HOSTS("http.nonProxyHosts"), //
  HTTP_HOST("http.proxyHost"), //
  HTTP_PORT("http.proxyPort"), //
  HTTPS_HOST("https.proxyHost"), //
  HTTPS_PORT("https.proxyPort"), //
  SOCKS_HOST("socksProxyHost"), //
  SOCKS_PORT("socksProxyPort");

  private final String key;

  ProxySystemVar(String key) {
    this.key = key;
  }

  public String getKey() {
    return key;
  }

  @Override
  public String toString() {
    return getKey();
  }

  @Nullable
  public String getSystemValue() {
    return System.getProperty(this.getKey());
  }

  /**
   * @return true if value was set false if it was cleared (value was null)
   */
  public boolean setSystemValue(@Nullable String value) {
    if (value != null) {
      System.setProperty(getKey(), value);
      return true;
    } else {
      System.clearProperty(getKey());
      return false;
    }
  }
}
