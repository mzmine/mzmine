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

import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.wavelet.SnrResult.Fallback;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.wavelet.SnrResult.Passed;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.wavelet.SnrResult.Surrounded;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.wavelet.SnrResult.Undetermined;

sealed interface SnrResult permits Passed, Fallback, Surrounded, Undetermined {

  Undetermined undetermined = new Undetermined();
  Surrounded surrounded = new Surrounded();

  static Undetermined undetermined() {
    return undetermined;
  }

  static Surrounded surrounded() {
    return surrounded;
  }

  static Passed passed(double snr) {
    return new Passed(snr);
  }

  static Fallback fallback(double snr) {
    return new Fallback(snr);
  }

  record Passed(double snr) implements SnrResult {

  }

  record Fallback(double snr) implements SnrResult {

  }

  record Surrounded() implements SnrResult {

  }

  record Undetermined() implements SnrResult {

  }
}
