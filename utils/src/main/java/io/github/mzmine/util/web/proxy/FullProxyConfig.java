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

import org.jetbrains.annotations.NotNull;

/**
 * Combines configs from manual and auto
 *
 * @param option       auto, manual, none
 * @param manualConfig manual config
 */
public record FullProxyConfig(@NotNull ProxyConfigOption option,
                              @NotNull ManualProxyConfig manualConfig) {


  public static FullProxyConfig defaultConfig() {
    return FullProxyConfig.create(ProxyConfigOption.AUTO_PROXY);
  }

  public static @NotNull FullProxyConfig noProxy() {
    return FullProxyConfig.create(ProxyConfigOption.NO_PROXY);
  }

  public static @NotNull FullProxyConfig create(@NotNull ProxyConfigOption option) {
    return new FullProxyConfig(option, ManualProxyConfig.defaultConfig());
  }
}
