package io.github.mzmine.modules.io.tdfimport.datamodel.sql;

import java.util.Arrays;

public class TDFFrameMsMsInfoTable extends TDFDataTable<Integer> {

  public static final String FRAME_MSMS_INFO_TABLE = "FrameMsMsInfo";
  public static final String FRAME_COLUMN_NAME = "Frame";

  public static final String PARENT_COLUMN_NAME = "Parent";
  public static final String TRIGGER_MASS_COLUMN_NAME = "TriggerMass";
  public static final String ISOLATION_WIDTH_COLUMN_NAME = "IsolationWidth";
  public static final String PRECURSOR_CHARGE_COLUMN_NAME = "PrecursorCharge";
  public static final String COLLISION_ENERGY_COLUMN_NAME = "CollisionEnergy";

  public TDFFrameMsMsInfoTable() {
    super(FRAME_MSMS_INFO_TABLE, FRAME_COLUMN_NAME);

    columns.addAll(Arrays.asList(
        new TDFDataColumn<Integer>(PARENT_COLUMN_NAME),
        new TDFDataColumn<Double>(TRIGGER_MASS_COLUMN_NAME),
        new TDFDataColumn<Double>(ISOLATION_WIDTH_COLUMN_NAME),
        new TDFDataColumn<Integer>(PRECURSOR_CHARGE_COLUMN_NAME),
        new TDFDataColumn<Double>(COLLISION_ENERGY_COLUMN_NAME)
    ));
  }

  @Override
  public boolean isValid() {
    return true;
  }
}
