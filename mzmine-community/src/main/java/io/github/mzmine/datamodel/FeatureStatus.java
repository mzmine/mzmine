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
import io.github.mzmine.javafx.util.color.Colors;
import io.github.mzmine.javafx.util.color.ColorsFX;
import io.github.mzmine.javafx.util.color.Vision;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.util.color.SimpleColorPalette;
import java.awt.Color;
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
   * feature was created as a compound aggregate
   */
  COMPOUND_AGGREGATE,

  /**
   * Peak was defined manually
   */
  MANUAL;

  public Color getColor() {
    SimpleColorPalette palette =
        (ConfigService.getDefaultColorPalette() != null) ? ConfigService.getDefaultColorPalette()
            : SimpleColorPalette.DEFAULT.get(Vision.DEUTERANOPIA);

    return switch (this) {
      case DETECTED -> palette.getPositiveColorAWT();
      case ESTIMATED -> palette.getNeutralColorAWT();
      case MANUAL -> Color.BLACK;
      case UNKNOWN -> palette.getNegativeColorAWT();
      case COMPOUND_AGGREGATE -> Colors.MAGENTA;
    };
  }

  public javafx.scene.paint.Color getColorFX() {
    SimpleColorPalette palette =
        (ConfigService.getDefaultColorPalette() != null) ? ConfigService.getDefaultColorPalette()
            : SimpleColorPalette.DEFAULT.get(Vision.DEUTERANOPIA);

    return switch (this) {
      case DETECTED -> palette.getPositiveColor();
      case ESTIMATED -> palette.getNeutralColor();
      case MANUAL -> javafx.scene.paint.Color.BLACK;
      case UNKNOWN -> palette.getNegativeColor();
      case COMPOUND_AGGREGATE -> ColorsFX.MAGENTA;
    };
  }

  @Override
  public @NotNull String getUniqueID() {
    return switch (this) {
      case DETECTED -> "DETECTED";
      case ESTIMATED -> "ESTIMATED";
      case MANUAL -> "MANUAL";
      case UNKNOWN -> "UNKNOWN";
      case  COMPOUND_AGGREGATE -> "COMPOUND_AGGREGATE";
    };
  }

  @Override
  public String toString() {
    return switch (this) {
      case DETECTED -> "Detected";
      case ESTIMATED -> "Estimated";
      case MANUAL -> "Manual";
      case UNKNOWN -> "Unknown";
      case COMPOUND_AGGREGATE -> "Compound aggregate";
    };
  }

  @NotNull
  public String getDescription() {
    return switch (this) {
      case DETECTED -> "Feature was found in primary peak picking";
      case ESTIMATED -> "Feature was estimated in secondary peak picking (gap-filling)";
      case MANUAL -> "Feature was defined manually";
      case UNKNOWN -> "Feature was not found";
      case COMPOUND_AGGREGATE -> "Feature was created as a compound aggregate";
    };
  }

}
