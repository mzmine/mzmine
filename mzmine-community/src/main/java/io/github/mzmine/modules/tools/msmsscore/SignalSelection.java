/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.tools.msmsscore;

/**
 * Defines what signals should be matched
 */
public enum SignalSelection {
  NEUTRAL_LOSSES, MZ_SIGNALS;

  /**
   * throws RuntimeException exception if NEUTRAL_LOSS is selected and charge is >0 or if MZ_SIGNALS
   * is selected and charge is 0
   *
   * @param charge charge of a formula or similar
   */
  public void matchesChargeStateOrElseThrow(final int charge) {
    if (this == SignalSelection.MZ_SIGNALS && charge == 0) {
      throw new IllegalStateException(
          "Cannot use SignalSelection.NEUTRAL_LOSS with charge state=" + charge);
    }
    if (this == SignalSelection.NEUTRAL_LOSSES && Math.abs(charge) > 0) {
      throw new IllegalStateException(
          "Cannot use SignalSelection.NEUTRAL_LOSS with charge state>0: " + charge);
    }
  }
}
