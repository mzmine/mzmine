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

package io.github.mzmine.modules.dataprocessing.filter_isotopefinder.benchmark;

import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.impl.SimpleMassSpectrum;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.openscience.cdk.Element;

/**
 * A single benchmark case ready to feed to the isotope finder engine, rebuilt from a
 * {@link BenchmarkPattern}. Carries the input spectrum, the engine configuration (allowed elements,
 * polarity, max charge, tolerance) and the ground truth used to score a detection.
 *
 * @param id                  stable unique id (from the pattern)
 * @param axis                dominant stressor label
 * @param spectrum            the input spectrum
 * @param seedMz              base-peak m/z of the spectrum (the finder seed)
 * @param seedHeight          base-peak intensity of the spectrum
 * @param elements            allowed elements for the engine
 * @param polarity            polarity of the pattern
 * @param maxCharge           maximum charge to search (per molecule class)
 * @param tol                 m/z tolerance
 * @param trueCharge          the true charge state
 * @param trueMonoMz          m/z of the monoisotopic ion
 * @param trueOffsetsMz       true isotope peaks present in the spectrum
 * @param borderlineOffsetsMz weak true peaks (below 5% of the base peak)
 * @param falseOffsetsMz      injected false peaks (noise + interference)
 * @param trueHeavyElements   heavy elements present (formula minus C and H)
 * @param seed                stable base seed
 */
public record GroundTruthCase(@NotNull String id, @NotNull String axis,
                              @NotNull SimpleMassSpectrum spectrum, double seedMz,
                              double seedHeight, @NotNull List<Element> elements,
                              @NotNull PolarityType polarity, int maxCharge,
                              @NotNull MZTolerance tol, int trueCharge, double trueMonoMz,
                              @NotNull double[] trueOffsetsMz,
                              @NotNull double[] borderlineOffsetsMz,
                              @NotNull double[] falseOffsetsMz,
                              @NotNull Set<String> trueHeavyElements, long seed) {

  /**
   * Default m/z tolerance for the benchmark cases (high-resolution data).
   */
  @NotNull
  public static MZTolerance defaultTolerance() {
    return new MZTolerance(0.005, 10);
  }

  /**
   * Wide m/z tolerance for the unit-resolution axis: low-resolution instruments report peaks on a
   * coarse, low-accuracy m/z axis (~0.1-0.3 Da error), so the finder is run with a matching
   * absolute tolerance instead of the high-resolution default.
   */
  @NotNull
  public static MZTolerance unitResolutionTolerance() {
    return new MZTolerance(0.2, 0);
  }

  /**
   * The tolerance the finder should run with for a given axis: the wide unit-resolution tolerance
   * on the {@link GenerationConfig#UNIT_RESOLUTION_AXIS} axis, the high-resolution default
   * otherwise.
   */
  @NotNull
  public static MZTolerance toleranceForAxis(@NotNull final String axis) {
    return GenerationConfig.UNIT_RESOLUTION_AXIS.equals(axis) ? unitResolutionTolerance()
        : defaultTolerance();
  }

  /**
   * Rebuild a runnable case from a serialized {@link BenchmarkPattern}: reconstruct the spectrum,
   * map element symbols to CDK {@link Element}s, and derive the seed (base peak).
   */
  @NotNull
  public static GroundTruthCase fromPattern(@NotNull final BenchmarkPattern p) {
    final SimpleMassSpectrum spectrum = new SimpleMassSpectrum(p.mz(), p.intensity());

    final List<Element> elements = new ArrayList<>(p.elements().length);
    for (final String symbol : p.elements()) {
      elements.add(new Element(symbol));
    }

    final Integer baseIdx = spectrum.getBasePeakIndex();
    final double seedMz =
        baseIdx != null ? spectrum.getMzValue(baseIdx) : (p.mz().length > 0 ? p.mz()[0] : 0d);
    final double seedHeight = baseIdx != null ? spectrum.getIntensityValue(baseIdx) : 0d;

    final PolarityType polarity = PolarityType.valueOf(p.polarity());
    final MoleculeClass cls = MoleculeClass.valueOf(p.moleculeClass());
    final Set<String> heavy = new LinkedHashSet<>(List.of(p.trueHeavyElements()));

    return new GroundTruthCase(p.id(), p.axis(), spectrum, seedMz, seedHeight, elements, polarity,
        cls.maxCharge(), toleranceForAxis(p.axis()), p.trueCharge(), p.trueMonoMz(),
        p.trueOffsetsMz(), p.borderlineOffsetsMz(), p.falseOffsetsMz(), heavy, p.seed());
  }
}
