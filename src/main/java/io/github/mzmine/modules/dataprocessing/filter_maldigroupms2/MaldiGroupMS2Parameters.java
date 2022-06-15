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

package io.github.mzmine.modules.dataprocessing.filter_maldigroupms2;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import org.jetbrains.annotations.NotNull;

public class MaldiGroupMS2Parameters extends SimpleParameterSet {

  public static final FeatureListsParameter PEAK_LISTS = new FeatureListsParameter();

  public static final RawDataFilesParameter files = new RawDataFilesParameter();

  public static final MZToleranceParameter mzTol = new MZToleranceParameter(
      "MS1 to MS2 precursor tolerance (m/z)",
      "Describes the tolerance between the precursor ion in a MS1 scan and the precursor "
          + "m/z assigned to the MS2 scan.", 0.01, 10);

  public static final BooleanParameter combineTimsMsMs = new BooleanParameter(
      "Combine MS/MS spectra (TIMS)",
      "If checked, all assigned MS/MS spectra with the same collision energy will be merged into a single MS/MS spectrum.",
      false);

  public static final BooleanParameter lockMS2ToFeatureMobilityRange = new BooleanParameter(
      "Lock to feature mobility range",
      "If checked, only mobility scans from the mobility range of the feature will be merged.\n"
          + "This is usually not needed. However, if isomeres/isobares elute at the same retention time and are close in mobility, "
          + "the MS/MS window might be larger than the peak in mobility dimension and thus cause chimeric MS/MS spectra.\n"
          + "This can be investigated in hte \"All MS MS\" window", false);

  public static final OptionalParameter<DoubleParameter> outputNoiseLevel = new OptionalParameter<>(
      new DoubleParameter("Minimum merged intensity (IMS)",
          "If an ion mobility spectrometry (IMS) feature is processed, this parameter "
              + "can be used to filter low abundant peaks in the MS/MS spectrum, since multiple "
              + "MS/MS mobility scans need to be merged together.",
          MZmineCore.getConfiguration().getIntensityFormat(), 250d, 0d, Double.MAX_VALUE));

  public MaldiGroupMS2Parameters() {
    super(new Parameter[]{PEAK_LISTS, files, mzTol, combineTimsMsMs,
        lockMS2ToFeatureMobilityRange, outputNoiseLevel}, "https://mzmine.github.io/mzmine_documentation/module_docs/featdet_ms2_scan_pairing/ms2_scan_pairing.html");
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }
}
