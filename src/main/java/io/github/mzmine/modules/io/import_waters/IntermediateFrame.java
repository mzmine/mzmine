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
import io.github.mzmine.datamodel.impl.DDAMsMsInfoImpl;
import io.github.mzmine.datamodel.impl.SimpleFrame;
import io.github.mzmine.datamodel.msms.ActivationMethod;
import io.github.mzmine.datamodel.msms.DDAMsMsInfo;
import java.util.ArrayList;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.csibio.aird.util.ArrayUtil;

public class IntermediateFrame extends IntermediateScan {

  private static final Logger logger = Logger.getLogger(IntermediateFrame.class.getName());
 private int driftScanCount;

 private IMSRawDataFile imsRawDataFile;

  private Double setMass;

  private Float collisionEnergy;

  public int getDriftScanCount() {
    return driftScanCount;
  }

  public IntermediateFrame(RawDataFile newMZmineFile, boolean isContinuum, int msLevel,
      MassLynxIonMode ionMode, Range<Double> MZRange, int functionNumber, float retentionTime,int numScan,int driftScanCount,IMSRawDataFile imsRawDataFile,Double setMass,Float collisionEnergy) {
    super(newMZmineFile, isContinuum, msLevel, ionMode, MZRange, functionNumber, retentionTime,numScan,setMass,
        collisionEnergy);
    this.driftScanCount=driftScanCount;
    this.imsRawDataFile=imsRawDataFile;
    this.collisionEnergy = collisionEnergy;
    this.setMass =setMass;
  }

  public SimpleFrame toFrame(MassLynxRawScanReader rawscanreader, int mzmine_scannum,
      MassLynxRawInfoReader massLynxRawInfoReader)
      throws MasslynxRawException {
    //scan Value
    Scan frameScan=null;
    Scan driftScan;
    //Frame
    SimpleFrame simpleFrame;

    try {
      frameScan = rawscanreader.ReadScan(this.getFunctionNumber(), this.getNumScan());
    }
    catch(MasslynxRawException e)
    {
      logger.log(Level.WARNING, "MasslynxRawException :: ", e.getMessage());
    }

    //Mobilities
    double[] mobilities = new double[this.getDriftScanCount()];

    ArrayList<BuildingMobilityScan> mobilityscanlist=new ArrayList<>();

    for (int driftScanNum = 0; driftScanNum < this.getDriftScanCount(); driftScanNum++) {

      try{

        driftScan = rawscanreader.ReadScan(this.getFunctionNumber(),this.getNumScan(),driftScanNum);

        mobilityscanlist.add(new BuildingMobilityScan(driftScanNum, ArrayUtil.fromFloatToDouble(driftScan.GetMasses()),
            ArrayUtil.fromFloatToDouble(driftScan.GetIntensities())));
      }
      catch(MasslynxRawException e)
      {
        logger.log(Level.WARNING, "MasslynxRawException :: ", e.getMessage());

        mobilityscanlist.add(new BuildingMobilityScan(driftScanNum,new double[0],
            new double[0]));
      }
      mobilities[driftScanNum] = massLynxRawInfoReader.GetDriftTime(getFunctionNumber(), driftScanNum);
    }

    MassSpectrumType spectrumType=this.isContinuum()?MassSpectrumType.PROFILE:MassSpectrumType.CENTROIDED;

    PolarityType polarity= this.getIonMode()==MassLynxIonMode.ES_POS? PolarityType.POSITIVE:PolarityType.NEGATIVE;
    String scanDefinition=WatersImportTask.scanDefinitionFunction(this.getMsLevel(),this.getNumScan(),polarity,this.getRetentionTime(),this.isContinuum());

    if(Objects.isNull(frameScan))
    {
      simpleFrame=new SimpleFrame(this.imsRawDataFile,mzmine_scannum,this.getMsLevel()
          ,this.getRetentionTime(),null,null,spectrumType,polarity,"",
          this.getMZRange(), MobilityType.TRAVELING_WAVE,null,null);
    }
    else
    {
      simpleFrame=new SimpleFrame(this.imsRawDataFile,mzmine_scannum,this.getMsLevel()
          ,this.getRetentionTime(),ArrayUtil.fromFloatToDouble(frameScan.GetMasses()),ArrayUtil.fromFloatToDouble(frameScan.GetIntensities()),
          spectrumType,polarity,scanDefinition,
          this.getMZRange(), MobilityType.TRAVELING_WAVE,null,null);
    }
    simpleFrame.setMobilityScans(mobilityscanlist, false);

    simpleFrame.setMobilities(mobilities);
    //MsMsInfo Implemented

    if(this.getMsLevel()>1)
    {
      DDAMsMsInfo ddaMsMsInfo=this.getSetMass()>0?getDDAMsMsInfo(simpleFrame):null;
      simpleFrame.setMsMsInfo(ddaMsMsInfo);
    }
    return simpleFrame;
  }
  //MsMsInfo Function
  public DDAMsMsInfo getDDAMsMsInfo(io.github.mzmine.datamodel.Scan msmsScan)
  {
    DDAMsMsInfoImpl ddaMsMsInfo = new DDAMsMsInfoImpl(this.getSetMass(),
        null,this.getCollisionEnergy(),msmsScan,null,
        this.getMsLevel(), ActivationMethod.CID,null);
    return ddaMsMsInfo;
  }

}

