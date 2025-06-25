/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.group_spectral_networking.modified_cosine;


import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.filter_scan_merge_select.SpectraMergeSelectParameter;
import io.github.mzmine.modules.dataprocessing.filter_scan_merge_select.options.SpectraMergeSelectPresets;
import io.github.mzmine.modules.dataprocessing.group_spectral_networking.SignalFiltersParameters;
import io.github.mzmine.modules.dataprocessing.group_spectral_networking.SpectralSignalFilter;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.submodules.ParameterSetParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * MS/MS similarity check based on difference and signal comparison
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class ModifiedCosineSpectralNetworkingParameters extends SimpleParameterSet {

  public static final SpectraMergeSelectParameter spectraMergeSelect = SpectraMergeSelectParameter.createMolecularNetworkingDefault();

  // MZ-tolerance: deisotoping, adducts
  public static final MZToleranceParameter MZ_TOLERANCE = new MZToleranceParameter("m/z tolerance",
      "Tolerance value of the m/z difference between MS2 signals (add absolute tolerance to cover small neutral losses (5 ppm on m=18 is insufficient))",
      0.003, 10);

  public static final DoubleParameter MIN_COSINE_SIMILARITY = new DoubleParameter(
      "Min cosine similarity", "Minimum spectral cosine similarity (scaled 0-1). Default is 0.7",
      MZmineCore.getConfiguration().getScoreFormat(), 0.7, 0d, 1d);

  public static final IntegerParameter MIN_MATCH = new IntegerParameter("Minimum matched signals",
      "Minimum matched signals or neutral losses (m/z differences). Default is 4 for small molecules but the higher the more confident.",
      4);

  public static final OptionalParameter<DoubleParameter> MAX_MZ_DELTA = new OptionalParameter<>(
      new DoubleParameter("Max precursor m/z delta",
          "Maximum allowed m/z delta between precursor ions to be tested. This can speed up the process",
          MZmineCore.getConfiguration().getMZFormat(), 600d), true);

  public static final ParameterSetParameter<SignalFiltersParameters> signalFilters = new ParameterSetParameter<>(
      SignalFiltersParameters.NAME, SignalFiltersParameters.DESCRIPTION,
      new SignalFiltersParameters());


  // legacy parameters that were replaced as private final
  private final BooleanParameter ONLY_BEST_MS2_SCAN = new BooleanParameter("Only best MS2 scan",
      "Compares only the best MS2 scan (or all MS2 scans)", true);

  public ModifiedCosineSpectralNetworkingParameters() {
    super(
        "https://mzmine.github.io/mzmine_documentation/module_docs/group_spectral_net/molecular_networking.html",
        MZ_TOLERANCE, spectraMergeSelect, MAX_MZ_DELTA, MIN_MATCH, MIN_COSINE_SIMILARITY,
        signalFilters);
  }


  public static void setAll(final ParameterSet param, final MZTolerance mergingTolerance,
      final boolean removePrecursor, final double maxMzDelta, final int minMatched,
      final double minCosine, final MZTolerance mzTol, final SpectralSignalFilter filter) {
    param.setParameter(MAX_MZ_DELTA, removePrecursor, maxMzDelta);
    param.getParameter(spectraMergeSelect)
        .setSimplePreset(SpectraMergeSelectPresets.SINGLE_MERGED_SCAN, mergingTolerance);
    param.setParameter(MIN_MATCH, minMatched);
    param.setParameter(MIN_COSINE_SIMILARITY, minCosine);
    param.setParameter(MZ_TOLERANCE, mzTol);
    param.getParameter(signalFilters).getEmbeddedParameters().setValue(filter);
  }

  @Override
  public Map<String, Parameter<?>> getNameParameterMap() {
    var map = super.getNameParameterMap();
    map.put(ONLY_BEST_MS2_SCAN.getName(), ONLY_BEST_MS2_SCAN);
    return map;
  }

  @Override
  public void handleLoadedParameters(final Map<String, Parameter<?>> loadedParams) {
    MZTolerance mzTol = MZTolerance.FIFTEEN_PPM_OR_FIVE_MDA;
    if (loadedParams.containsKey(MZ_TOLERANCE.getName())) {
      mzTol = getValue(MZ_TOLERANCE);
    }
    if (loadedParams.containsKey(ONLY_BEST_MS2_SCAN.getName())) {
      boolean onlyBest = ONLY_BEST_MS2_SCAN.getValue();

      var preset = onlyBest ? SpectraMergeSelectPresets.SINGLE_MERGED_SCAN
          : SpectraMergeSelectPresets.REPRESENTATIVE_SCANS;
      getParameter(spectraMergeSelect).setSimplePreset(preset, mzTol);
    }
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }

  @Override
  public @Nullable String getVersionMessage(final int version) {
    return switch (version) {
      case 3 -> """
          From mzmine version > 4.4.3 the scan selection and merging has been harmonized across modules.
          Please check and configure the %s parameter.""".formatted(spectraMergeSelect.getName());
      default -> null;
    };
  }

  @Override
  public int getVersion() {
    return 3;
  }
}
