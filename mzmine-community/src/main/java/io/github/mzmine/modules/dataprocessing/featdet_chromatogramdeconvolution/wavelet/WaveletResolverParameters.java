/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.wavelet;

import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.FeatureResolverSetupDialog;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.GeneralResolverParameters;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.Resolver;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.ResolvingDimension;
import io.github.mzmine.modules.dataprocessing.filter_groupms2.GroupMS2SubParameters;
import io.github.mzmine.modules.presets.ModulePreset;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.parametertypes.AdvancedParametersParameter;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter.OriginalFeatureListOption;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelectionType;
import io.github.mzmine.util.ExitCode;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WaveletResolverParameters extends GeneralResolverParameters {

  public static final boolean DEFAULT_DIP_FILTER = true;

  public static final DoubleParameter snr = new DoubleParameter("Signal to noise threshold",
      "Minimum required signal to noise ratio of a peak. ", new DecimalFormat("#.#"), 5d, 1d,
      Double.MAX_VALUE);

  public static final OptionalParameter<DoubleParameter> topToEdge = new OptionalParameter<>(
      new DoubleParameter("Top to edge (SNR override)",
          "If a potential feature does not match the SNR threshold, check if the top/edge ratio is above this threshold.",
          new DecimalFormat("#.#"), 3d, 1d, Double.MAX_VALUE), false);

  public static final DoubleParameter minHeight = new DoubleParameter("Minimum height",
      "Minimum absolute required of a signal to be retained as a feature.",
      ConfigService.getGuiFormats().intensityFormat(), 1E3, 0d, Double.MAX_VALUE);

  public static final ComboParameter<NoiseCalculation> noiseCalculation = new ComboParameter<>(
      "Noise calculation",
      "Choose a method to calculate the noise around a potential signal. Default: %s".formatted(
          NoiseCalculation.STANDARD_DEVIATION.toString()), NoiseCalculation.values(),
      NoiseCalculation.STANDARD_DEVIATION);

  public static final BooleanParameter dipFilter = new BooleanParameter("Dip filter", """
      Filters V-shaped dips in the baseline (--v--) that are caused by instable sprays and may behave like an unresolved double peak.
      Default: enable for LC-MS, disable for GC-EI-MS""", DEFAULT_DIP_FILTER);

  public static final AdvancedParametersParameter<AdvancedWaveletParameters> advancedParameters = new AdvancedParametersParameter<>(
      new AdvancedWaveletParameters());

  public WaveletResolverParameters() {
    super(
        "https://mzmine.github.io/mzmine_documentation/module_docs/featdet_resolver_wavelet/wavelet_resolver.html",
        GeneralResolverParameters.PEAK_LISTS, GeneralResolverParameters.dimension,
        GeneralResolverParameters.groupMS2Parameters, snr, topToEdge, minHeight, noiseCalculation,
        dipFilter,

        GeneralResolverParameters.MIN_NUMBER_OF_DATAPOINTS, GeneralResolverParameters.SUFFIX,
        GeneralResolverParameters.handleOriginal,

        advancedParameters);
  }

  @Override
  public @Nullable Resolver getResolver(ParameterSet parameterSet, ModularFeatureList flist) {
    return new WaveletResolver(flist, parameterSet);
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    final FeatureResolverSetupDialog dialog = new FeatureResolverSetupDialog(valueCheckRequired,
        this, null);
    dialog.showAndWait();
    return dialog.getExitCode();
  }

  @Override
  public @NotNull List<ModulePreset> createDefaultPresets() {
    final String moduleId = MZmineCore.getModuleInstance(WaveletResolverModule.class).getUniqueID();
    return List.of(
        //
        new ModulePreset("AnyLC_Orbitrap", moduleId,
            create(new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS),
                ResolvingDimension.RETENTION_TIME, true, GroupMS2SubParameters.createDefault(), 5,
                "r", OriginalFeatureListOption.KEEP, 8, null, 1E5,
                NoiseCalculation.STANDARD_DEVIATION, true, false,
                AdvancedWaveletParameters.createLcDefault())),
        //
        new ModulePreset("AnyLC_TOF", moduleId,
            create(new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS),
                ResolvingDimension.RETENTION_TIME, true, GroupMS2SubParameters.createDefault(), 5,
                "r", OriginalFeatureListOption.KEEP, 8, null, 3E3,
                NoiseCalculation.STANDARD_DEVIATION, true, false,
                AdvancedWaveletParameters.createLcDefault())),
        //
        new ModulePreset("GC_EI_TOF", moduleId,
            create(new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS),
                ResolvingDimension.RETENTION_TIME, false, GroupMS2SubParameters.createDefault(), 8,
                "r", OriginalFeatureListOption.KEEP, 5, null, 3E3,
                NoiseCalculation.STANDARD_DEVIATION, false, false,
                AdvancedWaveletParameters.createGcDefault())),
        //
        new ModulePreset("GC_EI_Orbitrap", moduleId,
            create(new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS),
                ResolvingDimension.RETENTION_TIME, false, GroupMS2SubParameters.createDefault(), 8,
                "r", OriginalFeatureListOption.KEEP, 5, null, 1E5,
                NoiseCalculation.STANDARD_DEVIATION, false, false,
                AdvancedWaveletParameters.createGcDefault()))
        );
  }

  public static WaveletResolverParameters create(FeatureListsSelection flists,
      @NotNull ResolvingDimension dimension, boolean enableMs2,
      @NotNull GroupMS2SubParameters groupMs2, int minDp, @Nullable String suffix,
      @NotNull OriginalFeatureListOption handleOriginal, double snr, @Nullable Double topToEdge,
      double minHeight, @NotNull NoiseCalculation noiseCalculation, @Nullable Boolean dipFilter,
      boolean advancedEnabled, @NotNull AdvancedWaveletParameters advancedParam) {

    final ParameterSet param = new WaveletResolverParameters().cloneParameterSet();

    param.setParameter(WaveletResolverParameters.PEAK_LISTS, flists);
    param.setParameter(WaveletResolverParameters.dimension, dimension);
    param.setParameter(WaveletResolverParameters.groupMS2Parameters, enableMs2);
    param.getParameter(WaveletResolverParameters.groupMS2Parameters)
        .setEmbeddedParameters(groupMs2);
    param.setParameter(WaveletResolverParameters.MIN_NUMBER_OF_DATAPOINTS, minDp);
    param.setParameter(WaveletResolverParameters.SUFFIX, Objects.requireNonNullElse(suffix, "r"));
    param.setParameter(WaveletResolverParameters.handleOriginal, handleOriginal);
    param.setParameter(WaveletResolverParameters.snr, snr);
    param.setParameter(WaveletResolverParameters.topToEdge, topToEdge != null,
        Objects.requireNonNullElse(topToEdge, 3d));
    param.setParameter(WaveletResolverParameters.minHeight, minHeight);
    param.setParameter(WaveletResolverParameters.noiseCalculation, noiseCalculation);
    param.setParameter(WaveletResolverParameters.dipFilter,
        Objects.requireNonNullElse(dipFilter, DEFAULT_DIP_FILTER));
    param.setParameter(WaveletResolverParameters.advancedParameters, advancedEnabled);
    param.getParameter(WaveletResolverParameters.advancedParameters)
        .setEmbeddedParameters(advancedParam);

    return (WaveletResolverParameters) param;
  }

  public enum NoiseCalculation implements UniqueIdSupplier {
    STANDARD_DEVIATION, MEDIAN_ABSOLUTE_DEVIATION;

    @Override
    public @NotNull String getUniqueID() {
      return switch (this) {
        case STANDARD_DEVIATION -> "standard_deviation";
        case MEDIAN_ABSOLUTE_DEVIATION -> "median_absolute_deviation";
      };
    }

    @Override
    public String toString() {
      return switch (this) {
        case STANDARD_DEVIATION -> "Standard Deviation";
        case MEDIAN_ABSOLUTE_DEVIATION -> "Median absolute deviation";
      };
    }
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }
}
