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

package io.github.mzmine.modules.visualization.projectmetadata.extract;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A single case-insensitive value mapping, e.g. extracted value {@code media} -> stored value
 * {@code blank}. Matching is done case-insensitively against {@link #from()}.
 *
 * @param from the extracted value to match (case-insensitive, trimmed)
 * @param to   the value that should be stored instead
 */
public record MetadataValueMapping(@NotNull String from, @NotNull String to) {

  public MetadataValueMapping {
    from = from == null ? "" : from;
    to = to == null ? "" : to;
  }

  /**
   * @return true if this mapping defines a non-blank {@link #from()} key and is therefore active.
   */
  public boolean isActive() {
    return !from.isBlank();
  }

  /**
   * @param value the extracted value
   * @return true if the given value matches this mapping's {@link #from()} key (case-insensitive,
   * trimmed)
   */
  public boolean matches(@Nullable final String value) {
    return value != null && isActive() && from.trim().equalsIgnoreCase(value.trim());
  }
}
