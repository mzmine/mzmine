/*
 * Copyright 2006-2021 The MZmine Development Team
 * This file is part of MZmine.
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package io.github.mzmine.modules.io.import_waters;

import MassLynxSDK.MassLynxIonMode;
import MassLynxSDK.MassLynxRawScanReader;
import MassLynxSDK.MasslynxRawException;
import MassLynxSDK.Scan;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.impl.SimpleScan;
import net.csibio.aird.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;

public class IntermediateScan implements Comparable<IntermediateScan> {

  private boolean iscontinuum;
  private RawDataFile newMZmineFile;
  private int mslevel;
  private MassLynxIonMode ionmode;
  private Range<Double> MZRange;
  private int function_number;
  private float retentionTime;
  private int numscan;

  public IntermediateScan(RawDataFile newMZmineFile,boolean iscontinuum,int mslevel, MassLynxIonMode ionmode,
      Range<Double> MZRange, int function_number,float retentionTime,int numscan)
  {
    this.newMZmineFile=newMZmineFile;
    this.function_number=function_number;
    this.retentionTime=retentionTime;
    this.MZRange=MZRange;
    this.ionmode=ionmode;
    this.iscontinuum=iscontinuum;
    this.mslevel=mslevel;
    this.numscan=numscan;
  }

  public boolean isIscontinuum() {
    return iscontinuum;
  }

  public RawDataFile getNewMZmineFile() {
    return newMZmineFile;
  }

  public int getNumscan() {
    return numscan;
  }

  public int getMslevel() {
    return mslevel;
  }

  public MassLynxIonMode getIonmode() {
    return ionmode;
  }

  public Range<Double> getMZRange() {
    return MZRange;
  }

  public int getFunction_number() {
    return function_number;
  }

  public float getRetentionTime() {
    return retentionTime;
  }


  /**
   * for the simplescan
   * @param mzmine_scan
   * @param rawscanreader
   * @return
   * @throws MasslynxRawException
   */

  public SimpleScan getScan(int mzmine_scan, MassLynxRawScanReader rawscanreader)
      throws MasslynxRawException {
    PolarityType polarity = PolarityType.UNKNOWN;

    //scan Value
    Scan scan = rawscanreader.ReadScan(this.function_number,this.numscan);

      //Spectrum type is known as per Continuum function
      MassSpectrumType spectrumType=this.iscontinuum?MassSpectrumType.PROFILE:MassSpectrumType.CENTROIDED;

      //Polarity is calculated using Ion mode
      polarity= this.ionmode==MassLynxIonMode.ES_POS?PolarityType.POSITIVE:PolarityType.NEGATIVE;

      SimpleScan simplescan = new SimpleScan(this.newMZmineFile,mzmine_scan,this.getMslevel(),
          this.getRetentionTime(),null, ArrayUtil.fromFloatToDouble(scan.GetMasses()),ArrayUtil.fromFloatToDouble(scan.GetIntensities())
          ,spectrumType,polarity,"",
          this.getMZRange());
    return simplescan;
  }

  @Override
  public int compareTo(@NotNull IntermediateScan obj1) {
    float retentionTime2 = ((IntermediateScan)obj1).getRetentionTime();

    return Float.compare(this.retentionTime,retentionTime2);
  }
}

