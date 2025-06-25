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

import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.TDFDataColumn;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.TDFDataTable;
import java.util.Arrays;

/**
 * The 'Steps' table contains information about how MS^n spectra etc. are produced: a kind of
 * history of the isolation and fragmentation steps that have led to a "target spectrum" during
 * acquisition.
 */
public class BafStepsTable extends TDFDataTable<Long> {

  public static final String NAME = "Steps";

  public static final String TARGET_SPECTRUM_COL = "TargetSpectrum";
  public static final String NUMBER_COL = "Number";
  public static final String ISOLATION_TYPE_COL = "IsolationType";
  public static final String REACTION_TYPE_COL = "ReactionType";
  public static final String MS_LEVEL_COL = "MsLevel";
  public static final String MASS_COL = "Mass";

  private final TDFDataColumn<Long> targetSpectrumCol;
  private final TDFDataColumn<Long> numberCol = new TDFDataColumn<>(NUMBER_COL);
  private final TDFDataColumn<Long> isolationTypeCol = new TDFDataColumn<>(ISOLATION_TYPE_COL);
  private final TDFDataColumn<Long> reactionTypeCol = new TDFDataColumn<>(REACTION_TYPE_COL);
  private final TDFDataColumn<Long> msLevelCol = new TDFDataColumn<>(MS_LEVEL_COL);
  private final TDFDataColumn<Double> massCol = new TDFDataColumn<>(MASS_COL);

  public BafStepsTable() {
    super(NAME, TARGET_SPECTRUM_COL);

    targetSpectrumCol = (TDFDataColumn<Long>) getColumn(TARGET_SPECTRUM_COL);
    columns.addAll(
        Arrays.asList(numberCol, isolationTypeCol, reactionTypeCol, msLevelCol, massCol));
  }
}
