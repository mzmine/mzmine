package io.github.mzmine.modules.io.tdfimport.datamodel.sql;

import java.util.Arrays;

public class TDFFrameTable extends TDFDataTable<Integer> {

  public static final String FRAME_TABLE_NAME = "Frames";
  public static final String ID_COLUMN_HEADER = "Id";

  public static final String TIME_COLUMN_NAME = "Time";
  public static final String POLARITY_COLUMN_NAME = "Polarity";
  public static final String SCAN_MODE_COLUMN_NAME = "ScanMode";
  public static final String MSMS_TYPE_COLUMN_NAME = "MsMsType";
  public static final String TIMS_ID_COLUMN_NAME = "TimsId";
  public static final String MAX_INTENSITY_COLUMN_NAME = "MaxIntensity";
  public static final String SUMMED_INTENSITIES_COLUMN_NAME = "SummedIntensities";
  public static final String NUM_SCANS_COLUMN_NAME = "NumScans";
  public static final String NUM_PEAKS_COLUMN_NAME = "NumPeaks";
  public static final String MZ_CALIBRATION_COLUMN_NAME = "MzCalibration";
  public static final String T1_COLUMN_NAME = "T1";
  public static final String T2_COLUMN_NAME = "T2";
  public static final String TIMS_CALIBRATION_COLUMN_NAME = "TimsCalibration";
  public static final String PROPERTY_GROUP_COLUMN_NAME = "PropertyGroup";
  public static final String ACCUMULATION_TIME_COLUMN_NAME = "AccumulationTime";
  public static final String RAMP_TIME_COLUMN_NAME = "RampTime";

  public TDFFrameTable() {
    super(FRAME_TABLE_NAME, ID_COLUMN_HEADER);
    columns.addAll(Arrays.asList(new TDFDataColumn<Double>(TIME_COLUMN_NAME),
        new TDFDataColumn<String>(POLARITY_COLUMN_NAME),
        new TDFDataColumn<Integer>(SCAN_MODE_COLUMN_NAME),
        new TDFDataColumn<Integer>(MSMS_TYPE_COLUMN_NAME),
        new TDFDataColumn<Integer>(TIMS_ID_COLUMN_NAME),
        new TDFDataColumn<Integer>(MAX_INTENSITY_COLUMN_NAME),
        new TDFDataColumn<Integer>(SUMMED_INTENSITIES_COLUMN_NAME),
        new TDFDataColumn<Integer>(NUM_SCANS_COLUMN_NAME),
        new TDFDataColumn<Integer>(NUM_PEAKS_COLUMN_NAME),
        new TDFDataColumn<Integer>(MZ_CALIBRATION_COLUMN_NAME),
        new TDFDataColumn<Integer>(T1_COLUMN_NAME),
        new TDFDataColumn<Integer>(T2_COLUMN_NAME),
        new TDFDataColumn<Integer>(TIMS_CALIBRATION_COLUMN_NAME),
        new TDFDataColumn<Integer>(PROPERTY_GROUP_COLUMN_NAME),
        new TDFDataColumn<Integer>(ACCUMULATION_TIME_COLUMN_NAME),
        new TDFDataColumn<Double>(RAMP_TIME_COLUMN_NAME)));
  }

  @Override
  public boolean isValid() {
    return false;
  }
}
