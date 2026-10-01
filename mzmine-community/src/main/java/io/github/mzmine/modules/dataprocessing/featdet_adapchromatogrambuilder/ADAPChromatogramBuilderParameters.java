/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.javafx.components.factories.ArticleReferences;
import io.github.mzmine.javafx.components.factories.FxTextFlows;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.featdet_fastchromatogrambuilder.ChromatogramBuilderSensitivity;
import io.github.mzmine.modules.dataprocessing.featdet_fastchromatogrambuilder.FastAutoChromatogramBuilderParameters;
import io.github.mzmine.modules.dataprocessing.featdet_fastchromatogrambuilder.FastChromatogramBuilderParameters;
import io.github.mzmine.modules.dataprocessing.norm_rtcalibration2.RTCorrectionParameters;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.HiddenParameter;
import io.github.mzmine.parameters.parametertypes.OptOutParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.combowithinput.MZToleranceOrAuto;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelection;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelectionParameter;
import io.github.mzmine.parameters.parametertypes.submodules.ModuleOptionsEnumComboParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.ExitCode;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javafx.application.Platform;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Region;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Parameters of the {@link ModularADAPChromatogramBuilderModule}. The builder parameters depend on
 * the {@link ChromatogramBuilderAlgorithms algorithm}.
 * <p>
 * decision: batch steps from before the algorithm selection (version 1) load as
 * {@link ChromatogramBuilderAlgorithms#LEGACY_ADAP}, their builder parameters are top level
 * parameters with the names of the {@link LegacyAdapChromatogramBuilderParameters}.
 * <p>
 * Important Note: when changing any of the parameter names, reflect the changes in the
 * {@link io.github.mzmine.modules.dataprocessing.featdet_imagebuilder.ImageBuilderParameters} to
 * keep the compatibility.
 */
public class ADAPChromatogramBuilderParameters extends SimpleParameterSet {

  public static final RawDataFilesParameter dataFiles = new RawDataFilesParameter();

  public static final ScanSelectionParameter scanSelection = new ScanSelectionParameter(
      new ScanSelection(1));

  public static final ModuleOptionsEnumComboParameter<ChromatogramBuilderAlgorithms> algorithm = new ModuleOptionsEnumComboParameter<>(
      "Algorithm", """
      Fast (auto): the fast chromatogram builder with all parameters determined from the data \
      for a sensitivity.
      Fast: the fast chromatogram builder with user defined parameters.
      mzmine <4.11: the ADAP chromatogram builder of earlier mzmine versions, old batches load \
      with this algorithm.""", ChromatogramBuilderAlgorithms.FAST_AUTO);

  public static final StringParameter suffix = new StringParameter("Suffix",
      "This string is added to filename as suffix", "chromatograms");

  public static final BooleanParameter clearRtCorrection = new BooleanParameter(
      RTCorrectionParameters.clearPreviousCorrection.getName(), """
      If a file is processed multimple times, clearing potentially applied RT corrections ensures that
      the processing is reproducible for multiple runs. If no correction was applied previously,
      this parameter has no effect. Default = enabled.""", true);

  public static final HiddenParameter<Map<String, Boolean>> allowSingleScans = new HiddenParameter<>(
      new OptOutParameter("Allow single scan chromatograms",
          "Allows selection of single scans as chromatograms. This is useful for "
              + "feature table generation if MALDI point measurements."));

  public ADAPChromatogramBuilderParameters() {
    super(new Parameter[]{dataFiles, scanSelection, algorithm, suffix, clearRtCorrection,
            allowSingleScans},
        "https://mzmine.github.io/mzmine_documentation/module_docs/lc-ms_featdet/featdet_adap_chromatogram_builder/adap-chromatogram-builder.html");
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    assert Platform.isFxApplicationThread();

    final Region message = FxTextFlows.newTextFlowInAccordion("How to cite",
        boldText("ADAP Module Disclaimer:\n"), text("If you use the "),
        boldText(ChromatogramBuilderAlgorithms.LEGACY_ADAP.toString()),
        text(" algorithm (ADAP chromatogram builder), please cite: "), linebreak(),
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
    return "The chromatogram builder will build two-dimensional chromatograms based on summed "
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

    // the auto parameters of the fast builder never use a single scan
    final ParameterSet algorithmParameters = getEmbeddedParameterValue(algorithm);
    final Integer minConsecutive = switch (getValue(algorithm)) {
      case FAST_AUTO -> null;
      case FAST -> algorithmParameters.getValue(
          FastChromatogramBuilderParameters.minimumConsecutiveScans);
      case LEGACY_ADAP -> algorithmParameters.getValue(
          LegacyAdapChromatogramBuilderParameters.minimumConsecutiveScans);
    };

    if (minConsecutive != null && minConsecutive <= 1 && (singleScansOkOptOut == null
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
    final var nameParameterMap = super.getNameParameterMap();
    // version 1 had the builder parameters at the top level, they load into the legacy algorithm.
    // Newer versions have no top level parameters of these names.
    final ParameterSet legacy = getParameter(algorithm).getEmbeddedParameters(
        ChromatogramBuilderAlgorithms.LEGACY_ADAP);
    for (final Parameter<?> parameter : legacy.getParameters()) {
      nameParameterMap.put(parameter.getName(), parameter);
    }
    nameParameterMap.putAll(LegacyAdapChromatogramBuilderParameters.legacyNames(legacy));
    // renamed before version 1
    nameParameterMap.put("Scans", getParameter(scanSelection));
    return nameParameterMap;
  }

  @Override
  public void handleLoadedParameters(Map<String, Parameter<?>> loadedParams, int loadedVersion) {
    super.handleLoadedParameters(loadedParams, loadedVersion);
    if (!loadedParams.containsKey(clearRtCorrection.getName())) {
      setParameter(clearRtCorrection, true);
    }
    if (!loadedParams.containsKey(algorithm.getName())) {
      // decision: old batches keep their results
      setParameter(algorithm, ChromatogramBuilderAlgorithms.LEGACY_ADAP);
    }
  }

  @Override
  public int getVersion() {
    return 2;
  }

  @Override
  public @Nullable String getVersionMessage(int version) {
    return switch (version) {
      case 2 -> """
          The chromatogram builder now offers the algorithms "%s" (default) and "%s". The loaded \
          step uses "%s", the ADAP algorithm of earlier versions, with the same parameters and \
          results.""".formatted(ChromatogramBuilderAlgorithms.FAST_AUTO,
          ChromatogramBuilderAlgorithms.FAST, ChromatogramBuilderAlgorithms.LEGACY_ADAP);
      default -> null;
    };
  }

  /**
   * Parameters of the ADAP chromatogram builder of mzmine before 4.11.
   */
  @NotNull
  public static ADAPChromatogramBuilderParameters createLegacy(
      @NotNull RawDataFilesSelection files, @NotNull ScanSelection scans, int minRtDataPoints,
      @NotNull MZTolerance mzTolScans, @NotNull String suffix, double minGroupInt,
      double minHeight, boolean clearRtCorrection) {
    return create(files, scans, ChromatogramBuilderAlgorithms.LEGACY_ADAP,
        LegacyAdapChromatogramBuilderParameters.create(minRtDataPoints, minGroupInt, minHeight,
            mzTolScans), suffix, clearRtCorrection);
  }

  /**
   * Parameters of the fast chromatogram builder with user defined values.
   */
  @NotNull
  public static ADAPChromatogramBuilderParameters createFast(@NotNull RawDataFilesSelection files,
      @NotNull ScanSelection scans, int minConsecutiveScans, @NotNull MZToleranceOrAuto mzTolScans,
      @NotNull String suffix, double minGroupInt, double minHeight, boolean clearRtCorrection) {
    return create(files, scans, ChromatogramBuilderAlgorithms.FAST,
        FastChromatogramBuilderParameters.create(minConsecutiveScans, minGroupInt, minHeight,
            mzTolScans), suffix, clearRtCorrection);
  }

  /**
   * Parameters of the fast chromatogram builder with values determined from the data.
   */
  @NotNull
  public static ADAPChromatogramBuilderParameters createFastAuto(
      @NotNull RawDataFilesSelection files, @NotNull ScanSelection scans,
      @NotNull ChromatogramBuilderSensitivity sensitivity, @NotNull String suffix,
      boolean clearRtCorrection) {
    return create(files, scans, ChromatogramBuilderAlgorithms.FAST_AUTO,
        FastAutoChromatogramBuilderParameters.create(sensitivity), suffix, clearRtCorrection);
  }

  @NotNull
  private static ADAPChromatogramBuilderParameters create(@NotNull RawDataFilesSelection files,
      @NotNull ScanSelection scans, @NotNull ChromatogramBuilderAlgorithms algorithmValue,
      @NotNull ParameterSet algorithmParameters, @NotNull String suffixValue,
      boolean clearRtCorrectionValue) {
    final var param = new ADAPChromatogramBuilderParameters().cloneParameterSet();
    param.setParameter(dataFiles, files);
    param.setParameter(scanSelection, scans);
    param.getParameter(algorithm).setValue(algorithmValue, algorithmParameters);
    param.setParameter(suffix, suffixValue);
    param.setParameter(clearRtCorrection, clearRtCorrectionValue);
    return (ADAPChromatogramBuilderParameters) param;
  }

  /**
   * The values of an applied chromatogram builder. {@link ChromatogramBuilderAlgorithms#FAST_AUTO}
   * stores the values it determined in its hidden parameters of the applied method,
   * {@link ChromatogramBuilderAlgorithms#FAST} its estimated m/z tolerance.
   *
   * @param parameters the parameters of an applied method
   * @return the settings or empty if a value is missing, e.g., an auto value of parameters that
   * were not applied
   */
  @NotNull
  public static Optional<ChromatogramBuilderSettings> getAppliedSettings(
      @NotNull ParameterSet parameters) {
    final ModuleOptionsEnumComboParameter<ChromatogramBuilderAlgorithms> selected = parameters.tryGetParameter(
        algorithm).orElse(null);
    if (selected == null || selected.getValue() == null) {
      return Optional.empty();
    }
    if (selected.getValue() == ChromatogramBuilderAlgorithms.FAST_AUTO) {
      return FastAutoChromatogramBuilderParameters.getDeterminedSettings(
          selected.getEmbeddedParameters(ChromatogramBuilderAlgorithms.FAST_AUTO));
    }
    final Integer minConsecutive;
    final Double minGroup;
    final Double minHeight;
    final MZTolerance tolerance;
    if (selected.getValue() == ChromatogramBuilderAlgorithms.FAST) {
      final ParameterSet fast = selected.getEmbeddedParameters(ChromatogramBuilderAlgorithms.FAST);
      minConsecutive = fast.getValue(FastChromatogramBuilderParameters.minimumConsecutiveScans);
      minGroup = fast.getValue(FastChromatogramBuilderParameters.minGroupIntensity);
      minHeight = fast.getValue(FastChromatogramBuilderParameters.minHighestPoint);
      final MZToleranceOrAuto fastTolerance = fast.getValue(
          FastChromatogramBuilderParameters.mzTolerance);
      tolerance = fastTolerance == null ? null : fastTolerance.tolerance();
    } else {
      final ParameterSet legacy = selected.getEmbeddedParameters(
          ChromatogramBuilderAlgorithms.LEGACY_ADAP);
      minConsecutive = legacy.getValue(
          LegacyAdapChromatogramBuilderParameters.minimumConsecutiveScans);
      minGroup = legacy.getValue(LegacyAdapChromatogramBuilderParameters.minGroupIntensity);
      minHeight = legacy.getValue(LegacyAdapChromatogramBuilderParameters.minHighestPoint);
      tolerance = legacy.getValue(LegacyAdapChromatogramBuilderParameters.mzTolerance);
    }
    if (minConsecutive == null || minGroup == null || minHeight == null || tolerance == null) {
      return Optional.empty();
    }
    return Optional.of(
        new ChromatogramBuilderSettings(minConsecutive, minGroup, minHeight, tolerance));
  }

  /**
   * @param appliedMethods all applied methods, newest last
   * @return the settings of the latest chromatogram builder, see
   * {@link #getAppliedSettings(ParameterSet)}
   */
  @NotNull
  public static Optional<ChromatogramBuilderSettings> getAppliedSettings(
      @NotNull List<FeatureListAppliedMethod> appliedMethods) {
    for (int i = appliedMethods.size() - 1; i >= 0; i--) {
      final ParameterSet parameters = appliedMethods.get(i).getParameters();
      if (parameters instanceof ADAPChromatogramBuilderParameters) {
        return getAppliedSettings(parameters);
      }
    }
    return Optional.empty();
  }
}
