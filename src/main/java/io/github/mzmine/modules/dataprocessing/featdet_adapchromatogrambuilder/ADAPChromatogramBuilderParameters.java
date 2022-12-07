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

package io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.HiddenParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.OptOutParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelectionParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.util.ExitCode;
import java.util.Collection;
import java.util.Map;
import javafx.scene.control.ButtonType;
import org.jetbrains.annotations.NotNull;

/**
 * Important Note: when changing any of the parameter names, reflect the changes
 * in the
 * {@link
 * io.github.mzmine.modules.dataprocessing.featdet_imagebuilder.ImageBuilderParameters}
 * to keep the compatibility.
 */
public class ADAPChromatogramBuilderParameters extends SimpleParameterSet {

  public static final RawDataFilesParameter dataFiles = new RawDataFilesParameter();

  public static final ScanSelectionParameter scanSelection = new ScanSelectionParameter(
      new ScanSelection(1));

  public static final IntegerParameter minimumScanSpan = new IntegerParameter(
      "Min group size in # of scans", """
      Minimum scan span over which some feature in the chromatogram must have (continuous) points above the noise level
      to be recognized as a chromatogram.
      The optimal value depends on the chromatography system setup. The best way to set this parameter
      is by studying the raw data and determining what is the typical time span of chromatographic features.""",
      5, true, 1, null);

  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter(
      "Scan to scan accuracy (m/z)", """
      m/z tolerance of the same compound between two scans.
      This does not describe the deviation of the accurate mass (measured) from the exact mass (calculated),
      but the fluctuation of the accurate between two scans.""", 0.002, 10);

  public static final StringParameter suffix = new StringParameter("Suffix",
      "This string is added to filename as suffix", "chromatograms");

  // Owen Edit
  public static final DoubleParameter minGroupIntensity = new DoubleParameter(
      "Group intensity threshold",
      "This parameter is the intensity value for which intensities greater than this value can contribute to the minimumScanSpan count.",
      MZmineCore.getConfiguration().getIntensityFormat());

  public static final DoubleParameter minHighestPoint = new DoubleParameter(
      "Min highest intensity",
      "Points below this intensity will not be considered in starting a new chromatogram",
      MZmineCore.getConfiguration().getIntensityFormat());
  // End Owen Edit

  public static final HiddenParameter<OptOutParameter, Map<String, Boolean>> allowSingleScans = new HiddenParameter<>(
      new OptOutParameter("Allow single scan chromatograms",
          "Allows selection of single scans as chromatograms. This is useful for "
              + "feature table generation if MALDI point measurements."));

  public ADAPChromatogramBuilderParameters() {
    super(new Parameter[]{dataFiles, scanSelection, minimumScanSpan,
            minGroupIntensity, minHighestPoint, mzTolerance, suffix,
            allowSingleScans},
        "https://mzmine.github.io/mzmine_documentation/module_docs/lc-ms_featdet/featdet_adap_chromatogram_builder/adap-chromatogram-builder.html");
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
    ParameterSetupDialog dialog = new ParameterSetupDialog(valueCheckRequired,
        this, message);
    dialog.showAndWait();
    return dialog.getExitCode();
  }

  @Override
  public String getRestrictedIonMobilitySupportMessage() {
    return
        "ADAP chromatogram builder will build two-dimensional chromatograms based on summed "
            + "frame data (if there is any). Thus, the mobility dimension is not taken into account. "
            + "The mobility dimension can be added by the IMS expander module after feature resolving. "
            + "Do you wish to continue?";
  }

  @NotNull
  @Override
  public IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }

  @Override
  public boolean checkParameterValues(Collection<String> errorMessages) {
    if (!super.checkParameterValues(errorMessages)) {
      return false;
    }

    final Boolean singleScansOkOptOut = getParameter(
        allowSingleScans).getValue().get("optoutsinglescancheck");

    if (getParameter(minimumScanSpan).getValue() <= 1 && (
        singleScansOkOptOut == null || singleScansOkOptOut == false)) {
      ButtonType buttonType = MZmineCore.getDesktop()
          .createAlertWithOptOut("Confirmation",
              "Single consecutive scan selected.",
              "The number of consecutive scans was set to <= 1.\nThis can lead to more noise"
                  + " detected as EICs.\nDo you want to proceed?",
              "Do not show again.",
              b -> this.getParameter(allowSingleScans).getValue()
                  .put("optoutsinglescancheck", b));
      if (buttonType.equals(ButtonType.YES)) {
        return true;
      }
      return false;
    }
    return true;
  }
}
