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

  private boolean isContinuum;
  private RawDataFile newMZmineFile;
  private int msLevel;
  private MassLynxIonMode ionMode;
  private Range<Double> MZRange;
  private int functionNumber;
  private float retentionTime;
  private int numScan;

  public IntermediateScan(RawDataFile newMZmineFile,boolean isContinuum,int msLevel, MassLynxIonMode ionMode,
      Range<Double> MZRange, int functionNumber,float retentionTime,int numScan)
  {
    this.newMZmineFile=newMZmineFile;
    this.functionNumber = functionNumber;
    this.retentionTime=retentionTime;
    this.MZRange=MZRange;
    this.ionMode = ionMode;
    this.isContinuum = isContinuum;
    this.msLevel = msLevel;
    this.numScan = numScan;
  }

  public boolean isContinuum() {
    return isContinuum;
  }

  public RawDataFile getNewMZmineFile() {
    return newMZmineFile;
  }

  public int getNumScan() {
    return numScan;
  }

  public int getMsLevel() {
    return msLevel;
  }

  public MassLynxIonMode getIonMode() {
    return ionMode;
  }

  public Range<Double> getMZRange() {
    return MZRange;
  }

  public int getFunctionNumber() {
    return functionNumber;
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

    //scan Value
    Scan scan = rawscanreader.ReadScan(this.functionNumber,this.numScan);

      //Spectrum type is known as per Continuum function
      MassSpectrumType spectrumType=this.isContinuum ?MassSpectrumType.PROFILE:MassSpectrumType.CENTROIDED;

      //Polarity is calculated using Ion mode
    PolarityType  polarity= this.ionMode ==MassLynxIonMode.ES_POS?PolarityType.POSITIVE:PolarityType.NEGATIVE;
    String scanDefination=WatersImportTask.scanDefinationFunction(this.getMsLevel(),this.getNumScan(),polarity,this.getRetentionTime(),this.isContinuum());

      SimpleScan simplescan = new SimpleScan(this.newMZmineFile,mzmine_scan,this.getMsLevel(),
          this.getRetentionTime(),null, ArrayUtil.fromFloatToDouble(scan.GetMasses()),ArrayUtil.fromFloatToDouble(scan.GetIntensities())
          ,spectrumType,polarity,scanDefination,
          this.getMZRange());
    return simplescan;
  }

  @Override
  public int compareTo(@NotNull IntermediateScan obj1) {
    float retentionTime2 = obj1.getRetentionTime();

    return Float.compare(this.retentionTime,retentionTime2);
  }
}

