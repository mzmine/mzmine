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

package io.github.mzmine.modules.visualization.projectmetadata.color;

import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScaleTransform;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.main.MZmineConfiguration;
import io.github.mzmine.util.color.SimpleColorPalette;

/**
 * @param handleNumericOption        an option how to handle numeric and date values. Either
 *                                   gradient or distinct colors or automatic gradient for large
 *                                   sample sets.
 * @param transform                  transformation for the transformPalette paint scale to
 *                                   interpolate colors between each step.
 * @param cloneResetCategoryPalette  For {@link ColorByNumericOption#DISCRETE}: this palette holds
 *                                   distinct colors for categories without interpolation. Usually:
 *                                   {@link MZmineConfiguration#getDefaultColorPalette()}. Notice:
 *                                   this will always return a clone with reset index!
 * @param cloneResetTransformPalette For {@link ColorByNumericOption#GRADIENT}: this palette holds
 *                                   colors for interpolation PaintScales and applies the transform.
 *                                   Usually
 *                                   {@link MZmineConfiguration#getDefaultPaintScalePalette()}
 *                                   Notice: this will always return a clone with reset index!
 */
public record ColorByMetadataConfig(ColorByNumericOption handleNumericOption,
                                    PaintScaleTransform transform,
                                    SimpleColorPalette cloneResetCategoryPalette,
                                    SimpleColorPalette cloneResetTransformPalette) {

  public ColorByMetadataConfig(ColorByNumericOption handleNumericOption,
      PaintScaleTransform transform) {
    final MZmineConfiguration config = ConfigService.getConfiguration();
    this(handleNumericOption, transform, config.getDefaultColorPalette().clone(true),
        config.getDefaultPaintScalePalette().clone(true));
  }

  /**
   * @return always a clone with reset index
   */
  public SimpleColorPalette cloneResetCategoryPalette() {
    return cloneResetCategoryPalette.clone(true);
  }

  /**
   * @return always a clone with reset index
   */
  public SimpleColorPalette cloneResetTransformPalette() {
    return cloneResetTransformPalette.clone(true);
  }

  public static ColorByMetadataConfig createDefault() {
    return new ColorByMetadataConfig(ColorByNumericOption.AUTO, PaintScaleTransform.LINEAR);
  }

  public boolean isUseGradient(int size) {
    return handleNumericOption.isUseGradient(size);
  }
}
