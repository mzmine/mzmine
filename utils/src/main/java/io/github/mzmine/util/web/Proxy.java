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

import io.github.mzmine.util.objects.ObjectUtils;
import static java.util.Objects.requireNonNullElse;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @param address prefix of http or https:// are removed internally
 */
public record Proxy(boolean active, @Nullable String address, @Nullable String port,
                    @NotNull ProxyType type) {

  public static final Proxy EMPTY = new Proxy(false, null, null, ProxyType.HTTP);

  public Proxy(final boolean active, @Nullable final String address, @Nullable final String port) {
    this(active, address, port, null);
  }

  public Proxy(final boolean active, @Nullable final String address, @Nullable final String port,
      @Nullable ProxyType type) {
    // deactivate if any is null
    this.active = active && ObjectUtils.noneNull(address, port);
    this.port = port;

    // some proxy urls contain http:// at the beginning, we need to filter this out
    if (address == null) {
      this.address = address;
    } else if (address.toLowerCase().startsWith("http://")) {
      this.address = address.replaceFirst("http://", "");
      type = ProxyType.HTTP;
    } else if (address.toLowerCase().startsWith("https://")) {
      this.address = address.replaceFirst("https://", "");
      type = ProxyType.HTTPS;
    } else {
      this.address = address;
    }
    this.type = requireNonNullElse(type, ProxyType.HTTP);
  }

  /**
   * Format of type address and port together, e.g., http://someaddress:80
   */
  @Nullable
  public String fullTypeAddressPort() {
    if (ObjectUtils.anyIsNull(address, port, type)) {
      return null;
    }
    return "%s://%s:%s".formatted(type, address, port);
  }

}
