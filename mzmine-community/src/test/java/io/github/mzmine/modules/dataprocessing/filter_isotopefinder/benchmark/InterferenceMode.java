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

/**
 * The kind of co-eluting interferent a sweep variant injects into the target spectrum.
 */
public enum InterferenceMode {

  /**
   * No interferent.
   */
  NONE,

  /**
   * <b>Realistic co-elution.</b> A DIFFERENT compound from the catalog, at a seeded non-harmonic m/z
   * offset and at a seeded fraction of the target's intensity. This is what an ordinary chimeric MS1
   * window looks like.
   * <p>
   * The offset is deliberately drawn away from exactly half the isotope spacing. A decoy placed
   * there would be a copy of the target's own comb interleaved with it, which synthesises a
   * near-perfect {@code 2z} ladder - the theoretical maximum of harmonic confusion, not something a
   * real spectrum produces. An adversarial mode doing exactly that was removed from the benchmark
   * because its accuracy numbers were routinely misread as a real-world failure rate.
   */
  REALISTIC
}
