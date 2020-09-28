package io.github.mzmine.modules.io.tdfimport.datamodel.sql;

import java.util.Arrays;

public class TDFPrecursorTable  extends TDFDataTable<Integer> {

  public static final String PRECURSOR_TABLE_NAME = "Precursors";
  public static final String ID_COLUMN_HEADER = "Id";

  public TDFPrecursorTable() {
    super(PRECURSOR_TABLE_NAME, ID_COLUMN_HEADER);

    columns.addAll(Arrays.asList(new TDFDataColumn<Double>("LargestPeakMz"),
        new TDFDataColumn<Double>("AverageMz"),
        new TDFDataColumn<Double>("MonoisotopicMz"),
        new TDFDataColumn<Integer>("Charge"),
        new TDFDataColumn<Double>("ScanNumber"),
        new TDFDataColumn<Double>("Intensity"),
        new TDFDataColumn<Integer>("Parent")
    ));
  }

  @Override
  public boolean isValid() {
    return true;
  }
}
