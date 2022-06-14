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
import MassLynxSDK.MassLynxIonMode;
import MassLynxSDK.MassLynxRawInfoReader;
import MassLynxSDK.MassLynxRawScanReader;
import MassLynxSDK.MasslynxRawException;
import MassLynxSDK.Scan;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.csibio.aird.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;


public class WatersImportTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(WatersImportTask.class.getName());

  private File fileNameToOpen;
  private String filepath;
  private MZmineProject project;
  private RawDataFile newMZmineFile;
  private final ParameterSet parameters;
  private final Class<? extends MZmineModule> module;
  private MassLynxRawInfoReader massLynxRawInfoReader;
  private MassLynxRawScanReader rawscanreader;
  private SimpleScan simplescan;
  private Scan scan;
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
    String filenamepath = this.filepath;
    if(filenamepath.equals("")||filenamepath==null)
    {
      setErrorMessage("Invalid file");
      setStatus(TaskStatus.ERROR);
      return;
    }
    readMetaData(filenamepath);
    setStatus(TaskStatus.FINISHED);
  }
  private void readMetaData(String filepath){
    try {
      setDescription("Reading metadata from "+this.fileNameToOpen.getName());
      massLynxRawInfoReader = new MassLynxRawInfoReader(filepath);
      rawscanreader = new MassLynxRawScanReader(filepath);
      MassSpectrumType spectrumType;
      PolarityType polarity = PolarityType.UNKNOWN;
      int mslevel;
      int scanvalue;
      int driftscancount;
      int functioncount =massLynxRawInfoReader.GetFunctionCount();
      ArrayList<SimpleScan> ss=new ArrayList<>();
      ArrayList<Float> drifttime=new ArrayList<>();
      // massLynxRawInfoReader.GetFunctionCount() Gets the number of function in Raw file
      for(int i=0;i<functioncount;++i)
      {
        //total Scan values in each function
        scanvalue = massLynxRawInfoReader.GetScansInFunction(i);

        //msLevel is calculated as per Function type
        mslevel=getMsLevel(massLynxRawInfoReader,i);

        //Spectrum type is calculated as per Continuum function
        spectrumType= massLynxRawInfoReader.IsContinuum(i)?MassSpectrumType.PROFILE:MassSpectrumType.CENTROIDED;

        //Polarity is calculated using Ion mode
        polarity= massLynxRawInfoReader.GetIonMode(i)==MassLynxIonMode.ES_POS?PolarityType.POSITIVE:PolarityType.NEGATIVE;

        //Range is calculated using AcquisitionMass
        Range<Double> mzrange= Range.closed((double) massLynxRawInfoReader.GetAcquisitionMassRange(i).getStart(),(double) massLynxRawInfoReader.GetAcquisitionMassRange(i).getEnd());

        //Drift scan count in each function
        driftscancount=massLynxRawInfoReader.GetDriftScanCount(i);

        //Optimisation need for this loop it is too slow to function
        for (int j = 0; j < scanvalue; ++j)
        {
          scan = rawscanreader.ReadScan(i, j);
          //Scan gives masses and intensities doubt in ScanNumber as well as mzValue[]

          simplescan = new SimpleScan(this.newMZmineFile,0,mslevel,
              massLynxRawInfoReader.GetRetentionTime(i, j),
              null,ArrayUtil.fromFloatToDouble(scan.GetMasses()),ArrayUtil.fromFloatToDouble(scan.GetIntensities()),spectrumType,polarity
                    ,"",mzrange);
          ss.add(simplescan);
        }
        //Error
/*        for (int k = 0; k < driftscancount; ++k)
        {
          drifttime.add(massLynxRawInfoReader.GetDriftTime(i,k));
        }*/
      }
    }
    catch (MasslynxRawException e)
    {
      e.printStackTrace();
      MZmineCore.getDesktop().displayErrorMessage("MasslynxRawException :: " + e.getMessage());
      logger.log(Level.SEVERE, "MasslynxRawException :: ", e.getMessage());
    }
  }
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
    else if (massLynxRawInfoReader.GetFunctionType(value)==MassLynxFunctionType.TOFM)
    {
      return MassLynxFunctionType.TOFM.getValue();
    }
    else
    {
      return 0;
    }
  }
}
