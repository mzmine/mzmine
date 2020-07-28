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

package io.github.mzmine.modules.dataprocessing.masscalibration;

import com.google.common.collect.Range;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.MassListParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.combonested.NestedComboParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import io.github.mzmine.parameters.parametertypes.ranges.DoubleRangeParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;
import io.github.mzmine.util.ExitCode;

import java.text.NumberFormat;
import java.util.TreeMap;

public class MassCalibrationParameters extends SimpleParameterSet {

  public static final RawDataFilesParameter dataFiles = new RawDataFilesParameter();

  public static final MassListParameter massList = new MassListParameter();

  public static final FileNameParameter standardsList = new FileNameParameter("Standards list",
          "File with a list of standard calibrants (ionic formula and retention time)" +
                  " expected to appear in the dataset", FileSelectionType.OPEN);

  public static final BooleanParameter filterDuplicates = new BooleanParameter("Filter out duplicate errors",
          "If checked, the distribution of errors will be filtered to remove duplicates");

  public static final DoubleParameter tolerance = new DoubleParameter("Error range tolerance",
          "Error range tolerance is the max distance allowed between errors to be included in the same range." +
                  " This is used when extending the most populated error range, if next closest error is within that" +
                  " tolerance, the range is extended to contain it. The process is repeated until no new errors can be" +
                  " included in that range. The tolerance is the absolute difference between PPM errors of m/z ratio." +
                  " See help for more details.",
          NumberFormat.getNumberInstance(), 0.4, 0.0, Double.POSITIVE_INFINITY);

  public static final DoubleParameter rangeSize = new DoubleParameter("Most populated error range size",
          "The maximum length of the range that contains the most errors. The module searches for a range" +
                  " of error values that is up to this size and contains the most errors, this way a high-density error" +
                  " range can be established. The range size is the difference between upper and lower endpoint" +
                  " of the range, both are values of PPM errors of m/z ratio. See help for more details.",
          NumberFormat.getNumberInstance(), 2.0, 0.0, Double.POSITIVE_INFINITY);

  public static final DoubleRangeParameter percentileRange = new DoubleRangeParameter("Interpercentile range",
          "The interpercentile range used for extraction of errors.", NumberFormat.getNumberInstance(), true,false,
          Range.closed(25.0, 75.0), Range.closed(0.0, 100.0));

  public static final TreeMap<String, ParameterSet> rangeExtractionChoices = new TreeMap<>() {{
    put("range method", new SimpleParameterSet(new Parameter[]{tolerance, rangeSize}));
    put("interpercentile range", new SimpleParameterSet(new Parameter[]{percentileRange}));
  }};

  public static final NestedComboParameter rangeExtractionMethod = new NestedComboParameter("Range extraction method",
          "Method used to extract range of errors considered substantial to the bias estimation of" +
                  " mass peaks m/z measurement", rangeExtractionChoices, "range method", true, 250);

  public static final MZToleranceParameter mzRatioTolerance = new MZToleranceParameter("mz ratio tolerance",
          "Max difference between actual mz peaks and standard calibrants to consider a match," +
                  " max of m/z and ppm is used", 0.001, 5, true);

  public static final RTToleranceParameter retentionTimeTolerance = new RTToleranceParameter("Retention time tolerance",
          "Max retention time difference between mass peaks and standard calibrants to consider a match.");

  public static final StringParameter suffix = new StringParameter("Suffix",
          "This string is added to mass list name as a suffix", "calibrated");

  public static final BooleanParameter autoRemove =
          new BooleanParameter("Remove original mass list",
                  "If checked, original mass list will be removed and only filtered version remains");

  public MassCalibrationParameters() {
    super(new Parameter[]{dataFiles, massList, standardsList, mzRatioTolerance, retentionTimeTolerance,
            filterDuplicates, rangeExtractionMethod, suffix, autoRemove});
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    MassCalibrationSetupDialog dialog = new MassCalibrationSetupDialog(valueCheckRequired, this);
    dialog.showAndWait();
    return dialog.getExitCode();
  }

}
