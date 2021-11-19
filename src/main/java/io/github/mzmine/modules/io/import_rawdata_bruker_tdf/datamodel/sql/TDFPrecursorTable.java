/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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

  private final TDFDataColumn<Long> precursorIdColumn;
  private final TDFDataColumn<Double> largestPeakMzColumn;
  private final TDFDataColumn<Double> averageMzColumn;
  private final TDFDataColumn<Double> monoisotopicMzColumn;
  private final TDFDataColumn<Long> chargeColumn;
  private final TDFDataColumn<Double> scanNumberColumn;
  private final TDFDataColumn<Double> intensityColumn;
  private final TDFDataColumn<Long> frameIdColumn;

  public TDFPrecursorTable() {
    super(PRECURSOR_TABLE_NAME, PRECURSOR_ID);

    precursorIdColumn = (TDFDataColumn<Long>) getColumn(PRECURSOR_ID);
    largestPeakMzColumn = new TDFDataColumn<>(LARGEST_PEAK_MZ);
    averageMzColumn = new TDFDataColumn<>(AVERAGE_MZ);
    monoisotopicMzColumn = new TDFDataColumn<>(MONOISOTOPIC_MZ);
    chargeColumn = new TDFDataColumn<>(CHARGE);
    scanNumberColumn = new TDFDataColumn<>(SCAN_NUMBER);
    intensityColumn = new TDFDataColumn<>(INTENSITY);
    frameIdColumn = new TDFDataColumn<>(PARENT_ID);

    columns.addAll(Arrays.asList(largestPeakMzColumn,
        averageMzColumn,
        monoisotopicMzColumn,
        chargeColumn,
        scanNumberColumn,
        intensityColumn,
        frameIdColumn));
  }

  public Set<Long> getPrecursorIdsForMS1Frame(long frame) {
    HashSet<Long> precursorIds = new HashSet<>();
    for (int index = 0; index < precursorIdColumn.size(); index++) {
      if (frameIdColumn.get(index) == frame) {
        precursorIds.add(precursorIdColumn.get(index));
      } else if (frameIdColumn.get(index) > frame) {
        break;
      }
    }
    return precursorIds;
  }

  @Override
  public boolean isValid() {
    return true;
  }

}
