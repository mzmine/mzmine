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

package io.github.mzmine.modules.dataprocessing.filter_isotopefinder.engine;

import org.jetbrains.annotations.NotNull;

/**
 * Per-signal attribution of a kept isotope-pattern data point, produced only when the engine runs
 * with {@code keepDiagnostics}. Lets the compound dashboard label each detected peak (m/z + likely
 * cause, e.g. {@code +1 13C} or {@code Cl}).
 *
 * @param mz             observed m/z of the signal.
 * @param intensity      observed (absolute) intensity of the signal.
 * @param offsetFromMono integer isotope offset relative to the (predicted) monoisotopic peak: 0 =
 *                       monoisotopic, 1 = M+1, ... May be negative when the observed base is above
 *                       the monoisotopic.
 * @param assignment     how the signal was attributed.
 * @param label          short human-readable label of the likely cause (e.g. {@code M},
 *                       {@code +1 13C}, {@code Cl}); never null (falls back to {@code heavy}).
 * @param relIntensity   intensity relative to the observed base peak (0..1+).
 */
public record IsotopeSignalAttribution(double mz, double intensity, int offsetFromMono,
                                       @NotNull IsotopeAssignment assignment, @NotNull String label,
                                       double relIntensity) {

}
