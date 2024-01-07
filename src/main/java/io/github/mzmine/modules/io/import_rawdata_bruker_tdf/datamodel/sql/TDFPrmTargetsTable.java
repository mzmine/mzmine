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

package io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql;

import java.util.Arrays;

/**
 * Rationale behind this table is to include some metadata for every target that might be useful for
 * generic processing software (i.e., processing software which does not know anything about the
 * software that set up the method / target list in the first place).
 */
public class TDFPrmTargetsTable extends TDFDataTable<Long> {

  public static final String TARGET_TABLE_NAME = "PrmTargets";

  /**
   * Number that uniquely identifies this precursor in this analysis.
   */
  public static final String TARGET_ID = "Id";

  public static final String EXTERNAL_ID = "ExternalId";

  /**
   * RT in seconds
   */
  public static final String TIME = "Time";

  public static final String ONE_OVER_K0 = "OneOverK0";

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

  public static final String DESCRIPTION = "Description";

  private final TDFDataColumn<Long> precursorIdColumn;
  private final TDFDataColumn<String> externalIdColumn;
  private final TDFDataColumn<Double> oneOverK0Column;
  private final TDFDataColumn<Double> monoisotopicMzColumn;
  private final TDFDataColumn<Long> chargeColumn;
  private final TDFDataColumn<String> descriptionColumn;

  public TDFPrmTargetsTable() {
    super(TARGET_TABLE_NAME, TARGET_ID);

    precursorIdColumn = (TDFDataColumn<Long>) getColumn(TDFPrmTargetsTable.TARGET_ID);
    externalIdColumn = new TDFDataColumn<>(TDFPrmTargetsTable.EXTERNAL_ID);
    monoisotopicMzColumn = new TDFDataColumn<>(TDFPrmTargetsTable.MONOISOTOPIC_MZ);
    chargeColumn = new TDFDataColumn<>(TDFPrmTargetsTable.CHARGE);
    oneOverK0Column = new TDFDataColumn<>(TDFPrmTargetsTable.ONE_OVER_K0);
    descriptionColumn = new TDFDataColumn<>(TDFPrmTargetsTable.DESCRIPTION);

    columns.addAll(
        Arrays.asList(externalIdColumn, monoisotopicMzColumn, chargeColumn, oneOverK0Column,
            descriptionColumn));
  }

}
