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
import org.jetbrains.annotations.Nullable;

/**
 * Diagnostic isotope data for one candidate heavy element used by {@link ElementAutoDetector}: the
 * M+2 defect and per-atom relative intensity (to the most-abundant isotope), plus the optional M+1
 * defect and per-atom relative intensity. Cl and Br have no natural M+1 isotope, so {@code m1Delta}
 * is {@code null} for them.
 *
 * @param symbol  element symbol
 * @param m2Delta mass difference (Da) of the M+2 isotope to the most-abundant isotope
 * @param m2Rel   abundance of the M+2 isotope relative to the most-abundant isotope
 * @param m1Delta mass difference (Da) of the M+1 isotope, or {@code null} if the element has none
 * @param m1Rel   abundance of the M+1 isotope relative to the most-abundant isotope (0 if none)
 */
record ElementIsotopes(@NotNull String symbol, double m2Delta, double m2Rel,
                       @Nullable Double m1Delta, double m1Rel) {

}
