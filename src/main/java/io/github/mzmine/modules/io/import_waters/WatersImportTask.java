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
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.impl.SimpleFrame;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;


public class WatersImportTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(WatersImportTask.class.getName());

  private File fileNameToOpen;
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
    this.fileNameToOpen=file;
    this.filepath=this.fileNameToOpen.getAbsolutePath();
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
    if(loading()) {
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
    setDescription("Reading metadata from " + this.fileNameToOpen.getName());
    try {
      MassLynxRawInfoReader massLynxRawInfoReader = new MassLynxRawInfoReader(filepath);
      MassLynxRawScanReader rawscanreader = new MassLynxRawScanReader(filepath);
      ArrayList<IntermediateScan> intermediatescanarray = new ArrayList<>();
      int totalfunctioncount = massLynxRawInfoReader.GetFunctionCount(); // massLynxRawInfoReader.GetFunctionCount() Gets the number of function in Raw file
      IntermediateScan intermediatescan = null;

      //int countnumscan=1;

      for (int functioncount = 0; functioncount < totalfunctioncount; ++functioncount) {
        //total Scan values in each function
        int total_scanvalue_in_each_function = massLynxRawInfoReader.GetScansInFunction(
            functioncount);

        //msLevel is calculated as per Function type
        int mslevel = getMsLevel(massLynxRawInfoReader, functioncount);

        //Range is calculated using AcquisitionMass
        Range<Double> mzrange = Range.closed(
            (double) massLynxRawInfoReader.GetAcquisitionMassRange(functioncount).getStart(),
            (double) massLynxRawInfoReader.GetAcquisitionMassRange(functioncount).getEnd());

        for (int numscan = 0; numscan < total_scanvalue_in_each_function; ++numscan) {
          intermediatescan = new IntermediateScan(this.newMZmineFile,
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
      }
    }
    catch (MasslynxRawException e)
    {
      e.printStackTrace();
      MZmineCore.getDesktop().displayErrorMessage("MasslynxRawException :: " + e.getMessage());
      logger.log(Level.SEVERE, "MasslynxRawException :: ", e.getMessage());
    }
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
      return MassLynxFunctionType.MS.getValue();
    }
    else if (masslynxrawinforeader.GetFunctionType(value)==MassLynxFunctionType.MS2)
    {
      return MassLynxFunctionType.MS2.getValue();
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
    setDescription("Reading metadata from " + this.fileNameToOpen.getName());
    try {
      MassLynxRawInfoReader massLynxRawInfoReader = new MassLynxRawInfoReader(filepath);
      MassLynxRawScanReader rawscanreader = new MassLynxRawScanReader(filepath);
      IntermediateFrame intermediateframe=null;
      int totalfunctioncount = massLynxRawInfoReader.GetFunctionCount();
      SimpleFrame simpleFrame=null;

      for (int i=0;i<totalfunctioncount;++i) {
        //Skipping functions which don't have drift Scan
       if (!isReferenceMeasurement(i))
       {
         continue;
       }
       int totalscanvalue_in_eachfunction=massLynxRawInfoReader.GetScansInFunction(i);
       //Drift Scan Value
       int numdriftscan= massLynxRawInfoReader.GetDriftScanCount(i);

        //msLevel is calculated as per Function type
        int mslevel = getMsLevel(massLynxRawInfoReader, i);

        //Range is calculated using AcquisitionMass
        Range<Double> mzrange = Range.closed(
            (double) massLynxRawInfoReader.GetAcquisitionMassRange(i).getStart(),
            (double) massLynxRawInfoReader.GetAcquisitionMassRange(i).getEnd());


        for (int j=0;j<totalscanvalue_in_eachfunction;++j) {

          intermediateframe=new IntermediateFrame(this.newMZmineFile,
              massLynxRawInfoReader.IsContinuum(i), mslevel,
              massLynxRawInfoReader.GetIonMode(i), mzrange, i,
              massLynxRawInfoReader.GetRetentionTime(i, j), j);
        }
        for (int k=0;k<numdriftscan;++k)
        {
          simpleFrame=intermediateframe.toframe(rawscanreader,k);
        }

      }

    } catch (MasslynxRawException e) {
      e.printStackTrace();
      MZmineCore.getDesktop().displayErrorMessage("MasslynxRawException :: " + e.getMessage());
      logger.log(Level.SEVERE, "MasslynxRawException :: ", e.getMessage());
    }

  }
  public boolean isReferenceMeasurement(int functionNumber)
  {
    try
    {
      MassLynxRawInfoReader massLynxRawInfoReader = new MassLynxRawInfoReader(this.filepath);
      int numdriftscan= massLynxRawInfoReader.GetDriftScanCount(functionNumber);
      if (numdriftscan > 0)
      {
        return true;
      }
      else
      {
        return false;
      }
    }
    catch (MasslynxRawException e) {
      return false;
    }
  }
  //for IMS Files
  public boolean loading()
  {
    try{
      MassLynxRawInfoReader massLynxRawInfoReader = new MassLynxRawInfoReader(this.filepath);
      int getfunctioncount=massLynxRawInfoReader.GetFunctionCount();
      for (int i=0;i<getfunctioncount;i++) {
        try {
          int numdriftscan = massLynxRawInfoReader.GetDriftScanCount(getfunctioncount);
          if (numdriftscan > 0) {
            return true;
          }
        }
        catch (MasslynxRawException e)
        {
          continue;
        }
      }
    }
    catch (MasslynxRawException e) {
      e.printStackTrace();
    }
    return false;
  }
}
