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

import java.util.ArrayList;
import java.util.List;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.NotNull;

public class PresetUtils {

  /**
   * Only keep new presets from toFilter that are not present in currentPresets. Uses equals to
   * compare.
   *
   * @param currentPresets
   * @param toFilter
   * @return new list of presets
   */
  @NotNull
  public static <T extends Preset> List<? extends T> filterDifferent(
      @NotNull ObservableList<? extends T> currentPresets, @NotNull List<? extends T> toFilter) {
    final List<T> keep = new ArrayList<>();
    for (T preset : toFilter) {
      boolean exists = false;
      for (T current : currentPresets) {
        if (preset.equals(current)) {
          exists = true;
          break;
        }
      }
      if (!exists) {
        keep.add(preset);
      }
    }
    return keep;
  }
}
