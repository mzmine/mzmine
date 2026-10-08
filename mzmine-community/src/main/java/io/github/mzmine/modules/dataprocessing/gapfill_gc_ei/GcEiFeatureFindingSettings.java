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

package io.github.mzmine.modules.dataprocessing.gapfill_gc_ei;

import static java.util.Objects.requireNonNullElse;

import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.ADAPChromatogramBuilderParameters;
import io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.ModularADAPChromatogramBuilderModule;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.FeatureResolverModule;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.GeneralResolverParameters;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.ResolvingDimension;
import io.github.mzmine.modules.dataprocessing.featdet_smoothing.SmoothingModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The feature detection steps that originally created the features of a feature list, extracted
 * from its applied methods. Used to re-detect features with exactly the same settings during GC-EI
 * gap filling.
 *
 * @param chromatogramParameters parameters of the ADAP chromatogram builder
 * @param smoothingParameters    parameters of the smoothing step that was applied between
 *                               chromatogram building and resolving, or null if there was none
 * @param resolverParameters     parameters of the feature resolver
 */
public record GcEiFeatureFindingSettings(@NotNull ParameterSet chromatogramParameters,
                                         @Nullable ParameterSet smoothingParameters,
                                         @NotNull GeneralResolverParameters resolverParameters) {

  /**
   * Extracts the latest chromatogram builder call, the latest resolver call after it, and the
   * latest smoothing call in between.
   *
   * @param methods applied methods of a feature list, oldest first
   * @throws IllegalStateException if the chromatogram builder or resolver step is missing
   */
  public static @NotNull GcEiFeatureFindingSettings fromAppliedMethods(
      @NotNull final List<FeatureListAppliedMethod> methods) {
    final int chromIndex = lastIndexOf(methods, ModularADAPChromatogramBuilderModule.class, 0,
        methods.size());
    if (chromIndex < 0) {
      throw new IllegalStateException("""
          GC-EI gap filling re-runs the chromatogram building and resolving of the original processing, \
          but no "Chromatogram builder" step was found in the processing history of the feature list.""");
    }

    final int resolverIndex = lastIndexOf(methods, FeatureResolverModule.class, chromIndex + 1,
        methods.size());
    if (resolverIndex < 0 || !(methods.get(
        resolverIndex).getParameters() instanceof GeneralResolverParameters resolverParameters)) {
      throw new IllegalStateException("""
          GC-EI gap filling re-runs the chromatogram building and resolving of the original processing, \
          but no feature resolver step was found after the "Chromatogram builder" step in the processing history of the feature list.""");
    }
    if (resolverParameters.getValue(GeneralResolverParameters.dimension)
        != ResolvingDimension.RETENTION_TIME) {
      throw new IllegalStateException(
          "GC-EI gap filling requires a feature resolver in the retention time dimension.");
    }

    // decision: only smoothing between chromatogram building and resolving is replicated, as the
    // resolver was tuned on smoothed data. Smoothing after resolving is not part of feature detection.
    final int smoothingIndex = lastIndexOf(methods, SmoothingModule.class, chromIndex + 1,
        resolverIndex);
    final ParameterSet smoothingParameters =
        smoothingIndex >= 0 ? methods.get(smoothingIndex).getParameters() : null;

    return new GcEiFeatureFindingSettings(methods.get(chromIndex).getParameters(),
        smoothingParameters, resolverParameters);
  }

  /**
   * @return index of the last method of the module class in [from, toExclusive) or -1
   */
  private static int lastIndexOf(@NotNull final List<FeatureListAppliedMethod> methods,
      @NotNull final Class<? extends MZmineModule> module, final int from, final int toExclusive) {
    for (int i = toExclusive - 1; i >= from; i--) {
      final FeatureListAppliedMethod method = methods.get(i);
      if (method != null && module.isInstance(method.getModule())) {
        return i;
      }
    }
    return -1;
  }

  public @NotNull MZTolerance mzTolerance() {
    return chromatogramParameters.getValue(ADAPChromatogramBuilderParameters.mzTolerance);
  }

  public @NotNull ScanSelection scanSelection() {
    return chromatogramParameters.getValue(ADAPChromatogramBuilderParameters.scanSelection);
  }

  public int minConsecutiveScans() {
    return chromatogramParameters.getValue(
        ADAPChromatogramBuilderParameters.minimumConsecutiveScans);
  }

  public double minGroupIntensity() {
    return requireNonNullElse(
        chromatogramParameters.getValue(ADAPChromatogramBuilderParameters.minGroupIntensity), 0d);
  }

  public double minHighestPoint() {
    return requireNonNullElse(
        chromatogramParameters.getValue(ADAPChromatogramBuilderParameters.minHighestPoint), 0d);
  }
}
