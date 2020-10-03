/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.io.tdfimport.datamodel.sql;

import java.util.Arrays;

/**
 * Table containing all precursors / features detected by an on-line precursor-selection algorithm.
 * <p>
 * 'Precursors' table lists only the information derived from MS^1 spectra. How a precursor is
 * actually measured (= quadrupole settings and scan-number begin/end) remains in the
 * 'PasefFrameMsMsInfo' table.
 */
public class TDFPrecursorTable extends TDFDataTable<Long> {

  public static final String PRECURSOR_TABLE_NAME = "Precursors";

  /**
   * Number that uniquely identifies this precursor in this analysis.
   */
  public static final String PRECURSOR_ID = "Id";

  /**
   * m/z of the largest (most intensive) peak in this precursor's isotope pattern.
   */
  public static final String LARGEST_PEAK_MZ = "LargestPeakMz";

  /**
   * Intensity-weighted average m/z of this precursor's isotope pattern. If only one peak was
   * detected, this will be the m/z of that peak, and identical to 'LargestPeakMz'.
   */
  public static final String AVERAGE_MZ = "AverageMz";

  /**
   * An estimate for the monoisotopic m/z derived from the isotope pattern of the precursor. May be
   * NULL when detection failed.
   */
  public static final String MONOISOTOPIC_MZ = "MonoisotopicMz";

  /**
   * The charge state of the precursor (a positive integer) as estimated from the isotope pattern of
   * the precursor. May be NULL when detection failed.
   */
  public static final String CHARGE = "Charge";

  /**
   * Mobility (in scan-number units) of this precursor in the corresponding MS^1 frame.
   */
  public static final String SCAN_NUMBER = "ScanNumber";

  /**
   * Intensity of this precursor in the corresponding MS^1 frame.
   */
  public static final String INTENSITY = "Intensity";

  /**
   * The corresponding MS^1 frame in which this precursor was detected. In the case that MS^1 frames
   * were repeatedly measured and averaged to improve SNR for precursor detection, the TDF stores
   * those frames individually, and this field points to the last of that set of frames. (Field can
   * be NULL, which means that the parent MS^1 is not included in the TDF; e.g., because recording
   * started in the middle of the DDA cycle, or due to an error writing the MS^1 data.)
   */
  public static final String PARENT_ID = "Parent";

  public TDFPrecursorTable() {
    super(PRECURSOR_TABLE_NAME, PRECURSOR_ID);

    columns.addAll(Arrays.asList(new TDFDataColumn<Double>(LARGEST_PEAK_MZ),
        new TDFDataColumn<Double>(AVERAGE_MZ),
        new TDFDataColumn<Double>(MONOISOTOPIC_MZ),
        new TDFDataColumn<Long>(CHARGE),
        new TDFDataColumn<Double>(SCAN_NUMBER),
        new TDFDataColumn<Double>(INTENSITY),
        new TDFDataColumn<Long>(PARENT_ID)
    ));
  }

  private static class X {
    long frame;
    long precursorId;

  }

  @Override
  public boolean isValid() {
    return true;
  }
}
