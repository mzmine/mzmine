/*
 * Copyright 2006-2021 The MZmine Development Team
 *
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


import MassLynxSDK.MassLynxFunctionType;
import MassLynxSDK.MassLynxParameters;
import MassLynxSDK.MassLynxRawInfoReader;
import MassLynxSDK.MassLynxRawScanReader;
import MassLynxSDK.MassLynxScanItem;
import MassLynxSDK.MasslynxRawException;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.impl.DDAMsMsInfoImpl;
import io.github.mzmine.datamodel.impl.SimpleFrame;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.datamodel.msms.PasefMsMsInfo;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.scans.ScanUtils;
import java.io.File;
import java.io.IOException;
import java.text.Format;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;


public class WatersImportTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(WatersImportTask.class.getName());

  private File fileToOpen;
  private String filePath;
  private MZmineProject project;
  private RawDataFile newMZmineFile;
  private final ParameterSet parameters;
  private final Class<? extends MZmineModule> module;
  private String description;
  private double finishedPercentage;
  private double lastFinishedPercentage;

  public void setDescription(String description) {
    this.description = description;
  }

  public void setFinishedPercentage(double percentage) {
    if (percentage - lastFinishedPercentage > 0.1) {
      logger.finest(() -> String.format("%s - %d", description, (int) (percentage * 100)) + "%");
      lastFinishedPercentage = percentage;
    }
    finishedPercentage = percentage;
  }

  public WatersImportTask(MZmineProject project,File file, RawDataFile newMZmineFile,
      @NotNull final Class<? extends MZmineModule> module, @NotNull final ParameterSet parameters,
      @NotNull Instant moduleCallDate)
  {
    super(newMZmineFile.getMemoryMapStorage(),moduleCallDate);
    this.fileToOpen =file;
    this.filePath =file.getAbsolutePath();
    this.project = project;
    this.newMZmineFile = newMZmineFile;
    this.parameters = parameters;
    this.module = module;
  }

  @Override
  public String getTaskDescription() {
    return description;
  }

  @Override
  public double getFinishedPercentage() {
    return finishedPercentage;
  }

  @Override
  public void run()
  {
    setStatus(TaskStatus.PROCESSING);
    if(this.filePath.equals("")||this.filePath ==null)
    {
      setErrorMessage("Invalid file");
      setStatus(TaskStatus.ERROR);
      return;
    }

    if(imsFiles())
    {
      loadIonMobilityFile(this.filePath);
    }
    else
    {
      loadRegularFile(this.filePath);
    }
    setStatus(TaskStatus.FINISHED);
  }

  /**
   *
   * @param filepath
   */
  private void loadRegularFile(String filepath){
    setDescription("Reading metadata from " + this.fileToOpen.getName());
    try {
      final MemoryMapStorage storage = MemoryMapStorage.forRawDataFile();
      RawDataFile newMZmineFile = MZmineCore.createNewFile(this.fileToOpen.getName(),filepath,storage);
      MassLynxRawInfoReader massLynxRawInfoReader = new MassLynxRawInfoReader(filepath);
      MassLynxRawScanReader rawScanReader = new MassLynxRawScanReader(filepath);
      ArrayList<IntermediateScan> intermediateScanArray = new ArrayList<>();
      int totalFunctionCount = massLynxRawInfoReader.GetFunctionCount(); // massLynxRawInfoReader.GetFunctionCount() Gets the number of function in Raw file
      IntermediateScan intermediatescan = null;

      for (int functionCount = 0; functionCount < totalFunctionCount; ++functionCount) {
        //total Scan values in each function
        int scanValueInFunction = massLynxRawInfoReader.GetScansInFunction(
            functionCount);


        //msLevel is calculated as per Function type
        int msLevel = getMsLevel(massLynxRawInfoReader, functionCount);

        //Range is calculated using AcquisitionMass
        Range<Double> mzRange = Range.closed(
            (double) massLynxRawInfoReader.GetAcquisitionMassRange(functionCount).getStart(),
            (double) massLynxRawInfoReader.GetAcquisitionMassRange(functionCount).getEnd());

        for (int numScan = 0; numScan < scanValueInFunction; ++numScan) {

          Double SetMass=Double.parseDouble(massLynxRawInfoReader.GetScanItem(functionCount,numScan,MassLynxScanItem.SET_MASS));
          Float collision_energy=Float.parseFloat(massLynxRawInfoReader.GetScanItem(functionCount,numScan,MassLynxScanItem.COLLISION_ENERGY));

          intermediatescan = new IntermediateScan(newMZmineFile,
              massLynxRawInfoReader.IsContinuum(functionCount), msLevel,
              massLynxRawInfoReader.GetIonMode(functionCount), mzRange, functionCount,
              massLynxRawInfoReader.GetRetentionTime(functionCount, numScan), numScan,SetMass,collision_energy);
          intermediateScanArray.add(intermediatescan);
        }
      }
      //Sorting w.r.t Retentiontime
      Collections.sort(intermediateScanArray);
      //this.setFinishedPercentage(0.01);
      int arraySize=intermediateScanArray.size();
      for (int mzmineScan=0;mzmineScan<arraySize;mzmineScan++)
      {
        SimpleScan simpleScan = intermediateScanArray.get(mzmineScan).getScan(mzmineScan+1, rawScanReader);
        newMZmineFile.addScan(simpleScan);
        this.setFinishedPercentage(0.01 * (mzmineScan+1/arraySize));
      }
      this.project.addFile(newMZmineFile);
    }
    catch (MasslynxRawException e)
    {
      e.printStackTrace();
      MZmineCore.getDesktop().displayErrorMessage("MasslynxRawException :: " + e.getMessage());
      logger.log(Level.SEVERE, "MasslynxRawException :: ", e.getMessage());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   *
   * @param masslynxrawinforeader
   * @param functionCount
   * @return Msvalues
   * @throws MasslynxRawException
   */
  private int getMsLevel(MassLynxRawInfoReader masslynxrawinforeader,int functionCount)
      throws MasslynxRawException {
    if(masslynxrawinforeader.GetFunctionType(functionCount)==MassLynxFunctionType.MS)
    {
      return 1;
    }
    else if (masslynxrawinforeader.GetFunctionType(functionCount)==MassLynxFunctionType.MS2)
    {
      return 2;
    }
    else if (masslynxrawinforeader.GetFunctionType(functionCount)==MassLynxFunctionType.TOFM)
    {
      return functionCount>0?2:1;
    }
    else
    {
      return 0;
    }
  }

  /**
   *
   * @param filepath
   */
  public void loadIonMobilityFile(String filepath)
  {
    setDescription("Reading metadata from " + this.fileToOpen.getName());
    try {
      final MemoryMapStorage storage = MemoryMapStorage.forRawDataFile();
      IMSRawDataFile IMSnewMZmineFile = MZmineCore.createNewIMSFile(this.fileToOpen.getName(),filepath,storage);

      MassLynxRawInfoReader massLynxRawInfoReader = new MassLynxRawInfoReader(filepath);
      MassLynxRawScanReader rawScanReader = new MassLynxRawScanReader(filepath);
      IntermediateFrame intermediateframe;
      int totalFunctionCount = massLynxRawInfoReader.GetFunctionCount();

      ArrayList<IntermediateFrame> intermediateFrameArrayList= new ArrayList<>();
      SimpleFrame simpleFrame;

      for (int functionCount=0;functionCount<totalFunctionCount;++functionCount) {
        //Skipping functions which don't have drift Scan
       if (isReferenceMeasurement(functionCount,totalFunctionCount))
       {
         continue;
       }

       int scanValueInFunction=massLynxRawInfoReader.GetScansInFunction(functionCount);

        //Drift Scan Value
        int driftScanInFunction= massLynxRawInfoReader.GetDriftScanCount(functionCount);

        //msLevel is calculated as per Function type
        int msLevel = getMsLevel(massLynxRawInfoReader, functionCount);

        //Range is calculated using AcquisitionMass
        Range<Double> mzRange = Range.closed(
            (double) massLynxRawInfoReader.GetAcquisitionMassRange(functionCount).getStart(),
            (double) massLynxRawInfoReader.GetAcquisitionMassRange(functionCount).getEnd());


        for (int numScan=0;numScan<scanValueInFunction;++numScan) {
          Double SetMass=Double.parseDouble(massLynxRawInfoReader.GetScanItem(functionCount,numScan,MassLynxScanItem.SET_MASS));
          Float collision_energy=Float.parseFloat(massLynxRawInfoReader.GetScanItem(functionCount,numScan,MassLynxScanItem.COLLISION_ENERGY));

          intermediateframe=new IntermediateFrame(this.newMZmineFile,
              massLynxRawInfoReader.IsContinuum(functionCount), msLevel,
              massLynxRawInfoReader.GetIonMode(functionCount), mzRange, functionCount,
              massLynxRawInfoReader.GetRetentionTime(functionCount, numScan), numScan,driftScanInFunction,IMSnewMZmineFile
              ,SetMass,collision_energy);
          intermediateFrameArrayList.add(intermediateframe);
        }
      }
//      Sorting of Array by retention time
      Collections.sort(intermediateFrameArrayList);

      int arraySize=intermediateFrameArrayList.size();

      for (int mzmineScan = 0; mzmineScan < arraySize; mzmineScan++) {


        simpleFrame=intermediateFrameArrayList.get(mzmineScan).toframe(rawScanReader,
            mzmineScan+1,massLynxRawInfoReader);
        IMSnewMZmineFile.addScan(simpleFrame);
        this.setFinishedPercentage(0.01 * (mzmineScan+1/arraySize));
      }
      this.project.addFile(IMSnewMZmineFile);
    } catch (MasslynxRawException e) {
      e.printStackTrace();
      MZmineCore.getDesktop().displayErrorMessage("MasslynxRawException :: " + e.getMessage());
      logger.log(Level.SEVERE, "MasslynxRawException :: ", e.getMessage());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  //Change the function return true if func num is 2
  public boolean isReferenceMeasurement(int functionNumber,int totalFunctionCount)
  {
    return functionNumber >= totalFunctionCount-1;
    }
  public boolean imsFiles()
  {
    try{
      MassLynxRawInfoReader massLynxRawInfoReader = new MassLynxRawInfoReader(this.filePath);
      int totalFunctionCount=massLynxRawInfoReader.GetFunctionCount();
      for (int i=0;i<totalFunctionCount;i++) {
        try {
          int driftScanInFunction = massLynxRawInfoReader.GetDriftScanCount(i);
          if (driftScanInFunction > 0) {
            return true;
          }
        }
        catch (MasslynxRawException e)
        {
          logger.log(Level.INFO,"Not an ION Mobility Function");
        }
      }
    }
    catch (MasslynxRawException e) {
      e.printStackTrace();
    }
    return false;
  }
  public static String scanDefinationFunction(int msLevel,int numScan, PolarityType polarity, float retentionTime,boolean isContinum)
  {
    StringBuilder sb = new StringBuilder();
    Format retentionTimeFormat = MZmineCore.getConfiguration().getRTFormat();
    sb.append("Scan Number= "+numScan+" ;"+"MSLevel= "+msLevel+" ;");
    if(PolarityType.POSITIVE==polarity)
    {
      sb.append("Polarity= Positive; ");
    }
    else
    {
      sb.append("Polarity= Negative; ");
    }
    if (isContinum)
    {
      sb.append("Spectrumtype= p; ");
    }
    else
    {
      sb.append("Spectrumtype= c; ");
    }
     sb.append("RT= ").append(retentionTimeFormat.format(retentionTime)).append("min");
    return sb.toString();
  }

}
