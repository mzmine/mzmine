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

/*
 * This module was prepared by Abi Sarvepalli, Christopher Jensen, and Zheng Zhang at the Dorrestein
 * Lab (University of California, San Diego).
 *
 * It is freely available under the GNU GPL licence of MZmine2.
 *
 * For any questions or concerns, please refer to:
 * https://groups.google.com/forum/#!forum/molecular_networking_bug_reports
 *
 * Credit to the Du-Lab development team for the initial commitment to the MGF export module.
 */

package io.github.mzmine.modules.io.spectraldbsubmit.formats;

/**
 * Specific values for GNPS entries (fields that only allow these values)
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class GnpsValues {

  public enum CompoundSource {
    Lysate, Isolated, Commercial, Crude, Other
  }

  public enum Polarity {
    Positive, Negative
  }

  public enum Instrument {
    qTof, QQQ, Ion_Trap("Ion Trap"), Hybrid_FT("Hybrid FT"), Orbitrap, ToF, ICR;

    private final String value;

    Instrument() {
      this.value = null;
    }

    Instrument(String value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return value != null ? value : super.toString();
    }
  }

  public enum IonSource {
    LC_ESI("LC-ESI"), DI_ESI("DI-ESI"), EI, APCI, ESI, MALDI, LDI, DESI;

    private final String value;

    IonSource() {
      this.value = null;
    }

    IonSource(String value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return value != null ? value : super.toString();
    }
  }
}
