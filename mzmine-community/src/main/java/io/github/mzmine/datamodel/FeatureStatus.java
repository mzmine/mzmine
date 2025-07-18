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

package io.github.mzmine.datamodel;

import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.util.color.SimpleColorPalette;
import java.awt.Color;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.javafx.util.color.Vision;
import org.jetbrains.annotations.NotNull;

public enum FeatureStatus implements UniqueIdSupplier {

  /**
   * Peak was not found
   */
  UNKNOWN,

  /**
   * Peak was found in primary peak picking
   */
  DETECTED,

  /**
   * Peak was estimated in secondary peak picking
   */
  ESTIMATED,

  /**
   * Peak was defined manually
   */
  MANUAL;

  public Color getColor() {
    SimpleColorPalette palette =
        (ConfigService.getDefaultColorPalette() != null) ? ConfigService.getDefaultColorPalette()
            : SimpleColorPalette.DEFAULT.get(Vision.DEUTERANOPIA);

    switch (this) {
      case DETECTED:
        return palette.getPositiveColorAWT();
      case ESTIMATED:
        return palette.getNeutralColorAWT();
      case MANUAL:
        return Color.BLACK;
      case UNKNOWN:
      default:
        return palette.getNegativeColorAWT();
    }
  }

  public javafx.scene.paint.Color getColorFX() {
    SimpleColorPalette palette =
        (ConfigService.getDefaultColorPalette() != null) ? ConfigService.getDefaultColorPalette()
            : SimpleColorPalette.DEFAULT.get(Vision.DEUTERANOPIA);

    switch (this) {
      case DETECTED:
        return palette.getPositiveColor();
      case ESTIMATED:
        return palette.getNeutralColor();
      case MANUAL:
        return javafx.scene.paint.Color.BLACK;
      case UNKNOWN:
      default:
        return palette.getNegativeColor();
    }
  }

  @Override
  public @NotNull String getUniqueID() {
    return switch (this) {
      case DETECTED -> "DETECTED";
      case ESTIMATED -> "ESTIMATED";
      case MANUAL -> "MANUAL";
      case UNKNOWN -> "UNKNOWN";
    };
  }
}
