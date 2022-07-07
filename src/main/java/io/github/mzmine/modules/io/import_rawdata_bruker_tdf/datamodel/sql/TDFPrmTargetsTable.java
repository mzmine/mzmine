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
