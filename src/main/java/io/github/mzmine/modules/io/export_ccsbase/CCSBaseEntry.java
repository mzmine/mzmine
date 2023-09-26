/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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

package io.github.mzmine.modules.io.export_ccsbase;

import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.main.MZmineCore;
import java.util.Objects;

/**
 * Wrapper to store CCSBase entries for exports. Only uses the highest signal per adduct and
 * smiles.
 */
record CCSBaseEntry(String name, String adduct, double neutralMass, int charge, double mz,
                    double ccs, String smiles, String chemClassLabel, String mobilityType,
                    String ccsCalibrationMethod, double maxIntensity) {

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof CCSBaseEntry that)) {
      return false;
    }
    return Objects.equals(adduct, that.adduct) && Objects.equals(smiles, that.smiles);
  }

  @Override
  public int hashCode() {
    return Objects.hash(adduct, smiles);
  }

  public String toCsvLine() {
    final NumberFormats form = MZmineCore.getConfiguration().getExportFormats();

    StringBuilder line = new StringBuilder();
    line.append(name).append(",");
    line.append(adduct).append(",");
    line.append(form.mz(neutralMass)).append(",");
    line.append(charge).append(",");
    line.append(form.mz(mz)).append(",");
    line.append(form.ccs(ccs)).append(",");
    line.append(smiles).append(",");
    line.append(chemClassLabel).append(",");
    line.append(mobilityType).append(",");
    line.append(ccsCalibrationMethod).append(",");

    final String result = line.toString();
    if (result.toLowerCase().contains("null") || result.toLowerCase().contains(",,")) {
      return null;
    }

    return result;
  }
}
