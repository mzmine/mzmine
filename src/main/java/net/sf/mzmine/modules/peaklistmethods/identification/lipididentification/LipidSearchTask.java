package net.sf.mzmine.modules.peaklistmethods.identification.lipididentification;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;
import com.google.common.collect.Range;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.IonizationType;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.PolarityType;
import net.sf.mzmine.datamodel.impl.SimplePeakIdentity;
import net.sf.mzmine.datamodel.impl.SimplePeakList;
import net.sf.mzmine.datamodel.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.impl.HeadLessDesktop;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.identification.lipididentification.lipididentificationtools.MSMSLipidTools;
import net.sf.mzmine.modules.peaklistmethods.identification.lipididentification.lipids.LipidClasses;
import net.sf.mzmine.modules.peaklistmethods.identification.lipididentification.lipids.lipidmodifications.LipidModification;
import net.sf.mzmine.modules.peaklistmethods.identification.lipididentification.lipidutils.LipidIdentity;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.massdetection.exactmass.ExactMassDetector;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.massdetection.exactmass.ExactMassDetectorParameters;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;

public class LipidSearchTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private double finishedSteps, totalSteps;
  private PeakList peakList;

  private Object[] selectedObjects;
  private LipidClasses[] selectedLipids;
  private int minChainLength, maxChainLength, maxDoubleBonds, minDoubleBonds;
  private MZTolerance mzTolerance, mzToleranceMS2;
  private IonizationType ionizationType;
  private Boolean searchForMSMSFragments;
  private Boolean searchForModifications;
  private double noiseLevelMSMS;
  private double[] lipidModificationMasses;
  private LipidModification[] lipidModification;

  private ParameterSet parameters;

  /**
   * @param parameters
   * @param peakList
   */
  public LipidSearchTask(ParameterSet parameters, PeakList peakList) {

    this.peakList = peakList;
    this.parameters = parameters;

    minChainLength = parameters.getParameter(LipidSearchParameters.minChainLength).getValue();
    maxChainLength = parameters.getParameter(LipidSearchParameters.maxChainLength).getValue();
    maxDoubleBonds = parameters.getParameter(LipidSearchParameters.maxDoubleBonds).getValue();
    minDoubleBonds = parameters.getParameter(LipidSearchParameters.minDoubleBonds).getValue();
    mzTolerance = parameters.getParameter(LipidSearchParameters.mzTolerance).getValue();
    selectedObjects = parameters.getParameter(LipidSearchParameters.lipidClasses).getValue();
    ionizationType = parameters.getParameter(LipidSearchParameters.ionizationMethod).getValue();
    searchForMSMSFragments =
        parameters.getParameter(LipidSearchParameters.searchForMSMSFragments).getValue();
    searchForModifications =
        parameters.getParameter(LipidSearchParameters.useModification).getValue();
    mzToleranceMS2 = parameters.getParameter(LipidSearchParameters.mzToleranceMS2).getValue();
    noiseLevelMSMS = parameters.getParameter(LipidSearchParameters.noiseLevel).getValue();
    lipidModification = parameters.getParameter(LipidSearchParameters.modification).getChoices();

    // Convert Objects to LipidClasses
    selectedLipids = Arrays.stream(selectedObjects).filter(o -> o instanceof LipidClasses)
        .map(o -> (LipidClasses) o).toArray(LipidClasses[]::new);
  }

  /**
   * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  @Override
  public double getFinishedPercentage() {
    if (totalSteps == 0)
      return 0;
    return (finishedSteps) / totalSteps;
  }

  /**
   * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
   */
  @Override
  public String getTaskDescription() {
    return "Prediction of lipids in " + peakList;
  }

  /**
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {

    setStatus(TaskStatus.PROCESSING);

    logger.info("Starting lipid predriction in " + peakList);

    PeakListRow rows[] = peakList.getRows();

    // Check if lipids should be modified
    if (searchForModifications == true) {
      lipidModificationMasses = getLipidModificationMasses(lipidModification);
    }
    // Calculate how many possible lipids we will try
    totalSteps = ((maxChainLength - minChainLength + 1) * (maxDoubleBonds - minDoubleBonds + 1))
        * selectedLipids.length;

    // Try all combinations of fatty acid lengths and double bonds
    for (int i = 0; i < selectedLipids.length; i++) {
      int numberOfAcylChains = selectedLipids[i].getNumberOfAcylChains();
      int numberOfAlkylChains = selectedLipids[i].getNumberofAlkyChains();
      for (int chainLength = minChainLength; chainLength <= maxChainLength; chainLength++) {
        for (int chainDoubleBonds =
            minDoubleBonds; chainDoubleBonds <= maxDoubleBonds; chainDoubleBonds++) {
          // Task canceled?
          if (isCanceled())
            return;

          // If we have non-zero fatty acid, which is shorter
          // than minimal length, skip this lipid
          if (((chainLength > 0) && (chainLength < minChainLength))) {
            finishedSteps++;
            continue;
          }

          // If we have more double bonds than carbons, it
          // doesn't make sense, so let's skip such lipids
          if (((chainDoubleBonds > 0) && (chainDoubleBonds > chainLength - 1))) {
            finishedSteps++;
            continue;
          }
          // Prepare a lipid instance
          LipidIdentity lipidChain = new LipidIdentity(selectedLipids[i], chainLength,
              chainDoubleBonds, numberOfAcylChains, numberOfAlkylChains);
          // Find all rows that match this lipid
          findPossibleLipid(lipidChain, rows);
          finishedSteps++;
        }
      }
    }
    // Add task description to peakList
    ((SimplePeakList) peakList).addDescriptionOfAppliedTask(
        new SimplePeakListAppliedMethod("Identification of lipid identification", parameters));

    // Repaint the window to reflect the change in the peak list
    Desktop desktop = MZmineCore.getDesktop();
    if (!(desktop instanceof HeadLessDesktop))
      desktop.getMainWindow().repaint();

    setStatus(TaskStatus.FINISHED);

    logger.info("Finished lipid prediction in " + peakList);

  }

  /**
   * Check if candidate peak may be a possible adduct of a given main peak
   * 
   * @param mainPeak
   * @param possibleFragment
   */
  private void findPossibleLipid(LipidIdentity lipid, PeakListRow rows[]) {
    double lipidIonMass = 0.0;
    double lipidMass = 0.0;
    if (ionizationType.toString().contains("2M")) {
      lipidMass = lipid.getMass() * 2;
    } else {
      lipidMass = lipid.getMass();
    }
    if (ionizationType.toString().contains("2-")) {
      lipidIonMass = (lipidMass + ionizationType.getAddedMass()) / 2;
    } else {
      lipidIonMass = lipidMass + ionizationType.getAddedMass();
    }
    logger.info("Searching for lipid " + lipid.getDescription() + ", " + lipidIonMass + " m/z");
    for (int rowIndex = 0; rowIndex < rows.length; rowIndex++) {
      if (isCanceled())
        return;
      Range<Double> mzTolRange12C = mzTolerance.getToleranceRange(rows[rowIndex].getAverageMZ());
      if (mzTolRange12C.contains(lipidIonMass)) {
        // Calc rel mass deviation;
        double relMassDev =
            ((lipidIonMass - rows[rowIndex].getAverageMZ()) / lipidIonMass) * 1000000;
        rows[rowIndex].addPeakIdentity(lipid, false);
        rows[rowIndex].setComment("Ionization: " + ionizationType.getAdduct() + ", Δ "
            + NumberFormat.getInstance().format(relMassDev) + " ppm"); // Calc relativ mass
        // deviation
        // If search for MSMS fragments is selected search for fragments
        if (searchForMSMSFragments == true) {
          searchMsmsFragments(rows, lipidIonMass, rowIndex, lipid);
        }
        // Notify the GUI about the change in the project
        MZmineCore.getProjectManager().getCurrentProject().notifyObjectChanged(rows[rowIndex],
            false);
        logger.info("Found lipid: " + lipid.getName() + ", Δ "
            + NumberFormat.getInstance().format(relMassDev) + " ppm");
      }
      // If search for modifications is selected search for modifications in MS1
      if (searchForModifications == true) {
        searchModifications(rows[rowIndex], lipidIonMass, lipid, lipidModificationMasses,
            mzTolRange12C);
      }
    }
  }


  private void searchMsmsFragments(PeakListRow rows[], double lipidIonMass, int rowIndex,
      LipidIdentity lipid) {
    ExactMassDetector massDetector = new ExactMassDetector(); // other mass detectors!
    ExactMassDetectorParameters parametersMSMS = new ExactMassDetectorParameters();
    ExactMassDetectorParameters.noiseLevel.setValue(noiseLevelMSMS);

    // Check if selected feature has MSMS spectra
    if (rows[rowIndex].getBestFragmentation() != null) {
      DataPoint[] massList =
          massDetector.getMassValues(rows[rowIndex].getBestFragmentation(), parametersMSMS);
      /**
       * Check for lipid class specific fragment lipid class fragments are derived from lipid blast
       */
      MSMSLipidTools msmsLipidTools = new MSMSLipidTools();

      // check for negative polarity
      if (peakList.getRow(rowIndex).getBestFragmentation().getPolarity() == PolarityType.NEGATIVE) {

        // check if lipid class has set negative fragments
        String[] fragments = lipid.getLipidClass().getMsmsFragmentsNegativeIonization();
        ArrayList<String> listOfAnnotatedNegativeFragments = new ArrayList<String>();
        for (int i = 0; i < massList.length; i++) {
          Range<Double> mzTolRangeMSMS = mzToleranceMS2.getToleranceRange(massList[i].getMZ());
          String annotatedNegativeFragment = msmsLipidTools.checkForNegativeClassSpecificFragment(
              mzTolRangeMSMS, rows[rowIndex].getPreferredPeakIdentity(), lipidIonMass, fragments);
          if (annotatedNegativeFragment.equals("") == false) {
            listOfAnnotatedNegativeFragments.add(annotatedNegativeFragment);
          }
        }

        if (listOfAnnotatedNegativeFragments.isEmpty() == false) {

          // predict lipid fatty acid composition if possible
          ArrayList<String> listOfPossibleFattyAcidCompositions =
              msmsLipidTools.predictFattyAcidComposition(listOfAnnotatedNegativeFragments,
                  rows[rowIndex].getPreferredPeakIdentity());
          for (int i = 0; i < listOfPossibleFattyAcidCompositions.size(); i++) {
            // Add possible composition to comment
            if (rows[rowIndex].getComment().equals(null)) {
              rows[rowIndex].setComment(" " + listOfPossibleFattyAcidCompositions.get(i));
            } else {
              rows[rowIndex].setComment(rows[rowIndex].getComment() + ";" + " "
                  + listOfPossibleFattyAcidCompositions.get(i));
            }
          }

          // add class specific fragments
          for (int i = 0; i < listOfAnnotatedNegativeFragments.size(); i++) {
            if (listOfAnnotatedNegativeFragments.contains("C")) {
              // Add fragment to comment
              if (rows[rowIndex].getComment().equals(null)) {
                rows[rowIndex].setComment(" " + listOfPossibleFattyAcidCompositions.get(i));
              } else {
                rows[rowIndex].setComment(rows[rowIndex].getComment() + ";" + " "
                    + listOfPossibleFattyAcidCompositions.get(i));
              }
            }
          }
        }
      }

      // if (peakList.getRow(rowIndex).getBestFragmentation().getPolarity() ==
      // PolarityType.POSITIVE) {
      // for (int i = 0; i < massList.length; i++) {
      // Range<Double> mzTolRangeMSMS = mzToleranceMS2.getToleranceRange(massList[i].getMZ());
      // ArrayList<String> listOfPositiveFragments =
      // msmsLipidTools.checkForPositiveClassSpecificFragments(mzTolRangeMSMS,
      // rows[rowIndex].getPreferredPeakIdentity(), lipidIonMass);
      // if (listOfPositiveFragments.isEmpty() == false) {
      // for (int j = 0; j < listOfPositiveFragments.size(); j++) {
      // // Add masses to comment
      // System.out
      // .println(listOfPositiveFragments.get(j) + "\n" + listOfPositiveFragments.size());
      // if (rows[rowIndex].getComment().equals(null)) {
      // rows[rowIndex].setComment(" " + listOfPositiveFragments.get(j));
      // } else {
      // rows[rowIndex].setComment(
      // rows[rowIndex].getComment() + ";" + " " + listOfPositiveFragments.get(j));
      // }
      // }
      // }
      // }
      // }
    }

  }

  private void searchModifications(PeakListRow rows, double lipidIonMass, LipidIdentity lipid,
      double[] lipidModificationMasses, Range<Double> mzTolModification) {
    int charge = 0;
    if (ionizationType.toString().contains("2-")) {
      charge = 2;
    } else {
      charge = 1;
    }
    for (int j = 0; j < lipidModificationMasses.length; j++) {
      if (mzTolModification.contains(lipidIonMass + (lipidModificationMasses[j] / charge))) {
        // Calc relativ mass deviation
        double relMassDev =
            (((lipidIonMass + (lipidModificationMasses[j] / charge)) - rows.getAverageMZ())
                / (lipidIonMass + (lipidModificationMasses[j] / charge))) * 1000000;
        // Add row identity
        rows.addPeakIdentity(new SimplePeakIdentity(lipid + " " + lipidModification[j]), false);
        rows.setComment("Ionization: " + ionizationType.getAdduct() + " " + lipidModification[j]
            + ", Δ " + NumberFormat.getInstance().format(relMassDev) + " ppm");
        logger.info("Found modified lipid: " + lipid.getName() + " " + lipidModification[j] + ", Δ "
            + NumberFormat.getInstance().format(relMassDev) + " ppm");
        // Notify the GUI about the change in the project
        MZmineCore.getProjectManager().getCurrentProject().notifyObjectChanged(rows, false);
      }
    }
  }

  private double[] getLipidModificationMasses(LipidModification[] lipidModification) {
    double[] lipidModificationMasses = new double[lipidModification.length];
    for (int i = 0; i < lipidModification.length; i++) {
      lipidModificationMasses[i] = lipidModification[i].getModificationMass();
    }
    return lipidModificationMasses;
  }
}
