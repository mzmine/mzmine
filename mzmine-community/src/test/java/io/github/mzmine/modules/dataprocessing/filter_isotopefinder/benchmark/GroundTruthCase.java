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
 * A single benchmark case ready to feed to the engine, rebuilt from a {@link BenchmarkPattern}: the
 * input spectrum, the engine configuration and the ground truth used to score a detection.
 *
 * @param seedMz              base-peak m/z, i.e. the finder seed
 * @param maxCharge           maximum charge to search, per molecule class
 * @param borderlineOffsetsMz weak true peaks, below 5% of the base peak
 * @param falseOffsetsMz      injected false peaks (noise + interference)
 * @param trueHeavyElements   heavy elements present, i.e. the formula minus C and H
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
   * Default tolerance: high-resolution data.
   */
  @NotNull
  public static MZTolerance defaultTolerance() {
    return new MZTolerance(0.005, 10);
  }

  /**
   * Unit-resolution instruments report peaks on a coarse m/z axis (~0.1-0.3 Da error), so the
   * finder must run with a matching absolute tolerance rather than the high-resolution default.
   */
  @NotNull
  public static MZTolerance unitResolutionTolerance() {
    return new MZTolerance(0.2, 0);
  }

  @NotNull
  public static MZTolerance toleranceForAxis(@NotNull final String axis) {
    return GenerationConfig.UNIT_RESOLUTION_AXIS.equals(axis) ? unitResolutionTolerance()
        : defaultTolerance();
  }

  /**
   * Rebuild a runnable case: reconstruct the spectrum, map symbols to CDK {@link Element}s and
   * derive the seed from the base peak.
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
