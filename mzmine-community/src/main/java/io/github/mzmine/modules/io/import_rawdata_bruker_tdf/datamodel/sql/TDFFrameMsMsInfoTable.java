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

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.impl.DDAMsMsInfoImpl;
import io.github.mzmine.datamodel.msms.ActivationMethod;
import io.github.mzmine.datamodel.msms.DDAMsMsInfo;
import java.util.Arrays;

/**
 * Additional MS/MS meta-information.
 */
public class TDFFrameMsMsInfoTable extends TDFDataTable<Long> {

  public static final String FRAME_MSMS_INFO_TABLE = "FrameMsMsInfo";

  /**
   * The frame to which this information applies. Should be an MS^2 frame, i.e., the corresponding
   * Frames.MsMsType should be 2.
   */
  public static final String FRAME_ID = "Frame";

  /**
   * Links to the corresponding "parent" MS^1 frame, in which this precursor was found. (Due to
   * possible out-of-order / asynchronous scan-task execution in the engine, this is not necessarily
   * the first MS^1 frame preceding this MS^2 frame in the analysis.) Parent is NULL for MRM scans.
   */
  public static final String PARENT_ID = "Parent";

  /**
   * The mass to which the quadrupole has been tuned for isolating this particular precursor. (in
   * the m/z calibration state that was used during acquisition). May or may not coincide with one
   * of the peaks in the parent frame.
   */
  public static final String TRIGGER_MASS = "TriggerMass";

  /**
   * The total 3-dB width of the isolation window (in m/z units), the center of which is given by
   * 'TriggerMass'.
   */
  public static final String ISOLATION_WIDTH = "IsolationWidth";

  /**
   * The charge state of the precursor as estimated by the precursor selection code that controls
   * the DDA acquisition. Can be NULL, which means that the charge state could not be determined,
   * e.g., because only one isotope peak could be detected.
   */
  public static final String PRECURSOR_CHARGE = "PrecursorCharge";

  /**
   * Collision energy (in eV) using which this frame was produced.
   */
  public static final String COLLISION_ENERGY = "CollisionEnergy";

  TDFDataColumn<Long> frameId;
  TDFDataColumn<Long> parentId;
  TDFDataColumn<Double> precursorMz;
  TDFDataColumn<Double> isolationWidth;
  TDFDataColumn<Long> charge;
  TDFDataColumn<Double> ce;

  public TDFFrameMsMsInfoTable() {
    super(FRAME_MSMS_INFO_TABLE, FRAME_ID);

    parentId = new TDFDataColumn<>(PARENT_ID);
    precursorMz = new TDFDataColumn<>(TRIGGER_MASS);
    isolationWidth = new TDFDataColumn<>(ISOLATION_WIDTH);
    charge = new TDFDataColumn<>(PRECURSOR_CHARGE);
    ce = new TDFDataColumn<>(COLLISION_ENERGY);

    columns.addAll(Arrays.asList(parentId, precursorMz, isolationWidth, charge, ce));

    frameId = (TDFDataColumn<Long>) getColumn(FRAME_ID);
  }

  @Override
  public boolean isValid() {
    return true;
  }

  public DDAMsMsInfo getDDAMsMsInfo(int index, int msLevel, Scan msmsScan, Scan parentScan) {
    double precursor = precursorMz.get(index).doubleValue();
    double width = isolationWidth.get(index).doubleValue();
    DDAMsMsInfoImpl ddaMsMsInfo = new DDAMsMsInfoImpl(precursor, charge.get(index).intValue(),
        ce.get(index).floatValue(), msmsScan, parentScan, msLevel, ActivationMethod.CID,
        Range.closed(precursor - width / 2, precursor + width / 2));
    return ddaMsMsInfo;
  }
}
