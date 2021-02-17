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
 *
 * Edited and modified by Owen Myers (Oweenm@gmail.com)
 */

package io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelectionParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.util.ExitCode;

public class ADAPChromatogramBuilderParameters extends SimpleParameterSet {

  public static final RawDataFilesParameter dataFiles = new RawDataFilesParameter();

  public static final ScanSelectionParameter scanSelection =
      new ScanSelectionParameter(new ScanSelection(1));

  public static final IntegerParameter minimumScanSpan = new IntegerParameter(
      "Min group size in # of scans",
      "Minimum scan span over which some feature in the chromatogram must have (continuous) points above the noise level\n"
          + "to be recognized as a chromatogram.\n"
          + "The optimal value depends on the chromatography system setup. The best way to set this parameter\n"
          + "is by studying the raw data and determining what is the typical time span of chromatographic features.",
      5, true, 2, null);

  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter();

  public static final StringParameter suffix =
      new StringParameter("Suffix", "This string is added to filename as suffix", "chromatograms");

  // Owen Edit
  public static final DoubleParameter IntensityThresh2 = new DoubleParameter(
      "Group intensity threshold",
      "This parameter is the intensity value for wich intensities greater than this value can contribute to the minimumScanSpan count.",
      MZmineCore.getConfiguration().getIntensityFormat());

  public static final DoubleParameter startIntensity = new DoubleParameter("Min highest intensity",
      "Points below this intensity will not be considered in starting a new chromatogram",
      MZmineCore.getConfiguration().getIntensityFormat());
  // End Owen Edit

  public ADAPChromatogramBuilderParameters() {
    super(new Parameter[]{dataFiles, scanSelection, minimumScanSpan, IntensityThresh2,
        startIntensity, mzTolerance, suffix});
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    String message = "<html>ADAP Module Disclaimer:"
        + "<br> If you use the ADAP Chromatogram Builder Module, please cite the "
        + "<a href=\"https://bmcbioinformatics.biomedcentral.com/articles/10.1186/1471-2105-11-395\">MZmine2 paper</a> and the following article:"
        + "<br><a href=\"http://pubs.acs.org/doi/abs/10.1021/acs.analchem.7b00947\"> Myers OD, Sumner SJ, Li S, Barnes S, Du X: One Step Forward for Reducing False Positive and False Negative "
        + "<br>Compound Identifications from Mass Spectrometry Metabolomics Data: New Algorithms for Constructing Extracted "
        + "<br>Ion Chromatograms and Detecting Chromatographic Features. Anal Chem 2017, DOI: 10.1021/acs.analchem.7b00947</a>"
        + "</html>";
    ParameterSetupDialog dialog = new ParameterSetupDialog(valueCheckRequired, this, message);
    dialog.showAndWait();
    return dialog.getExitCode();
  }

  @Override
  public String getRestrictedIonMobilitySupportMessage() {
    return "ADAP chromatogram builder will build two-dimensional chromatograms based on summed "
        + "frame data (if there is any). Thus, the mobility dimension is not taken into account. "
        + "Do you wish to continue any way?";
  }

  @Override
  public IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.RESTRICTED;
  }
}
