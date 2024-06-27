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

package io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql;

import java.util.Arrays;

/**
 * An analysis consists of several frames (or "mobility scans"). A frame represents all TOF scans
 * acquired during a single TIMS-voltage ramp.
 */
public class TDFFrameTable extends TDFDataTable<Long> {

  public static final String FRAME_TABLE_NAME = "Frames";

  /**
   * Unique ID for this frame. Numbered consecutively starting from one.
   */
  public static final String FRAME_ID = "Id";

  /**
   * Time (in seconds), relative to the start time of the acquisition.
   */
  public static final String TIME = "Time";

  /**
   * Ionization mode used when acquiring this frame.
   */
  public static final String POLARITY = "Polarity";

  /**
   * 0 = MS; 1 = AutoMSMS; 2 = MRM; 3 = in-source CID; 4 = broadband CID; 8 = PASEF; 9 = DIA; 10 =
   * PRM; 20 = Maldi
   */
  public static final String SCAN_MODE = "ScanMode";

  /**
   * 0 = MS frame; 2 = MS/MS fragment frame; 8 = PASEF frame; 9 = DIA frame; 10 = PRM frame
   */
  public static final String MSMS_TYPE = "MsMsType";

  /**
   * ID for accessing mobility-resolved data for this frame. May be NULL in case this frame does not
   * have any mobility-resolved data.
   */
  public static final String TIMS_ID = "TimsId";

  /**
   * Maximum intensity occurring in all data belonging to this frame (do not use to generate a BPC
   * directly from this!).
   */
  public static final String MAX_INTENSITY = "MaxIntensity";

  /**
   * Sum of all intensities occurring in the data belonging to this frame (can quickly generate a
   * TIC from this).
   */
  public static final String SUMMED_INTENSITIES = "SummedIntensities";

  /**
   * The number of TOF scans that contributed to this frame. If 0, this frame does not contain any
   * data.
   */
  public static final String NUM_SCANS = "NumScans";

  /**
   * The number of peaks stored in this frame (total over all scans).
   */
  public static final String NUM_PEAKS = "NumPeaks";

  /**
   *
   */
  public static final String MZ_CALIBRATION = "MzCalibration";

  /**
   * ID of the mz calibration for this frame. Every frame has exactly one mz calibration.h
   */
  public static final String T1 = "T1";

  /**
   * Measured Temperature2 of the device. Required to perform a temperature compensated mz
   * calibration
   */
  public static final String T2 = "T2";

  /**
   * ID of the TIMS calibration for this frame. Every Frame has exactly one TIMS calibration.
   */
  public static final String TIMS_CALIBRATION = "TimsCalibration";

  /**
   * The property group for this frame. May be overridden by properties for this specific frame in
   * table 'FrameProperties'. Use the 'Properties' view for easy access to frame properties. This
   * field may be NULL, in which case only the FrameProperties apply to this frame.
   */
  public static final String PROPERTY_GROUP = "PropertyGroup";

  /**
   * Time of ion accumulation in the first funnel.
   */
  public static final String ACCUMULATION_TIME = "AccumulationTime";

  /**
   * Time of mobility elution from the second funnel.
   */
  public static final String RAMP_TIME = "RampTime";

  private final TDFDataColumn<Long> frameIdColumn;
  private final TDFDataColumn<Double> timeColumn;
  private final TDFDataColumn<String> polarityColumn;
  private final TDFDataColumn<Long> scanModeColumn;
  private final TDFDataColumn<Long> msMsTypeColumn;
  private final TDFDataColumn<Long> timsIdColumn;
  private final TDFDataColumn<Long> maxIntensityColumn;
  private final TDFDataColumn<Long> summedIntensityColumn;
  private final TDFDataColumn<Long> numScansColumn;
  private final TDFDataColumn<Long> numPeaksColumn;
  private final TDFDataColumn<Long> mzCalibrationColumn;
  private final TDFDataColumn<Double> t1Column;
  private final TDFDataColumn<Double> t2Column;
  private final TDFDataColumn<Long> timsCalibrationColumn;
  private final TDFDataColumn<Long> propertyGroupColumn;
  private final TDFDataColumn<Double> accumulationTimeColumn;
  private final TDFDataColumn<Double> rampTimeColumn;

  public TDFFrameTable() {
    super(FRAME_TABLE_NAME, FRAME_ID);
    columns.addAll(Arrays.asList(new TDFDataColumn<Double>(TIME),
        new TDFDataColumn<String>(POLARITY),
        new TDFDataColumn<Long>(SCAN_MODE),
        new TDFDataColumn<Long>(MSMS_TYPE),
        new TDFDataColumn<Long>(TIMS_ID),
        new TDFDataColumn<Long>(MAX_INTENSITY),
        new TDFDataColumn<Long>(SUMMED_INTENSITIES),
        new TDFDataColumn<Long>(NUM_SCANS),
        new TDFDataColumn<Long>(NUM_PEAKS),
        new TDFDataColumn<Long>(MZ_CALIBRATION),
        new TDFDataColumn<Long>(T1),
        new TDFDataColumn<Long>(T2),
        new TDFDataColumn<Long>(TIMS_CALIBRATION),
        new TDFDataColumn<Long>(PROPERTY_GROUP),
        new TDFDataColumn<Double>(ACCUMULATION_TIME),
        new TDFDataColumn<Double>(RAMP_TIME)));

    frameIdColumn = (TDFDataColumn<Long>) getColumn(FRAME_ID);
    timeColumn = (TDFDataColumn<Double>) getColumn(TIME);
    polarityColumn = (TDFDataColumn<String>) getColumn(POLARITY);
    scanModeColumn = (TDFDataColumn<Long>) getColumn(SCAN_MODE);
    msMsTypeColumn = (TDFDataColumn<Long>) getColumn(MSMS_TYPE);
    timsIdColumn = (TDFDataColumn<Long>) getColumn(TIMS_ID);
    maxIntensityColumn = (TDFDataColumn<Long>) getColumn(MAX_INTENSITY);
    summedIntensityColumn = (TDFDataColumn<Long>) getColumn(SUMMED_INTENSITIES);
    numScansColumn = (TDFDataColumn<Long>) getColumn(NUM_SCANS);
    numPeaksColumn = (TDFDataColumn<Long>) getColumn(NUM_PEAKS);
    mzCalibrationColumn = (TDFDataColumn<Long>) getColumn(MZ_CALIBRATION);
    t1Column = (TDFDataColumn<Double>) getColumn(T1);
    t2Column = (TDFDataColumn<Double>) getColumn(T2);
    timsCalibrationColumn = (TDFDataColumn<Long>) getColumn(TIMS_CALIBRATION);
    propertyGroupColumn = (TDFDataColumn<Long>) getColumn(PROPERTY_GROUP);
    accumulationTimeColumn = (TDFDataColumn<Double>) getColumn(ACCUMULATION_TIME);
    rampTimeColumn = (TDFDataColumn<Double>) getColumn(RAMP_TIME);
  }

  public long getFirstFrameNumber() {
    return getFrameIdColumn().get(0);
  }

  public int lastFrameId() {
    return getFrameIdColumn().get((getFrameIdColumn().size() - 1)).intValue();
  }

  public TDFDataColumn<Long> getFrameIdColumn() {
    return frameIdColumn;
  }

  public TDFDataColumn<Double> getTimeColumn() {
    return timeColumn;
  }

  public TDFDataColumn<String> getPolarityColumn() {
    return polarityColumn;
  }

  public TDFDataColumn<Long> getScanModeColumn() {
    return scanModeColumn;
  }

  public TDFDataColumn<Long> getMsMsTypeColumn() {
    return msMsTypeColumn;
  }

  public TDFDataColumn<Long> getTimsIdColumn() {
    return timsIdColumn;
  }

  public TDFDataColumn<Long> getMaxIntensityColumn() {
    return maxIntensityColumn;
  }

  public TDFDataColumn<Long> getSummedIntensityColumn() {
    return summedIntensityColumn;
  }

  public TDFDataColumn<Long> getNumScansColumn() {
    return numScansColumn;
  }

  public TDFDataColumn<Long> getNumPeaksColumn() {
    return numPeaksColumn;
  }

  public TDFDataColumn<Long> getMzCalibrationColumn() {
    return mzCalibrationColumn;
  }

  public TDFDataColumn<Double> getT1Column() {
    return t1Column;
  }

  public TDFDataColumn<Double> getT2Column() {
    return t2Column;
  }

  public TDFDataColumn<Long> getTimsCalibrationColumn() {
    return timsCalibrationColumn;
  }

  public TDFDataColumn<Long> getPropertyGroupColumn() {
    return propertyGroupColumn;
  }

  public TDFDataColumn<Double> getAccumulationTimeColumn() {
    return accumulationTimeColumn;
  }

  public TDFDataColumn<Double> getRampTimeColumn() {
    return rampTimeColumn;
  }
}
