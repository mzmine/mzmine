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
import MassLynxSDK.MassLynxRawInfoReader;
import MassLynxSDK.MassLynxRawScanReader;
import MassLynxSDK.MasslynxRawException;
import MassLynxSDK.Scan;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.impl.BuildingMobilityScan;
import io.github.mzmine.datamodel.impl.SimpleFrame;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.csibio.aird.util.ArrayUtil;

public class IntermediateFrame extends IntermediateScan {

  private static final Logger logger = Logger.getLogger(IntermediateFrame.class.getName());
 private int driftScanCount;

 private IMSRawDataFile imsRawDataFile;

  public int getDriftScanCount() {
    return driftScanCount;
  }

  public IntermediateFrame(RawDataFile newMZmineFile, boolean iscontinuum, int mslevel,
      MassLynxIonMode ionmode, Range<Double> MZRange, int function_number, float retentionTime,int numscan,int driftScanCount,IMSRawDataFile imsRawDataFile) {
    super(newMZmineFile, iscontinuum, mslevel, ionmode, MZRange, function_number, retentionTime,numscan);
    this.driftScanCount=driftScanCount;
    this.imsRawDataFile=imsRawDataFile;
  }

  public SimpleFrame toframe(MassLynxRawScanReader rawscanreader, int mzmine_scannum,
      MassLynxRawInfoReader massLynxRawInfoReader)
      throws MasslynxRawException {
    //scan Value
    Scan framescan=null;
    Scan driftScan;
    try {
      framescan = rawscanreader.ReadScan(this.getFunction_number(), this.getNumscan());
    }
    catch(MasslynxRawException e)
    {
      System.out.println("Value of framescan :: "+ e.getMessage());
      logger.log(Level.WARNING, "MasslynxRawException :: ", e.getMessage());
    }
    String frameLogValue=String.valueOf(this.getFunction_number())+" :: "+String.valueOf(this.getNumscan());
    logger.log(Level.INFO,frameLogValue);

    //Mobilities
    double[] mobilities = new double[this.getDriftScanCount()];

    ArrayList<BuildingMobilityScan> mobilityscanlist=new ArrayList<>();
    for (int driftScanNum = 0; driftScanNum < this.getDriftScanCount(); driftScanNum++) {
      try{
        driftScan = rawscanreader.ReadScan(this.getFunction_number(),this.getNumscan(),driftScanNum);
        mobilityscanlist.add(new BuildingMobilityScan(driftScanNum, ArrayUtil.fromFloatToDouble(driftScan.GetMasses()),
            ArrayUtil.fromFloatToDouble(driftScan.GetIntensities())));
        String driftLogValue=String.valueOf(this.getFunction_number())+" :: "
            +String.valueOf(this.getNumscan())+" :: "+String.valueOf(driftScanNum);
        logger.log(Level.INFO,driftLogValue);
      }
      catch(MasslynxRawException e)
      {
      System.out.println("Value of driftscan :: "+ e.getMessage());
        logger.log(Level.WARNING, "MasslynxRawException :: ", e.getMessage());
        mobilityscanlist.add(new BuildingMobilityScan(driftScanNum,new double[0],
            new double[0]));
      }
      mobilities[driftScanNum] = massLynxRawInfoReader.GetDriftTime(getFunction_number(), driftScanNum);
    }
    MassSpectrumType spectrumType=this.isIscontinuum()?MassSpectrumType.PROFILE:MassSpectrumType.CENTROIDED;

    PolarityType polarity= this.getIonmode()==MassLynxIonMode.ES_POS? PolarityType.POSITIVE:PolarityType.NEGATIVE;

    SimpleFrame simpleframe=new SimpleFrame(this.imsRawDataFile,mzmine_scannum,this.getMslevel()
        ,this.getRetentionTime(),ArrayUtil.fromFloatToDouble(framescan.GetMasses()),ArrayUtil.fromFloatToDouble(framescan.GetIntensities()),spectrumType,polarity,"",
        this.getMZRange(), MobilityType.TRAVELING_WAVE,null,null);

    simpleframe.setMobilityScans(mobilityscanlist, false);
    simpleframe.setMobilities(mobilities);

    return simpleframe;
  }

}

