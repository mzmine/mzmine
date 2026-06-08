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
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.metadata.MetadataGroupingParameter;
import java.util.Arrays;
import java.util.stream.Collectors;

public class ColorByMetadataColumnParameters extends SimpleParameterSet {

  public static final MetadataGroupingParameter colorByColumn = new MetadataGroupingParameter();

  public static final ComboParameter<ColorByNumericOption> colorNumericValues = new ComboParameter<>(
      "Handle numeric values", """
      Dates and numbers can be sorted and handled by gradient style paint scales or by distinct colors.
      """ + Arrays.stream(ColorByNumericOption.values()).map(ColorByNumericOption::getDescription)
      .collect(Collectors.joining("\n")), ColorByNumericOption.values(), ColorByNumericOption.AUTO);

  public static final ComboParameter<PaintScaleTransform> gradientTransform = new ComboParameter<>(
      "Gradient transform", """
      Dates and numbers can be sorted and handled by gradient style paint scales. This is the transform used.""",
      PaintScaleTransform.values(), PaintScaleTransform.LINEAR);

  public ColorByMetadataColumnParameters() {
    super(colorByColumn, colorNumericValues, gradientTransform);
  }

}
