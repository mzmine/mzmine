package io.github.mzmine.modules.io.tdfimport.datamodel.sql;

import java.util.Arrays;
import java.util.Collections;

public class TDFFrameTable extends TDFDataTable<Integer> {

  public static final String FRAME_TABLE_NAME = "Frames";
  public static final String ID_COLUMN_HEADER = "Id";

  public TDFFrameTable() {
    super(FRAME_TABLE_NAME, ID_COLUMN_HEADER);
    columns.addAll(Arrays.asList(new TDFDataColumn<Double>("Time"),
        new TDFDataColumn<String>("Polarity"),
        new TDFDataColumn<Integer>("ScanMode"),
        new TDFDataColumn<Integer>("MsMsType"),
        new TDFDataColumn<Integer>("TimsId"),
        new TDFDataColumn<Integer>("MaxIntensity"),
        new TDFDataColumn<Integer>("SummedIntensities"),
        new TDFDataColumn<Integer>("NumScans"),
        new TDFDataColumn<Integer>("NumPeaks"),
        new TDFDataColumn<Integer>("MzCalibration"),
        new TDFDataColumn<Integer>("T1"),
        new TDFDataColumn<Integer>("T2"),
        new TDFDataColumn<Integer>("TimsCalibration"),
        new TDFDataColumn<Integer>("PropertyGroup"),
        new TDFDataColumn<Integer>("AccumulationTime"),
        new TDFDataColumn<Double>("RampTime")));
  }

  @Override
  public boolean isValid() {
    return false;
  }
}
