/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.util.scans;

import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.util.scans.merging.FloatGrouping;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * ID to group scans by fragmentation energy and by MSn precursor mzs
 *
 * @param fragmentationEnergy fragmentation energy of this scan: last fragmentation energy in case
 *                            of MSn. Merged scans may be from multiple energies and this will be
 *                            reflected as {@link FloatGrouping.Type#MULTIPLE_VALUES}
 * @param msnPrecursorMzs     list of MSn precursor mz isolation steps.
 */
public record ScanPrecursorEnergyGroupId(@NotNull FloatGrouping fragmentationEnergy,
                                         @NotNull List<Double> msnPrecursorMzs) {

  public ScanPrecursorEnergyGroupId(@NotNull MassSpectrum scan) {
    var energy = FloatGrouping.of(ScanUtils.extractCollisionEnergies(scan));
    var msnPrecursorMzs = ScanUtils.getMsnPrecursorMzs(scan);

    this(energy, msnPrecursorMzs.isEmpty() ? List.of() : msnPrecursorMzs.getFirst());
  }

}
