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

package io.github.mzmine.util.presets;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.github.mzmine.util.files.FileAndPathUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface Preset extends Comparable<Preset> {

  @NotNull String name();

  /**
   * Path safe name
   *
   * @return name but path save encoding
   */
  @JsonIgnore
  default @NotNull String getFileName() {
    return FileAndPathUtil.safePathEncode(name());
  }

  @Override
  default int compareTo(@Nullable Preset o) {
    if (o == null) {
      return 1;
    } else if (this == o) {
      return 0;
    }
    return name().compareTo(o.name());
  }

  /**
   * @return true if filename or name equal case insensitive
   */
  default boolean equalsIgnoreCaseName(Preset preset) {
    return getFileName().equalsIgnoreCase(preset.getFileName()) || name().equalsIgnoreCase(
        preset.name());
  }

  @NotNull <T extends Preset> T withName(String name);
}
