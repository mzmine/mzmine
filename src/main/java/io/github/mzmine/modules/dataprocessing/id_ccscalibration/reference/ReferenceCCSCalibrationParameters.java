/*
 * Copyright 2006-2022 The MZmine Development Team
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
 *
 */

package io.github.mzmine.modules.dataprocessing.id_ccscalibration.reference;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import io.github.mzmine.parameters.parametertypes.ranges.RTRangeParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelection;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance.MobilityTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance.MobilityToleranceParameter;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;

public class ReferenceCCSCalibrationParameters extends SimpleParameterSet {

  public static final OptionalParameter<RawDataFilesParameter> files = new OptionalParameter<>(
      new RawDataFilesParameter("Set to additional raw files", 0, Integer.MAX_VALUE), false);

  public static final FeatureListsParameter flists = new FeatureListsParameter(
      "Feature list (with reference compounds)", 1, Integer.MAX_VALUE);

  public static final FileNameParameter referenceList = new FileNameParameter("Reference list",
      "The file containing the reference compounds for m/z and mobility.", FileSelectionType.OPEN,
      false);

  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter("m/z tolerance",
      "Tolerance for the given reference compound list", 0.005, 5);

  public static final MobilityToleranceParameter mobTolerance = new MobilityToleranceParameter(
      "Mobility tolerance", "Tolerance for the given reference compound list",
      new MobilityTolerance(0.1f));

  public static final RTRangeParameter rtRange = new RTRangeParameter(
      "Calibration segment RT range", "The rt range of the calibration segment.", true,
      Range.closed(0d, 60d));

  public static final DoubleParameter minHeight = new DoubleParameter("Minumum height",
      "The minimum intensity of a calibrant feature to be used for calibration.",
      MZmineCore.getConfiguration().getIntensityFormat(), 1E3);

  public ReferenceCCSCalibrationParameters() {
    super(new Parameter[]{files, flists, referenceList, mzTolerance, mobTolerance, rtRange,
        minHeight}, "https://mzmine.github.io/mzmine_documentation/module_docs/id_ccs_calibration/ccs_calibration.html#reference-css-calibration");
  }

  @Override
  public boolean checkParameterValues(Collection<String> errorMessages) {
    if (!super.checkParameterValues(errorMessages)) {
      return false;
    }

    boolean check = true;

    if (getValue(files)) {
      final RawDataFilesSelection value = getParameter(files).getEmbeddedParameter().getValue();
      final RawDataFilesSelection clone = value.clone();
      final RawDataFile[] files = clone.getMatchingRawDataFiles(); // dont evaluate the real parameter, otherwise we have to reset it.

      ModularFeatureList[] flists = getValue(
          ReferenceCCSCalibrationParameters.flists).getMatchingFeatureLists();

      if (files.length != 0 && flists.length > 1) {
        errorMessages.add(
            "Invalid parameter selection. Either select one feature list and >= 1 raw data file or no raw data files. (Reference calibration)");
        check = false;
      }
    }

    return check;
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.ONLY;
  }
}
