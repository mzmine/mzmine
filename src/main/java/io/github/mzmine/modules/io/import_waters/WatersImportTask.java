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
  private MassLynxRawInfoReader obj;
  private MassLynxRawScanReader rawscanreader;
  private SimpleScan simplescan;
  private Scan scan;
  private String description;
  private double finishedPercentage;
  private double lastFinishedPercentage;
  private int funcval;
  private int scanvalue;

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
      obj = new MassLynxRawInfoReader(filepath);
      rawscanreader = new MassLynxRawScanReader(filepath);
      MassSpectrumType spectrumType;
      PolarityType polarity = PolarityType.UNKNOWN;
      int mslevel;
      this.funcval = obj.GetFunctionCount();// Gets the number of function in Raw file
      for(int i=0;i<this.funcval;++i)
      {
        //Scan values in each function
        this.scanvalue = obj.GetScansInFunction(i);

        //msLevel is calculated as per Function type
        mslevel=obj.GetFunctionType(i)==MassLynxFunctionType.MS? MassLynxFunctionType.MS.getValue()
            :obj.GetFunctionType(i)==MassLynxFunctionType.MS2?MassLynxFunctionType.MS2.getValue():
                obj.GetFunctionType(i)==MassLynxFunctionType.TOFM?MassLynxFunctionType.TOFM.getValue():0;

        //Spectrum type is calculated as per Continuum function
        spectrumType=obj.IsContinuum(i)?MassSpectrumType.PROFILE:MassSpectrumType.CENTROIDED;

        //Polarity is calculated using Ion mode
        polarity=obj.GetIonMode(i)==MassLynxIonMode.ES_POS?PolarityType.POSITIVE:PolarityType.NEGATIVE;

        //Range is calculated using AcquisitionMass
        Range<Double> mzrange= Range.closed((double)obj.GetAcquisitionMassRange(i).getStart(),(double)obj.GetAcquisitionMassRange(i).getEnd());

        for (int j = 0; j < scanvalue; ++j)
        {
          scan = rawscanreader.ReadScan(i, j);

          simplescan = new SimpleScan(this.newMZmineFile,0,mslevel,obj.GetRetentionTime(i, j),
              null,ArrayUtil.fromFloatToDouble(scan.GetMasses()),ArrayUtil.fromFloatToDouble(scan.GetIntensities()),spectrumType,polarity
                    ,"",mzrange);
        }
      }
    }
    catch (MasslynxRawException e)
    {
      e.printStackTrace();
      MZmineCore.getDesktop().displayErrorMessage("MasslynxRawException :: " + e.getMessage());
      logger.log(Level.SEVERE, "MasslynxRawException :: ", e.getMessage());
    }
  }
}
