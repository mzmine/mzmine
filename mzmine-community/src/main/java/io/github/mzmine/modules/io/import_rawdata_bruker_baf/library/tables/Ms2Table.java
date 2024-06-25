/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.io.import_rawdata_bruker_baf.library.tables;

import static io.github.mzmine.modules.io.import_rawdata_bruker_baf.library.tables.BafStepsTable.ISOLATION_TYPE_COL;
import static io.github.mzmine.modules.io.import_rawdata_bruker_baf.library.tables.BafStepsTable.MASS_COL;
import static io.github.mzmine.modules.io.import_rawdata_bruker_baf.library.tables.BafStepsTable.MS_LEVEL_COL;
import static io.github.mzmine.modules.io.import_rawdata_bruker_baf.library.tables.BafStepsTable.NUMBER_COL;
import static io.github.mzmine.modules.io.import_rawdata_bruker_baf.library.tables.BafStepsTable.REACTION_TYPE_COL;

import io.github.mzmine.datamodel.impl.DDAMsMsInfoImpl;
import io.github.mzmine.datamodel.msms.ActivationMethod;
import io.github.mzmine.datamodel.msms.MsMsInfo;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.TDFDataColumn;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.TDFDataTable;
import java.sql.Connection;
import java.util.Arrays;
import java.util.stream.Collectors;

public class Ms2Table extends TDFDataTable<Long> {

  public static final String NAME = "MS2 Table";
  public static final String TARGET_SPECTRUM_COL = "TargetSpectrum";

  private final TDFDataColumn<Long> targetSpectrumCol;
  private final TDFDataColumn<Long> numberCol = new TDFDataColumn<>(NUMBER_COL);
  private final TDFDataColumn<Long> isolationTypeCol = new TDFDataColumn<>(ISOLATION_TYPE_COL);
  private final TDFDataColumn<Long> reactionTypeCol = new TDFDataColumn<>(REACTION_TYPE_COL);
  private final TDFDataColumn<Long> msLevelCol = new TDFDataColumn<>(MS_LEVEL_COL);
  private final TDFDataColumn<Double> massCol = new TDFDataColumn<>(MASS_COL);

  /**
   * Merged in from the Variables table. Specifically added as last column, since it is created in
   * the query {@link this#getQueryText(String)}. Do not move
   */
  private final TDFDataColumn<Double> collisionEnergyColumn = new TDFDataColumn<>(
      "CollisionEnergy");
  private final TDFDataColumn<Double> isolationWidthColumn = new TDFDataColumn<>("IsolationWidth");


  public Ms2Table() {
    super(NAME, BafSpectraTable.ID_COL);

    targetSpectrumCol = (TDFDataColumn<Long>) getColumn(TARGET_SPECTRUM_COL);
    columns.addAll(Arrays.asList(numberCol, isolationTypeCol, reactionTypeCol, msLevelCol, massCol,
        collisionEnergyColumn, isolationWidthColumn));
  }

  public ActivationMethod getActivationMethod(int index) {
    return switch (reactionTypeCol.get(index).intValue()) {
      case 0x00000001 -> ActivationMethod.CID;
      default -> null;
    };
  }

  public MsMsInfo getMsMsInfo(int index) {
    if (massCol.get(index) == null || massCol.get(index) == 0L) {
      return null;
    }
    return new DDAMsMsInfoImpl(massCol.get(index), null,
        collisionEnergyColumn.get(index).floatValue(), null, null, getMsLevel(index),
        getActivationMethod(index), null);
  }

  public int getMsLevel(int index) {
    return switch (msLevelCol.get(index).intValue()) {
      case 0 -> 1;
      case 1 -> 2;
      default -> throw new IllegalStateException("Unknown scan mode.");
    };
  }

  @Override
  protected String getColumnHeadersForQuery() {

    BafStepsTable stepsTable = new BafStepsTable();

    final String stepsString = stepsTable.columns().stream()
        .map(col -> "%s.%s".formatted(BafStepsTable.NAME, col.getCoulumnName()))
        .collect(Collectors.joining(", "));
    final String collisionEnergyCol =
        BafVariables.NAME + "." + BafVariables.VALUE_COL + " AS CollisionEnergy";
    final String isolationWidthCol =
        BafVariables.NAME + "." + BafVariables.VALUE_COL + " AS IsolationWidth";
    return String.join(", ", stepsString, collisionEnergyCol, isolationWidthCol);
  }

  @Override
  protected String getQueryText(String columnHeadersForQuery) {
    final String query = "SELECT " + columnHeadersForQuery + " FROM " + BafStepsTable.NAME + //
        // merges the collision energy column into this table (below)
        " LEFT JOIN " + BafVariables.NAME + " ON " + BafStepsTable.NAME + "." //
        + BafStepsTable.TARGET_SPECTRUM_COL + "=" + BafVariables.NAME + "." //
        + BafVariables.SPECTRUM_COL + " AND " + BafVariables.NAME + "." //
        + BafVariables.VARIABLE_COL + "=5" + //
        " LEFT JOIN " + BafVariables.NAME + " ON " + BafStepsTable.NAME + "." //
        + BafStepsTable.TARGET_SPECTRUM_COL + "=" + BafVariables.NAME + "." //
        + BafVariables.SPECTRUM_COL + " AND " + BafVariables.NAME + "." //
        + BafVariables.VARIABLE_COL + "=8";
    return query;
  }

  @Override
  public boolean executeQuery(Connection connection) {
    return super.executeQuery(connection);
  }
}
