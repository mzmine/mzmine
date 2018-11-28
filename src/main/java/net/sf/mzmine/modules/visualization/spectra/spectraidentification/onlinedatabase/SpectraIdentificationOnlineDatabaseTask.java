package net.sf.mzmine.modules.visualization.spectra.spectraidentification.onlinedatabase;

import static net.sf.mzmine.modules.peaklistmethods.identification.onlinedbsearch.SingleRowIdentificationParameters.DATABASE;
import java.awt.Color;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jfree.chart.labels.ItemLabelAnchor;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.ui.TextAnchor;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.IonizationType;
import net.sf.mzmine.datamodel.MassSpectrumType;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineProcessingStep;
import net.sf.mzmine.modules.peaklistmethods.identification.onlinedbsearch.DBCompound;
import net.sf.mzmine.modules.peaklistmethods.identification.onlinedbsearch.DBGateway;
import net.sf.mzmine.modules.peaklistmethods.identification.onlinedbsearch.OnlineDatabase;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.massdetection.MassDetector;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.massdetection.centroid.CentroidMassDetector;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.massdetection.centroid.CentroidMassDetectorParameters;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.massdetection.exactmass.ExactMassDetector;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.massdetection.exactmass.ExactMassDetectorParameters;
import net.sf.mzmine.modules.visualization.spectra.SpectraPlot;
import net.sf.mzmine.modules.visualization.spectra.datasets.DataPointsDataSet;
import net.sf.mzmine.modules.visualization.spectra.spectraidentification.SpectraDatabaseSearchLabelGenerator;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.ExceptionUtils;

public class SpectraIdentificationOnlineDatabaseTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  public static final NumberFormat massFormater = MZmineCore.getConfiguration().getMZFormat();

  private int finishedItems = 0, numItems;

  private MZmineProcessingStep<OnlineDatabase> db;
  private double searchedMass;
  private double noiseLevel;
  private MZTolerance mzTolerance;
  private Scan currentScan;
  private SpectraPlot spectraPlot;
  private IonizationType ionType;
  private DBGateway gateway;

  /**
   * Create the task.
   * 
   * @param parameters task parameters.
   * @param peakListRow peak-list row to identify.
   */
  public SpectraIdentificationOnlineDatabaseTask(ParameterSet parameters, Scan currentScan,
      SpectraPlot spectraPlot) {

    this.currentScan = currentScan;
    this.spectraPlot = spectraPlot;

    db = parameters.getParameter(DATABASE).getValue();
    try {
      gateway = db.getModule().getGatewayClass().newInstance();
    } catch (Exception e) {
      e.printStackTrace();
    }

    mzTolerance = parameters.getParameter(SpectraIdentificationOnlineDatabaseParameters.mzTolerance)
        .getValue();
    ionType = parameters.getParameter(SpectraIdentificationOnlineDatabaseParameters.ionizationType)
        .getValue();
    noiseLevel = parameters.getParameter(SpectraIdentificationOnlineDatabaseParameters.noiseLevel)
        .getValue();
  }

  /**
   * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  public double getFinishedPercentage() {
    if (numItems == 0)
      return 0;
    return ((double) finishedItems) / numItems;
  }

  /**
   * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
   */
  public String getTaskDescription() {
    return "Peak identification of " + massFormater.format(searchedMass) + " using " + db;
  }

  /**
   * @see java.lang.Runnable#run()
   */
  public void run() {

    setStatus(TaskStatus.PROCESSING);

    // create mass list for scan
    DataPoint[] massList = null;
    ArrayList<DataPoint> massListAnnotated = new ArrayList<>();
    MassDetector massDetector = null;
    ArrayList<String> allCompoundIDs = new ArrayList<>();

    // Create a new mass list for MS/MS scan. Check if sprectrum is profile or centroid mode
    if (currentScan.getSpectrumType() == MassSpectrumType.CENTROIDED) {
      massDetector = new CentroidMassDetector();
      CentroidMassDetectorParameters parameters = new CentroidMassDetectorParameters();
      CentroidMassDetectorParameters.noiseLevel.setValue(noiseLevel);
      massList = massDetector.getMassValues(currentScan, parameters);
    } else {
      massDetector = new ExactMassDetector();
      ExactMassDetectorParameters parameters = new ExactMassDetectorParameters();
      ExactMassDetectorParameters.noiseLevel.setValue(noiseLevel);
      massList = massDetector.getMassValues(currentScan, parameters);
    }
    numItems = massList.length;
    for (int i = 0; i < massList.length; i++) {
      // loop through every peak in mass list
      if (getStatus() != TaskStatus.PROCESSING) {
        return;
      }
      searchedMass = massList[i].getMZ() - ionType.getAddedMass();
      try {
        // find candidate compounds
        String compoundIDs[] =
            gateway.findCompounds(searchedMass, mzTolerance, 1, db.getParameterSet());
        // Combine strings
        String annotation = "";
        // max number of compounds to top three for visualization
        int counter = 0;
        for (int j = 0; !isCanceled() && j < compoundIDs.length; j++) {
          final DBCompound compound = gateway.getCompound(compoundIDs[j], db.getParameterSet());

          // In case we failed to retrieve data, skip this compound
          if (compound == null)
            continue;
          if (counter < 3) {
            int number = counter + 1;
            annotation = annotation + " " + number + ". " + compound.getName();
            counter++;
          }
        }
        if (annotation != "") {
          allCompoundIDs.add(annotation);
          massListAnnotated.add(massList[i]);
        }
      } catch (Exception e) {
        e.printStackTrace();
        logger.log(Level.WARNING, "Could not connect to " + db, e);
        setStatus(TaskStatus.ERROR);
        setErrorMessage("Could not connect to " + db + ": " + ExceptionUtils.exceptionToString(e));
        return;
      }
      finishedItems++;
    }

    // new mass list
    DataPoint[] annotatedMassList = new DataPoint[massListAnnotated.size()];
    massListAnnotated.toArray(annotatedMassList);
    String[] annotations = new String[annotatedMassList.length];
    allCompoundIDs.toArray(annotations);
    DataPointsDataSet detectedCompoundsDataset =
        new DataPointsDataSet("Detected compounds", annotatedMassList);
    // Add label generator for the dataset
    SpectraDatabaseSearchLabelGenerator labelGenerator =
        new SpectraDatabaseSearchLabelGenerator(annotations, spectraPlot);
    spectraPlot.addDataSet(detectedCompoundsDataset, Color.orange, true, labelGenerator);
    spectraPlot.getXYPlot().getRenderer()
        .setSeriesItemLabelGenerator(spectraPlot.getXYPlot().getSeriesCount(), labelGenerator);
    spectraPlot.getXYPlot().getRenderer().setDefaultPositiveItemLabelPosition(new ItemLabelPosition(
        ItemLabelAnchor.CENTER, TextAnchor.TOP_LEFT, TextAnchor.BOTTOM_CENTER, 0.0), true);
    setStatus(TaskStatus.FINISHED);

  }

}
