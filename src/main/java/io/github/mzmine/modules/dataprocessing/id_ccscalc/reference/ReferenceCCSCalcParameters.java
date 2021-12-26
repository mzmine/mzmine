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

package io.github.mzmine.modules.dataprocessing.id_ccscalc.reference;

import com.google.common.collect.Range;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import io.github.mzmine.parameters.parametertypes.ranges.RTRangeParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance.MobilityTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance.MobilityToleranceParameter;

public class ReferenceCCSCalcParameters extends SimpleParameterSet {

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
      Range.all());

  public static final DoubleParameter minHeight = new DoubleParameter("Minumum height",
      "The minimum intensity of a calibrant feature to be used for calibration.",
      MZmineCore.getConfiguration().getIntensityFormat(), 1E3);

  public ReferenceCCSCalcParameters() {
    super(new Parameter[]{referenceList, mzTolerance, mobTolerance, rtRange, minHeight});
  }
}
