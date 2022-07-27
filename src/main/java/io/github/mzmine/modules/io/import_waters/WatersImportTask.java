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
import MassLynxSDK.MassLynxRawInfoReader;
import MassLynxSDK.MassLynxRawScanReader;
import MassLynxSDK.MasslynxRawException;
import MassLynxSDK.Scan;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.impl.SimpleFrame;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;


public class WatersImportTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(WatersImportTask.class.getName());

  private File fileToOpen;
  private String filepath;
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
    this.filepath=file.getAbsolutePath();
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
    if(this.filepath.equals("")||this.filepath==null)
    {
      setErrorMessage("Invalid file");
      setStatus(TaskStatus.ERROR);
      return;
    }
    if(imsFiles())
    {
      loadIonMobilityFile(this.filepath);
    }
    else
    {
      loadRegularFile(this.filepath);
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
      MassLynxRawScanReader rawscanreader = new MassLynxRawScanReader(filepath);
      ArrayList<IntermediateScan> intermediatescanarray = new ArrayList<>();
      int totalfunctioncount = massLynxRawInfoReader.GetFunctionCount(); // massLynxRawInfoReader.GetFunctionCount() Gets the number of function in Raw file
      IntermediateScan intermediatescan = null;

      for (int functioncount = 0; functioncount < totalfunctioncount; ++functioncount) {
        //total Scan values in each function
        int functionscanval = massLynxRawInfoReader.GetScansInFunction(
            functioncount);


        //msLevel is calculated as per Function type
        int mslevel = getMsLevel(massLynxRawInfoReader, functioncount);

        //Range is calculated using AcquisitionMass
        Range<Double> mzrange = Range.closed(
            (double) massLynxRawInfoReader.GetAcquisitionMassRange(functioncount).getStart(),
            (double) massLynxRawInfoReader.GetAcquisitionMassRange(functioncount).getEnd());

        for (int numscan = 0; numscan < functionscanval; ++numscan) {
          intermediatescan = new IntermediateScan(newMZmineFile,
              massLynxRawInfoReader.IsContinuum(functioncount), mslevel,
              massLynxRawInfoReader.GetIonMode(functioncount), mzrange, functioncount,
              massLynxRawInfoReader.GetRetentionTime(functioncount, numscan), numscan);
          intermediatescanarray.add(intermediatescan);
        }
      }
      //Sorting w.r.t Retentiontime
      Collections.sort(intermediatescanarray);
      for (int i=0;i<intermediatescanarray.size();i++)
      {
        SimpleScan simpleScan = intermediatescanarray.get(i).getScan(i+1, rawscanreader);
        newMZmineFile.addScan(simpleScan);
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
    setFinishedPercentage(1.0);
  }

  /**
   *
   * @param masslynxrawinforeader
   * @param value
   * @return Msvalues
   * @throws MasslynxRawException
   */
  private int getMsLevel(MassLynxRawInfoReader masslynxrawinforeader,int value)
      throws MasslynxRawException {
    if(masslynxrawinforeader.GetFunctionType(value)==MassLynxFunctionType.MS)
    {
      return 1;
    }
    else if (masslynxrawinforeader.GetFunctionType(value)==MassLynxFunctionType.MS2)
    {
      return 2;
    }
    else if (masslynxrawinforeader.GetFunctionType(value)==MassLynxFunctionType.TOFM)
    {
      return MassLynxFunctionType.TOFM.getValue();
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
      MassLynxRawScanReader rawscanreader = new MassLynxRawScanReader(filepath);
      IntermediateFrame intermediateframe;
      int totalfunctioncount = massLynxRawInfoReader.GetFunctionCount();

      ArrayList<IntermediateFrame> intermediateFrameArrayList= new ArrayList<>();
      SimpleFrame simpleFrame;

      for (int i=0;i<totalfunctioncount;++i) {
        //Skipping functions which don't have drift Scan
       if (isReferenceMeasurement(i))
       {
         continue;
       }

       int functionScanValue=massLynxRawInfoReader.GetScansInFunction(i);

        //Drift Scan Value
        int numdriftscan= massLynxRawInfoReader.GetDriftScanCount(i);

        //msLevel is calculated as per Function type
        int mslevel = getMsLevel(massLynxRawInfoReader, i);

        //Range is calculated using AcquisitionMass
        Range<Double> mzrange = Range.closed(
            (double) massLynxRawInfoReader.GetAcquisitionMassRange(i).getStart(),
            (double) massLynxRawInfoReader.GetAcquisitionMassRange(i).getEnd());


        for (int j=0;j<functionScanValue;++j) {

          intermediateframe=new IntermediateFrame(this.newMZmineFile,
              massLynxRawInfoReader.IsContinuum(i), mslevel,
              massLynxRawInfoReader.GetIonMode(i), mzrange, i,
              massLynxRawInfoReader.GetRetentionTime(i, j), j,numdriftscan,functionScanValue,IMSnewMZmineFile);
          intermediateFrameArrayList.add(intermediateframe);
        }
      }
      //Sorting of Array by retention time
      Collections.sort(intermediateFrameArrayList);

      for (int mzmine_scannum = 0; mzmine_scannum < intermediateFrameArrayList.size(); mzmine_scannum++) {

        simpleFrame=intermediateFrameArrayList.get(mzmine_scannum).toframe(rawscanreader,
            mzmine_scannum+1,massLynxRawInfoReader);
        IMSnewMZmineFile.addScan(simpleFrame);
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
  public boolean isReferenceMeasurement(int functionNumber)
  {
    return functionNumber >= 2;
    }
  public boolean imsFiles()
  {
    try{
      MassLynxRawInfoReader massLynxRawInfoReader = new MassLynxRawInfoReader(this.filepath);
      int getfunctioncount=massLynxRawInfoReader.GetFunctionCount();
      for (int i=0;i<getfunctioncount;i++) {
        try {
          int numdriftscan = massLynxRawInfoReader.GetDriftScanCount(3);
          if (numdriftscan > 0) {
            return true;
          }
        }
        catch (MasslynxRawException e)
        {
          logger.log(Level.INFO,"Not a ION Mobility Function");
        }
      }
    }
    catch (MasslynxRawException e) {
      e.printStackTrace();
    }
    return false;
  }
}
