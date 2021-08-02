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
package io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.formula.prediction;


import com.google.common.collect.Range;
import io.github.msdk.MSDKRuntimeException;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.datamodel.identities.iontype.IonNetwork;
import io.github.mzmine.datamodel.identities.iontype.IonNetworkLogic;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.modules.dataprocessing.id_formula_sort.FormulaSortParameters;
import io.github.mzmine.modules.dataprocessing.id_formula_sort.FormulaSortTask;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.ResultFormula;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.restrictions.elements.ElementalHeuristicChecker;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.restrictions.rdbe.RDBERestrictionChecker;
import io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.formula.createavgformulas.CreateAvgNetworkFormulasTask;
import io.github.mzmine.modules.tools.isotopepatternscore.IsotopePatternScoreCalculator;
import io.github.mzmine.modules.tools.isotopepatternscore.IsotopePatternScoreParameters;
import io.github.mzmine.modules.tools.isotopeprediction.IsotopePatternCalculator;
import io.github.mzmine.modules.tools.msmsscore.MSMSScore;
import io.github.mzmine.modules.tools.msmsscore.MSMSScoreCalculator;
import io.github.mzmine.modules.tools.msmsscore.MSMSScoreParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.openscience.cdk.formula.MolecularFormulaGenerator;
import org.openscience.cdk.formula.MolecularFormulaRange;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public class FormulaPredictionIonNetworkTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());
  private Range<Double> massRange;
  private MolecularFormulaRange elementCounts;
  private MolecularFormulaGenerator generator;
  private ModularFeatureList featureList;
  private boolean checkIsotopes, checkMSMS, checkRatios, checkRDBE;
  private ParameterSet isotopeParameters, msmsParameters, ratiosParameters, rdbeParameters;
  private MZTolerance mzTolerance;
  private String message;
  private int totalRows;
  private AtomicInteger finishedNets = new AtomicInteger(0);
  // correct values by ppm offset to shift correct molecular formulae to the center
  // usefull if all exact masses are shifted by 4 ppm enter -4 ppm
  private double ppmOffset;
  private double isotopeNoiseLevel;
  private double minScore;
  private double minMSMSScore;
  private boolean sortResults;
  private FormulaSortTask sorter;
  private CreateAvgNetworkFormulasTask netFormulaMerger;
  private int topNSignals;
  private boolean useTopNSignals;

  /**
   * @param parameters
   */
  public FormulaPredictionIonNetworkTask(ModularFeatureList featureList, ParameterSet parameters) {
    super(featureList.getMemoryMapStorage());
    this.featureList = featureList;
    mzTolerance =
        parameters.getParameter(FormulaPredictionIonNetworkParameters.mzTolerance).getValue();
    elementCounts =
        parameters.getParameter(FormulaPredictionIonNetworkParameters.elements).getValue();

    ppmOffset = parameters.getParameter(FormulaPredictionIonNetworkParameters.ppmOffset).getValue();

    checkRDBE =
        parameters.getParameter(FormulaPredictionIonNetworkParameters.rdbeRestrictions).getValue();
    if (checkRDBE) {
      rdbeParameters =
          parameters.getParameter(FormulaPredictionIonNetworkParameters.rdbeRestrictions)
              .getEmbeddedParameters();
    }

    checkRatios =
        parameters.getParameter(FormulaPredictionIonNetworkParameters.elementalRatios).getValue();
    if (checkRatios) {
      ratiosParameters =
          parameters.getParameter(FormulaPredictionIonNetworkParameters.elementalRatios)
              .getEmbeddedParameters();
    }

    checkIsotopes =
        parameters.getParameter(FormulaPredictionIonNetworkParameters.isotopeFilter).getValue();
    if (checkIsotopes) {
      isotopeParameters =
          parameters.getParameter(FormulaPredictionIonNetworkParameters.isotopeFilter)
              .getEmbeddedParameters();
      isotopeNoiseLevel = isotopeParameters
          .getParameter(IsotopePatternScoreParameters.isotopeNoiseLevel).getValue();
      minScore = isotopeParameters
          .getParameter(IsotopePatternScoreParameters.isotopePatternScoreThreshold).getValue();
    }

    checkMSMS =
        parameters.getParameter(FormulaPredictionIonNetworkParameters.msmsFilter).getValue();
    if (checkMSMS) {
      msmsParameters = parameters.getParameter(FormulaPredictionIonNetworkParameters.msmsFilter)
          .getEmbeddedParameters();
      minMSMSScore = msmsParameters.getParameter(MSMSScoreParameters.msmsMinScore).getValue();
      // limit to top n signals
      useTopNSignals = msmsParameters.getParameter(MSMSScoreParameters.useTopNSignals).getValue();
      topNSignals = !useTopNSignals ? -1
          : msmsParameters.getParameter(MSMSScoreParameters.useTopNSignals).getEmbeddedParameter()
              .getValue();
    }

    sortResults = parameters.getParameter(FormulaPredictionIonNetworkParameters.sorting).getValue();
    if (sortResults) {
      FormulaSortParameters sortingParam = parameters
          .getParameter(FormulaPredictionIonNetworkParameters.sorting).getEmbeddedParameters();
      sorter = new FormulaSortTask(sortingParam);
    }

    // merger to create avg formulas
    netFormulaMerger = new CreateAvgNetworkFormulasTask(sorter);
    message = "Formula Prediction (MS annotation networks)";
  }

  @Override
  public double getFinishedPercentage() {
    if (totalRows == 0) {
      return 0.0;
    }
    return finishedNets.get() / (double) totalRows;
  }

  @Override
  public String getTaskDescription() {
    return message;
  }

  /**
   * @see Runnable#run()
   */
  @Override
  public void run() {

    setStatus(TaskStatus.PROCESSING);

    // get all networks (filter out undefined ion types)
    List<IonNetwork> nets = IonNetworkLogic.streamNetworks(featureList, false)
        .filter(net -> !net.isUndefined()).collect(Collectors.toList());
    totalRows = nets.size();
    if (totalRows == 0) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("No annotation networks found in this list. Run MS annotation");
      return;
    }

    // parallel
    nets.stream().forEach(net -> {
      message = "Formula prediction on network " + net.getID();
      if (!isCanceled()) {
        predictFormulasForNetwork(net);
      }
      finishedNets.incrementAndGet();
    });

    logger.finest("Finished formula search for all networks");
    setStatus(TaskStatus.FINISHED);
  }

  public List<ResultFormula> predictFormulasForNetwork(IonNetwork net) {
    for (Entry<FeatureListRow, IonIdentity> e : net.entrySet()) {
      FeatureListRow row = e.getKey();
      IonIdentity ion = e.getValue();
      if (!ion.getIonType().isUndefinedAdduct()) {
        ion.clearMolFormulas();
        List<ResultFormula> list = predictFormulas(row, ion.getIonType());
        if (!list.isEmpty()) {
          if (sortResults && sorter != null) {
            sorter.sort(list);
          }
          ion.addMolFormulas(list);
        }
      }
    }
    // find best formula for neutral mol of network
    // add all that have the same mol formula in at least 2 different ions (rows)
    if (netFormulaMerger != null) {
      return netFormulaMerger.combineFormulasOfNetwork(net);
    }
    return null;
  }

  private List<ResultFormula> predictFormulas(FeatureListRow row, IonType ion) {
    List<ResultFormula> resultingFormulas = new ArrayList<>();
    double searchedMass = ion.getMass(row.getAverageMZ());
    // correct by ppm offset
    searchedMass += searchedMass * ppmOffset / 1E6;

    massRange = mzTolerance.getToleranceRange(searchedMass);

    IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();
    generator = new MolecularFormulaGenerator(builder, massRange.lowerEndpoint(),
        massRange.upperEndpoint(), elementCounts);

    IMolecularFormula cdkFormula;
    while ((cdkFormula = generator.getNextFormula()) != null) {
      try {
        // ionized formula
        IMolecularFormula cdkFormulaIon = ion.addToFormula(cdkFormula);

        // Mass is ok, so test other constraints
        checkConstraints(resultingFormulas, cdkFormula, cdkFormulaIon, row, ion, searchedMass);
      } catch (CloneNotSupportedException e) {
        logger.log(Level.SEVERE, "Cannot copy cdk formula", e);
        throw new MSDKRuntimeException(e);
      }
    }

    return resultingFormulas;
  }

  private void checkConstraints(List<ResultFormula> resultingFormulas,
      IMolecularFormula cdkFormulaNeutralM, IMolecularFormula cdkFormulaIon,
      FeatureListRow featureListRow, IonType ionType, double searchedMass) {
    int charge = ionType.getCharge();

    // Check elemental ratios
    if (checkRatios) {
      boolean check = ElementalHeuristicChecker.checkFormula(cdkFormulaNeutralM, ratiosParameters);
      if (!check) {
        return;
      }
    }

    Double rdbeValue = RDBERestrictionChecker.calculateRDBE(cdkFormulaNeutralM);
    // Check RDBE condition
    if (checkRDBE && (rdbeValue != null)) {
      boolean check = RDBERestrictionChecker.checkRDBE(rdbeValue, rdbeParameters);
      if (!check) {
        return;
      }
    }

    // Calculate isotope similarity score
    IsotopePattern detectedPattern = featureListRow.getBestIsotopePattern();
    IsotopePattern predictedIsotopePattern = null;
    Double isotopeScore = null;
    if ((checkIsotopes) && (detectedPattern != null)) {
      final double detectedPatternHeight = detectedPattern.getBasePeakIntensity();
      final double minPredictedAbundance = isotopeNoiseLevel / detectedPatternHeight;

      predictedIsotopePattern = IsotopePatternCalculator.calculateIsotopePattern(cdkFormulaIon,
          minPredictedAbundance, charge, ionType.getPolarity());

      isotopeScore = IsotopePatternScoreCalculator.getSimilarityScore(detectedPattern,
          predictedIsotopePattern, isotopeParameters);
      if (isotopeScore < minScore) {
        return;
      }

    }

    // MS/MS evaluation is slowest, so let's do it last
    Double msmsScore = null;
    Map<Double, String> msmsAnnotations = null;

    // there was a problem in the RoundRobinMoleculaFormulaGenerator (index out of range
    try {
      if (checkMSMS && featureListRow.getMostIntenseFragmentScan() != null) {
        Scan msmsScan = featureListRow.getMostIntenseFragmentScan();
        MassList ms2MassList = msmsScan.getMassList();
        if (ms2MassList == null) {
          setStatus(TaskStatus.ERROR);
          setErrorMessage("The MS/MS scan #" + msmsScan.getScanNumber() + " in file "
                          + msmsScan.getDataFile().getName() + " does not have a masslist");
          return;
        }

        MSMSScore score =
            MSMSScoreCalculator.evaluateMSMS(cdkFormulaIon, msmsScan, msmsParameters);

        if (score != null) {
          msmsScore = score.getScore();
          msmsAnnotations = score.getAnnotation();

          // Check the MS/MS condition
          if (msmsScore < minMSMSScore) {
            return;
          }
        }
      }
    } catch (Exception e) {
      logger.log(Level.WARNING,
          () -> MessageFormat.format(
              "Error in MS/MS score calculation for ion formula {0} (for neutral M: {1})",
              MolecularFormulaManipulator.getString(cdkFormulaIon),
              MolecularFormulaManipulator.getString(cdkFormulaNeutralM)));
    }

    // Create a new formula entry
    final ResultFormula resultEntry = new ResultFormula(cdkFormulaNeutralM, predictedIsotopePattern,
        isotopeScore, msmsScore, msmsAnnotations, searchedMass);

    // Add the new formula entry
    resultingFormulas.add(resultEntry);
  }

  @Override
  public void cancel() {
    super.cancel();

    // We need to cancel the formula generator, because searching for next
    // candidate formula may take a looong time
    if (generator != null) {
      generator.cancel();
    }

  }
}
