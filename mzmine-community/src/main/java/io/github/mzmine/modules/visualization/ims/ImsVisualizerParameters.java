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

package io.github.mzmine.modules.visualization.ims;
/*
import java.text.DecimalFormat;
import com.google.common.collect.Range;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScale;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScaleBoundStyle;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScaleColorStyle;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.PaintScaleParameter;
import io.github.mzmine.parameters.parametertypes.WindowSettingsParameter;
import io.github.mzmine.parameters.parametertypes.ranges.DoubleRangeParameter;
import io.github.mzmine.parameters.parametertypes.ranges.MZRangeParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelectionParameter;

public class ImsVisualizerParameters extends SimpleParameterSet {
  public static final RawDataFilesParameter dataFiles = new RawDataFilesParameter(1, 1);
  public static final ScanSelectionParameter scanSelection =
      new ScanSelectionParameter(new ScanSelection(1));
  public static final MZRangeParameter mzRange = new MZRangeParameter();
  public static final DoubleRangeParameter zScaleRange =
      new DoubleRangeParameter("Range for z-Axis scale", "", new DecimalFormat("##0.00"));

  public static final PaintScaleParameter paintScale =
      new PaintScaleParameter("Paint scale", "Select paint scale",
          new PaintScale[] {
              new PaintScale(PaintScaleColorStyle.RAINBOW, PaintScaleBoundStyle.NONE,
                  Range.closed(0.0, 100.0)),
              new PaintScale(PaintScaleColorStyle.RED, PaintScaleBoundStyle.NONE,
                  Range.closed(0.0, 100.0)),
              new PaintScale(PaintScaleColorStyle.GREEN, PaintScaleBoundStyle.NONE,
                  Range.closed(0.0, 100.0)),
              new PaintScale(PaintScaleColorStyle.CYAN, PaintScaleBoundStyle.NONE,
                  Range.closed(0.0, 100.0)),
              new PaintScale(PaintScaleColorStyle.YELLOW, PaintScaleBoundStyle.NONE,
                  Range.closed(0.0, 100.0))});
  public static final WindowSettingsParameter windowSettings = new WindowSettingsParameter();

  public ImsVisualizerParameters() {
    super(new Parameter[] {dataFiles, scanSelection, paintScale, mzRange, windowSettings});
  }
}
*/