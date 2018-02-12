/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package net.sf.mzmine.modules.visualization.kendrickmassplot;

import java.awt.Window;
import java.text.DecimalFormat;
import java.util.Arrays;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.WindowSettingsParameter;
import net.sf.mzmine.parameters.parametertypes.ranges.DoubleRangeParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakSelectionParameter;
import net.sf.mzmine.util.ExitCode;
import net.sf.mzmine.util.PeakListRowSorter;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;

/**
 * parameters for Kendrick mass plots
 * 
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class KendrickMassPlotParameters extends SimpleParameterSet {
  public static final PeakListsParameter peakList = new PeakListsParameter(1, 1);

  public static final PeakSelectionParameter selectedRows = new PeakSelectionParameter();

  public static final ComboParameter<String> yAxisValues = new ComboParameter<>("Y-Axis",
      "Select the kendrick mass defect base", new String[] {"KMD (CH2)", "KMD (H)", "KMD (O)"});

  public static final ComboParameter<String> xAxisValues = new ComboParameter<>("X-Axis",
      "Select a second kendrick mass defect base, kendrick masse (KM) or m/z",
      new String[] {"m/z", "KM", "KMD (CH2)", "KMD (H)", "KMD (O)"});

  public static final ComboParameter<String> zAxisValues = new ComboParameter<>("Z-Axis",
      "Select a parameter for a third dimension, displayed as a heatmap or select none for a 2D plot",
      new String[] {"none", "Retention time", "Intensity", "Area", "Tailing factor",
          "Asymmetry factor", "FWHM", "KMD (CH2)", "KMD (H)", "KMD (O)", "m/z"});

  public static final ComboParameter<String> zScaleType = new ComboParameter<>("Z-Axis scale",
      "Select Z-Axis scale", new String[] {"percentile", "custom"});

  public static final DoubleRangeParameter zScaleRange = new DoubleRangeParameter(
      "Range for z-Axis scale",
      "Set the range for z-Axis scale."
          + " If percentile is used for z-Axis scale type, you can remove extreme values of the scale."
          + " E. g. type 0.5 and 99.5 to ignore the 0.5 smallest and 0.5 highest values. "
          + "If you choose custom, set ranges manually "
          + "Features out of scale range are displayed in magenta",
      new DecimalFormat("##0.00"));

  public static final ComboParameter<String> paintScale = new ComboParameter<>("Heatmap style",
      "Select the style for the third dimension", new String[] {"Rainbow", "Monochrome red",
          "Monochrome green", "Monochrome yellow", "Monochrome cyan"});

  public static final WindowSettingsParameter windowSettings = new WindowSettingsParameter();

  public KendrickMassPlotParameters() {
    super(new Parameter[] {peakList, selectedRows, yAxisValues, xAxisValues, zAxisValues,
        zScaleType, zScaleRange, paintScale, windowSettings});
  }

  @Override
  public ExitCode showSetupDialog(Window parent, boolean valueCheckRequired) {

    PeakList selectedPeakLists[] = getParameter(peakList).getValue().getMatchingPeakLists();
    if (selectedPeakLists.length > 0) {
      PeakListRow plRows[] = selectedPeakLists[0].getRows();
      Arrays.sort(plRows, new PeakListRowSorter(SortingProperty.MZ, SortingDirection.Ascending));
    }

    return super.showSetupDialog(parent, valueCheckRequired);
  }

}
