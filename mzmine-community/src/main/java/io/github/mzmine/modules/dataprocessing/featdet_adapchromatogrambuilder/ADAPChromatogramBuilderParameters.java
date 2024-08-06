/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

import static io.github.mzmine.javafx.components.factories.FxTexts.boldText;
import static io.github.mzmine.javafx.components.factories.FxTexts.hyperlinkText;
import static io.github.mzmine.javafx.components.factories.FxTexts.linebreak;
import static io.github.mzmine.javafx.components.factories.FxTexts.text;

import io.github.mzmine.javafx.components.factories.ArticleReferences;
import io.github.mzmine.javafx.components.factories.FxTextFlows;
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
import io.github.mzmine.parameters.parametertypes.tolerances.ToleranceType;
import io.github.mzmine.util.ExitCode;
import java.util.Collection;
import java.util.Map;
import javafx.application.Platform;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Region;
import org.jetbrains.annotations.NotNull;

/**
 * Important Note: when changing any of the parameter names, reflect the changes in the
 * {@link io.github.mzmine.modules.dataprocessing.featdet_imagebuilder.ImageBuilderParameters} to
 * keep the compatibility.
 */
public class ADAPChromatogramBuilderParameters extends SimpleParameterSet {

  public static final RawDataFilesParameter dataFiles = new RawDataFilesParameter();

  public static final ScanSelectionParameter scanSelection = new ScanSelectionParameter(
      new ScanSelection(1));

  public static final IntegerParameter minimumConsecutiveScans = new IntegerParameter(
      "Minimum consecutive scans", """
      This number of scans needs to be above the specified 'Minimum intensity for consecutive scans' to detect EICs.
      The optimal value depends on the chromatography system setup. The best way to set this parameter
      is by studying the raw data and determining what is the typical time span (number of data points) of chromatographic features.""",
      5, true, 1, null);

  public static final DoubleParameter minGroupIntensity = new DoubleParameter(
      "Minimum intensity for consecutive scans", """
      This threshold is only used to find consecutive scans (data points) above a certain intensity.
      All data points, even below this level can be added to a chromatogram but at least N consecutive scans need to be above.
      """, MZmineCore.getConfiguration().getIntensityFormat(), 0d);

  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter(
      ToleranceType.SCAN_TO_SCAN, 0.002, 10);

  public static final StringParameter suffix = new StringParameter("Suffix",
      "This string is added to filename as suffix", "chromatograms");


  public static final DoubleParameter minHighestPoint = new DoubleParameter(
      "Minimum absolute height",
      "Points below this intensity will not be considered in starting a new chromatogram",
      MZmineCore.getConfiguration().getIntensityFormat());

  public static final HiddenParameter<Map<String, Boolean>> allowSingleScans = new HiddenParameter<>(
      new OptOutParameter("Allow single scan chromatograms",
          "Allows selection of single scans as chromatograms. This is useful for "
          + "feature table generation if MALDI point measurements."));

  public ADAPChromatogramBuilderParameters() {
    super(new Parameter[]{dataFiles, scanSelection, minimumConsecutiveScans, minGroupIntensity,
            minHighestPoint, mzTolerance, suffix, allowSingleScans},
        "https://mzmine.github.io/mzmine_documentation/module_docs/lc-ms_featdet/featdet_adap_chromatogram_builder/adap-chromatogram-builder.html");
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    assert Platform.isFxApplicationThread();

    final Region message = FxTextFlows.newTextFlowInAccordion("How to cite",
        boldText("ADAP Module Disclaimer:\n"),
        text("If you use the ADAP Chromatogram Builder Module, please cite: "), linebreak(),
        boldText("mzmine paper "), ArticleReferences.MZMINE3.hyperlinkText(), linebreak(),
        text("and the following article: "), hyperlinkText(
            "Myers OD, Sumner SJ, Li S, Barnes S, Du X, Anal. Chem. 2017, 89, 17, 8696–8703",
            "http://pubs.acs.org/doi/abs/10.1021/acs.analchem.7b00947"));

    ParameterSetupDialog dialog = new ParameterSetupDialog(valueCheckRequired, this, message);
    dialog.showAndWait();
    return dialog.getExitCode();
  }

  @Override
  public String getRestrictedIonMobilitySupportMessage() {
    return "ADAP chromatogram builder will build two-dimensional chromatograms based on summed "
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

    final Boolean singleScansOkOptOut = getParameter(allowSingleScans).getValue()
        .get("optoutsinglescancheck");

    if (getParameter(minimumConsecutiveScans).getValue() <= 1 && (singleScansOkOptOut == null
                                                                  || !singleScansOkOptOut)) {
      ButtonType buttonType = MZmineCore.getDesktop()
          .createAlertWithOptOut("Confirmation", "Single consecutive scan selected.",
              "The number of consecutive scans was set to <= 1.\nThis can lead to more noise"
              + " detected as EICs.\nDo you want to proceed?", "Do not show again.",
              b -> this.getParameter(allowSingleScans).getValue().put("optoutsinglescancheck", b));
      return buttonType.equals(ButtonType.YES);
    }
    return true;
  }


  @Override
  public Map<String, Parameter<?>> getNameParameterMap() {
    // parameters were renamed but stayed the same type
    var nameParameterMap = super.getNameParameterMap();
    // we use the same parameters here so no need to increment the version. Loading will work fine
    nameParameterMap.put("Min group size in # of scans", minimumConsecutiveScans);
    nameParameterMap.put("Group intensity threshold", minGroupIntensity);
    nameParameterMap.put("Min highest intensity", minHighestPoint);
    nameParameterMap.put("Scans", scanSelection);
    nameParameterMap.put("Scan to scan accuracy (m/z)", mzTolerance);
    return nameParameterMap;
  }
}
