package io.github.mzmine.modules.io.tdfimport.datamodel.sql;

import java.util.Arrays;

public class TDFPasefFrameMsMsInfoTable extends TDFDataTable<Integer> {

  public static final String FRAME_COLUMN_NAME = "Frame";
  public static final String PASEF_FRAME_MSMS_TABLE_NAME = "PasefFrameMsMsInfo";
  public static final String SCAN_NUM_BEGIN_COLUMN_NAME = "ScanNumBegin";
  public static final String SCAN_NUM_END_COLUMN_NAME = "ScanNumEnd";
  public static final String ISOLATION_MZ_COLUMN_NAME = "IsolationMz";
  public static final String ISOLATION_WIDTH_COLUMN_NAME = "IsolationWidth";
  public static final String COLLISION_ENERGY_COLUMN_NAME = "CollisionEnergy";
  public static final String PRECURSOR_COLUMN_NAME = "Precursor";

  public TDFPasefFrameMsMsInfoTable() {
    super(PASEF_FRAME_MSMS_TABLE_NAME, FRAME_COLUMN_NAME);

    columns.addAll(Arrays.asList(
        new TDFDataColumn<Integer>(SCAN_NUM_BEGIN_COLUMN_NAME),
        new TDFDataColumn<Integer>(SCAN_NUM_END_COLUMN_NAME),
        new TDFDataColumn<Double>(ISOLATION_MZ_COLUMN_NAME),
        new TDFDataColumn<Double>(ISOLATION_WIDTH_COLUMN_NAME),
        new TDFDataColumn<Double>(COLLISION_ENERGY_COLUMN_NAME),
        new TDFDataColumn<Integer>(PRECURSOR_COLUMN_NAME)
    ));
  }

  @Override
  public boolean isValid() {
    return true;
  }
}
