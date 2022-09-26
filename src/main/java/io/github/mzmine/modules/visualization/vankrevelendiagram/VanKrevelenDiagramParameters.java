/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
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
