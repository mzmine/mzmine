/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.visualization.kendrickmassplot;

import com.google.common.collect.Range;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScale;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScaleBoundStyle;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScaleColorStyle;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.PaintScaleParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.WindowSettingsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureSelectionParameter;
import io.github.mzmine.util.ExitCode;

/**
 * parameters for Kendrick mass plots
 *
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class KendrickMassPlotParameters extends SimpleParameterSet {
  public static final FeatureListsParameter featureList = new FeatureListsParameter(1, 1);

  public static final FeatureSelectionParameter selectedRows = new FeatureSelectionParameter();

  public static final StringParameter yAxisCustomKendrickMassBase =
      new StringParameter("Kendrick mass base for y-Axis",
          "Enter a sum formula for a Kendrick mass base, e.g. \"CH2\" ");

  public static final ComboParameter<String> xAxisValues = new ComboParameter<>("X-Axis",
      "Select Kendrick mass (KM) or m/z", new String[] {"m/z", "KM"});

  public static final OptionalParameter<StringParameter> xAxisCustomKendrickMassBase =
      new OptionalParameter<>(new StringParameter("Kendrick mass base for x-Axis",
          "Enter a sum formula for a Kendrick mass base to display a 2D Kendrick mass defect plot"));

  public static final ComboParameter<String> zAxisValues = new ComboParameter<>("Z-Axis",
      "Select a parameter for a third dimension, displayed as a heatmap or select none for a 2D plot",
      new String[] {"none", "Retention time", "Intensity", "Area", "Tailing factor",
          "Asymmetry factor", "FWHM", "m/z"});

  public static final OptionalParameter<StringParameter> zAxisCustomKendrickMassBase =
      new OptionalParameter<>(new StringParameter("Kendrick mass base for z-Axis",
          "Enter a sum formula for a Kendrick mass base to display a Kendrick mass defect in form of a heatmap"));

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

  public KendrickMassPlotParameters() {
    super(new Parameter[] {featureList, selectedRows, yAxisCustomKendrickMassBase, xAxisValues,
        xAxisCustomKendrickMassBase, zAxisValues, zAxisCustomKendrickMassBase, paintScale,
        windowSettings});
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    return super.showSetupDialog(valueCheckRequired);
  }

}
