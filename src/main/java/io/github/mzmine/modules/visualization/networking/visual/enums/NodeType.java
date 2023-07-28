/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.visualization.networking.visual.enums;

import java.util.Optional;

public enum NodeType implements ElementType {
  NEUTRAL_M, ION_FEATURE, SINGLE_FEATURE, NEUTRAL_LOSS_CENTER;

  /**
   * Some nodes define a special UI class in graph_network_style.css
   *
   * @return the style class or empty
   */
  @Override
  public Optional<String> getUiClass() {
    return switch (this) {
      case ION_FEATURE, SINGLE_FEATURE -> Optional.of("FEATURE");
      case NEUTRAL_M -> Optional.of("MOL");
      case NEUTRAL_LOSS_CENTER -> Optional.empty();
    };
  }
}
