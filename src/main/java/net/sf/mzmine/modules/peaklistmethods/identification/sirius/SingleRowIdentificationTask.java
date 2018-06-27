/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package net.sf.mzmine.modules.peaklistmethods.identification.sirius;

import static net.sf.mzmine.modules.peaklistmethods.identification.sirius.SiriusParameters.ELEMENTS;
import static net.sf.mzmine.modules.peaklistmethods.identification.sirius.SiriusParameters.MZ_TOLERANCE;
import static net.sf.mzmine.modules.peaklistmethods.identification.sirius.SiriusParameters.ISOTOPE_FILTER;
import static net.sf.mzmine.modules.peaklistmethods.identification.sirius.SiriusParameters.MAX_RESULTS;
import static net.sf.mzmine.modules.peaklistmethods.identification.sirius.SiriusParameters.PARENT_MASS;
import static net.sf.mzmine.modules.peaklistmethods.identification.sirius.SiriusParameters.NEUTRAL_MASS;

import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import io.github.msdk.MSDKException;
import io.github.msdk.datamodel.IonAnnotation;
import io.github.msdk.datamodel.IonType;
import io.github.msdk.datamodel.MsSpectrum;
import io.github.msdk.datamodel.SimpleMsSpectrum;
import io.github.msdk.id.sirius.ConstraintsGenerator;
import io.github.msdk.id.sirius.SiriusIdentificationMethod;
import io.github.msdk.util.IonTypeUtil;
import java.text.NumberFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.IonizationType;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import org.openscience.cdk.formula.MolecularFormulaRange;

public class SingleRowIdentificationTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  public static final NumberFormat massFormater = MZmineCore.getConfiguration().getMZFormat();

  private int finishedItems = 0, numItems;

  private double searchedMass;
  private MZTolerance mzTolerance;
  private int charge;
  private int numOfResults;
  private PeakListRow peakListRow;
  private IonizationType ionType;
  private boolean isotopeFilter = false;
  private ParameterSet isotopeFilterParameters;
  private MolecularFormulaRange formulaRange;
  private Double parentMass;

  /**
   * Create the task.
   * 
   * @param parameters task parameters.
   * @param peakListRow peak-list row to identify.
   */
  public SingleRowIdentificationTask(ParameterSet parameters, PeakListRow peakListRow) {

    this.peakListRow = peakListRow;

    searchedMass = parameters.getParameter(NEUTRAL_MASS).getValue();
    mzTolerance = parameters.getParameter(MZ_TOLERANCE).getValue();
    numOfResults = parameters.getParameter(MAX_RESULTS).getValue();

    ionType = parameters.getParameter(NEUTRAL_MASS).getIonType();
    charge = parameters.getParameter(NEUTRAL_MASS).getCharge();

    isotopeFilter = parameters.getParameter(ISOTOPE_FILTER).getValue();
    isotopeFilterParameters = parameters.getParameter(ISOTOPE_FILTER).getEmbeddedParameters();
    formulaRange = parameters.getParameter(ELEMENTS).getValue();
    parentMass = parameters.getParameter(PARENT_MASS).getValue();
  }

  /**
   * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  public double getFinishedPercentage() {
    if (numItems == 0)
      return 0;
    return ((double) finishedItems) / numItems;
  }

  //TODO: todo
  public String getTaskDescription() {
    return "Peak identification of " + massFormater.format(searchedMass) + " using Sirius module";
  }

  /**
   * @see Runnable#run()
   */
  public void run() {

    setStatus(TaskStatus.PROCESSING);

    NumberFormat massFormater = MZmineCore.getConfiguration().getMZFormat();

    ResultWindow window = new ResultWindow(peakListRow, searchedMass, this);
    window.setTitle("Sirius makes fun " + massFormater.format(searchedMass) + " amu");
    window.setVisible(true);

    /*  TODO: What is it for?
    IsotopePattern detectedPattern = peakListRow.getBestIsotopePattern();
    if ((isotopeFilter) && (detectedPattern == null)) {
      final String msg = "Cannot calculate isotope pattern scores, because selected"
          + " peak does not have any isotopes. Have you run the isotope peak grouper?";
      MZmineCore.getDesktop().displayMessage(window, msg);
    }

    */
    ConstraintsGenerator generator = new ConstraintsGenerator();
    FormulaConstraints constraints = generator.generateConstraint(formulaRange);


    double ppm = mzTolerance.getPpmTolerance();

    Feature bestPeak = peakListRow.getBestPeak();
    int ms1index = bestPeak.getRepresentativeScanNumber();
    int ms2index = bestPeak.getMostIntenseFragmentScanNumber();


    RawDataFile rawfile = bestPeak.getDataFile();
    Scan ms1scan = rawfile.getScan(ms1index);
    Scan ms2scan = rawfile.getScan(ms2index);
    int level1 = ms1scan.getMSLevel();
    int level2 = ms2scan.getMSLevel();

    DataPoint[] ms1points = ms1scan.getDataPoints();
    DataPoint[] ms2points = ms2scan.getDataPoints();


    MsSpectrum ms1 = buildSpectrum(ms1points);
    MsSpectrum ms2 = buildSpectrum(ms2points);

    List<MsSpectrum> ms1list, ms2list;
    ms1list = new LinkedList<>();
    ms2list = new LinkedList<>();
    IonType siriusIon = IonTypeUtil.createIonType(ionType.toString());
    ms1list.add(ms1);
    ms2list.add(ms2);

    SiriusIdentificationMethod siriusMethod = new SiriusIdentificationMethod(
        ms1list,
        ms2list,
        parentMass,
        siriusIon,
        numOfResults,
        constraints,
        ppm
    );

    try {
      siriusMethod.execute();
      List<IonAnnotation> results = siriusMethod.getResult();

      IonAnnotation best = results.get(0);
      //TODO: make it
      SiriusCompound compound = new SiriusCompound(best, 10.);

      window.addNewListItem(compound);
    } catch (MSDKException e) {
      e.printStackTrace();
      System.out.println("Hell is here");
    }


/* TAKEN FROM FORMULA PREDICTION
    resultWindow = new ResultWindow(
        "Searching for " + MZmineCore.getConfiguration().getMZFormat()
            .format(searchedMass),
        peakListRow, searchedMass, charge, this);
    resultWindow.setVisible(true);

    logger.finest("Starting search for formulas for " + massRange + " Da");

    IsotopePattern detectedPattern = peakListRow.getBestIsotopePattern();
    if ((checkIsotopes) && (detectedPattern == null)) {
      final String msg = "Cannot calculate isotope pattern scores, because selected"
          + " peak does not have any isotopes. Have you run the isotope peak grouper?";
      MZmineCore.getDesktop().displayMessage(resultWindow, msg);
    }

    IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();

    generator = new MolecularFormulaGenerator(builder,
        massRange.lowerEndpoint(), massRange.upperEndpoint(),
        elementCounts);

    IMolecularFormula cdkFormula;
    while ((cdkFormula = generator.getNextFormula()) != null) {

      if (isCanceled())
        return;

      // Mass is ok, so test other constraints
      checkConstraints(cdkFormula);

    }

    if (isCanceled())
      return;

    logger.finest("Finished formula search for " + massRange
        + " m/z, found " + foundFormulas + " formulas");

    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        resultWindow.setTitle("Finished searching for "
            + MZmineCore.getConfiguration().getMZFormat()
            .format(searchedMass)
            + " amu, " + foundFormulas + " formulas found");

      }
    });
*/

    /* TAKEN FROM ONLIBE DB
    setStatus(TaskStatus.PROCESSING);

    NumberFormat massFormater = MZmineCore.getConfiguration().getMZFormat();

    ResultWindow window = new ResultWindow(peakListRow, searchedMass, this);
    window.setTitle("Searching for " + massFormater.format(searchedMass) + " amu");
    window.setVisible(true);

    IsotopePattern detectedPattern = peakListRow.getBestIsotopePattern();
    if ((isotopeFilter) && (detectedPattern == null)) {
      final String msg = "Cannot calculate isotope pattern scores, because selected"
          + " peak does not have any isotopes. Have you run the isotope peak grouper?";
      MZmineCore.getDesktop().displayMessage(window, msg);
    }

    try {
      String compoundIDs[] =
          gateway.findCompounds(searchedMass, mzTolerance, numOfResults, db.getParameterSet());

      // Get the number of results
      numItems = compoundIDs.length;

      if (numItems == 0) {
        window.setTitle(
            "Searching for " + massFormater.format(searchedMass) + " amu: no results found");
      }

      // Process each one of the result ID's.
      for (int i = 0; i < numItems; i++) {

        if (getStatus() != TaskStatus.PROCESSING) {
          return;
        }

        DBCompound compound = gateway.getCompound(compoundIDs[i], db.getParameterSet());

        // In case we failed to retrieve data, skip this compound
        if (compound == null)
          continue;

        String formula = compound.getPropertyValue(PeakIdentity.PROPERTY_FORMULA);

        if (formula != null) {

          // First modify the formula according to the ionization
          String adjustedFormula = FormulaUtils.ionizeFormula(formula, ionType, charge);

          logger.finest("Calculating isotope pattern for compound formula " + formula
              + " adjusted to " + adjustedFormula);

          // Generate IsotopePattern for this compound
          IsotopePattern compoundIsotopePattern = IsotopePatternCalculator
              .calculateIsotopePattern(adjustedFormula, 0.001, charge, ionType.getPolarity());

          compound.setIsotopePattern(compoundIsotopePattern);

          IsotopePattern rawDataIsotopePattern = peakListRow.getBestIsotopePattern();

          // If required, check isotope score
          if (isotopeFilter && (rawDataIsotopePattern != null)
              && (compoundIsotopePattern != null)) {

            double score = IsotopePatternScoreCalculator.getSimilarityScore(rawDataIsotopePattern,
                compoundIsotopePattern, isotopeFilterParameters);
            compound.setIsotopePatternScore(score);

            double minimumScore = isotopeFilterParameters
                .getParameter(IsotopePatternScoreParameters.isotopePatternScoreThreshold)
                .getValue();

            if (score < minimumScore) {
              finishedItems++;
              continue;
            }
          }

        }

        // Add compound to the list of possible candidate and
        // display it in window of results.
        window.addNewListItem(compound);

        // Update window title
        window.setTitle("Searching for " + massFormater.format(searchedMass) + " amu (" + (i + 1)
            + "/" + numItems + ")");

        finishedItems++;

      }

    } catch (Exception e) {
      e.printStackTrace();
      logger.log(Level.WARNING, "Could not connect to " + db, e);
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Could not connect to " + db + ": " + ExceptionUtils.exceptionToString(e));
      return;
    }

    setStatus(TaskStatus.FINISHED);
    */

    setStatus(TaskStatus.FINISHED);

  }

  private MsSpectrum buildSpectrum(DataPoint[] ms1points) {
    SimpleMsSpectrum spectrum = new SimpleMsSpectrum();
    double mz[] = new double[ms1points.length];
    float intensity[] = new float[ms1points.length];
    IonType siriusIonType = IonTypeUtil.createIonType(ionType.toString());

    for (int i = 0; i < ms1points.length; i++) {
      mz[i] = ms1points[i].getMZ();
      intensity[i] = (float) ms1points[i].getIntensity();
    }

    spectrum.setDataPoints(mz, intensity, ms1points.length);
    return spectrum;
  }

}
