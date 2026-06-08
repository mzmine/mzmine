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

import io.github.mzmine.util.web.ProxySystemVar;
import java.net.ProxySelector;
import java.util.Arrays;

public enum ProxyConfigOption {

  /**
   * Sets the default system vars in {@link ProxySystemVar} and will overwrite them with the proxy
   * for auth.mzio.io website found through {@link ProxySelector#getDefault()}
   */
  AUTO_PROXY("Auto-detect proxy"),
  /**
   * Sets the default system vars in {@link ProxySystemVar} and IF THEY ARE EMPTY, they will be
   * overwritten with the proxy for auth.mzio.io website found through
   * {@link ProxySelector#getDefault()}
   */
  SYSTEM("System auto proxy"), NO_PROXY("No proxy"), MANUAL_PROXY("Manual proxy"),
  /**
   * This is only here to describe the state at start up. Users should not be able to select it
   * though. AUTO_PROXY is preferred as it synchronizes the settings between
   * {@link ProxySelector#getDefault()} and the System.getProperty values from
   * {@link ProxySystemVar}
   */
  STARTUP("Startup proxy");

  private final String text;

  ProxyConfigOption(String text) {
    this.text = text;
  }

  /**
   * {@link #STARTUP} is not selectable in parameters. Only present at startup
   */
  public static ProxyConfigOption[] defaultValues() {
    return Arrays.stream(values()).filter(o -> o != STARTUP).toArray(ProxyConfigOption[]::new);
  }

  @Override
  public String toString() {
    return text;
  }
}
