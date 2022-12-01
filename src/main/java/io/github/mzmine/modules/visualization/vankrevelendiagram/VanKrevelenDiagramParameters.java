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

package io.github.mzmine.modules.visualization.vankrevelendiagram;

import com.google.common.collect.Range;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScale;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScaleBoundStyle;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScaleColorStyle;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.PaintScaleParameter;
import io.github.mzmine.parameters.parametertypes.WindowSettingsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureSelectionParameter;
import io.github.mzmine.util.ExitCode;
import javafx.collections.FXCollections;

/*
 * Van Krevelen diagram class
 *
 * @author Ansgar Korf (ansgar.korf@uni-muenster)
 */
public class VanKrevelenDiagramParameters extends SimpleParameterSet {
  public static final FeatureListsParameter featureList = new FeatureListsParameter(1, 1);

  public static final FeatureSelectionParameter selectedRows = new FeatureSelectionParameter();

  public static final ComboParameter<String> zAxisValues = new ComboParameter<>("Z-Axis",
      "Select a parameter for a third dimension, displayed as a heatmap or select none for a 2D plot",
      FXCollections.observableArrayList("none", "Retention time", "Intensity", "Area",
          "Tailing factor", "Asymmetry factor", "FWHM", "m/z"));

  public static final PaintScaleParameter paintScale =
      new PaintScaleParameter("Color scale", "Select paint scale",
          new PaintScale[] {
              new PaintScale(PaintScaleColorStyle.RAINBOW, PaintScaleBoundStyle.NONE,
                  Range.closed(0.0, 100.0)),
              new PaintScale(PaintScaleColorStyle.GRREN_RED, PaintScaleBoundStyle.NONE,
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

  public VanKrevelenDiagramParameters() {
    super(new Parameter[] {featureList, selectedRows, zAxisValues, paintScale, windowSettings},
        "https://mzmine.github.io/mzmine_documentation/visualization_modules/processed_additional/processed_additional.html#van-krevelen-diagram");
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    return super.showSetupDialog(valueCheckRequired);
  }

}
