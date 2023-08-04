/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.formula.prediction;


import com.google.common.collect.Range;
import io.github.msdk.MSDKRuntimeException;
import io.github.mzmine.datamodel.DataPoint;
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
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.restrictions.elements.ElementalHeuristicParameters;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.restrictions.rdbe.RDBERestrictionChecker;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.restrictions.rdbe.RDBERestrictionParameters;
import io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.formula.createavgformulas.CreateAvgNetworkFormulasTask;
import io.github.mzmine.modules.tools.isotopepatternscore.IsotopePatternScoreCalculator;
import io.github.mzmine.modules.tools.isotopepatternscore.IsotopePatternScoreParameters;
import io.github.mzmine.modules.tools.isotopeprediction.IsotopePatternCalculator;
import io.github.mzmine.modules.tools.msmsscore.MSMSScore;
import io.github.mzmine.modules.tools.msmsscore.MSMSScoreCalculator;
import io.github.mzmine.modules.tools.msmsscore.MSMSScoreParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.OptionForValues;
import io.github.mzmine.parameters.parametertypes.ValueOption;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.formula.MolecularFormulaGenerator;
import org.openscience.cdk.formula.MolecularFormulaRange;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public class FormulaPredictionIonNetworkTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(
      FormulaPredictionIonNetworkTask.class.getName());
  private final Double minIsotopeScore;
  private final MZTolerance isotopeMZTolerance;
  private final MolecularFormulaRange elementCounts;
  private final ModularFeatureList featureList;
  private final boolean checkIsotopes;
  private final boolean checkMSMS;
  private final boolean checkRatios;
  private final boolean checkRDBE;
  private final MZTolerance mzTolerance;
  private final AtomicInteger finishedNets = new AtomicInteger(0);
  // correct values by ppm offset to shift correct molecular formulae to the center
  // usefull if all exact masses are shifted by 4 ppm enter -4 ppm
  private final double ppmOffset;
  private final Double isotopeNoiseLevel;
  private final boolean sortResults;
  private final CreateAvgNetworkFormulasTask netFormulaMerger;
  private final OptionForValues handleHigherMz;
  private final HashMap<IMolecularFormula, IsotopePattern> predictedPattern = new HashMap<>(1000);
  private MolecularFormulaGenerator generator;
  private String message;
  private int totalRows;
  private FormulaSortTask sorter;
  private Range<Double> rdbeRange;
  private Boolean rdbeIsInteger;
  private Boolean checkHCRatio;
  private Boolean checkMultipleRatios;
  private Boolean checkNOPSRatio;
  private Double msmsMinScore;
  private int topNmsmsSignals;
  private MZTolerance msmsMzTolerance;

  public FormulaPredictionIonNetworkTask(ModularFeatureList featureList, ParameterSet parameters,
      @NotNull Instant moduleCallDate) {
    super(featureList.getMemoryMapStorage(), moduleCallDate);
    this.featureList = featureList;
    mzTolerance = parameters.getValue(FormulaPredictionIonNetworkParameters.mzTolerance);
    elementCounts = parameters.getValue(FormulaPredictionIonNetworkParameters.elements);
    ppmOffset = parameters.getValue(FormulaPredictionIonNetworkParameters.ppmOffset);

    handleHigherMz = parameters.getValue(FormulaPredictionIonNetworkParameters.handleHigherMz);

    checkIsotopes = parameters.getParameter(FormulaPredictionIonNetworkParameters.isotopeFilter)
        .getValue();
    final ParameterSet isoParam = parameters.getParameter(
        FormulaPredictionIonNetworkParameters.isotopeFilter).getEmbeddedParameters();

    if (checkIsotopes) {
      minIsotopeScore = isoParam.getValue(
          IsotopePatternScoreParameters.isotopePatternScoreThreshold);
      isotopeNoiseLevel = isoParam.getValue(IsotopePatternScoreParameters.isotopeNoiseLevel);
      isotopeMZTolerance = isoParam.getValue(IsotopePatternScoreParameters.mzTolerance);
    } else {
      minIsotopeScore = null;
      isotopeNoiseLevel = null;
      isotopeMZTolerance = null;
    }

    sortResults = parameters.getValue(FormulaPredictionIonNetworkParameters.sorting);
    if (sortResults) {
      FormulaSortParameters sortingParam = parameters.getParameter(
          FormulaPredictionIonNetworkParameters.sorting).getEmbeddedParameters();
      sorter = new FormulaSortTask(sortingParam, getModuleCallDate());
    }

    // merger to create avg formulas
    netFormulaMerger = new CreateAvgNetworkFormulasTask(sorter, moduleCallDate);
    message = "Formula Prediction (MS annotation networks)";

    checkMSMS = parameters.getParameter(FormulaPredictionIonNetworkParameters.msmsFilter)
        .getValue();
    if (checkMSMS) {
      ParameterSet msmsParam = parameters.getParameter(
          FormulaPredictionIonNetworkParameters.msmsFilter).getEmbeddedParameters();

      msmsMinScore = msmsParam.getValue(MSMSScoreParameters.msmsMinScore);
      topNmsmsSignals =
          msmsParam.getValue(MSMSScoreParameters.useTopNSignals) ? msmsParam.getParameter(
              MSMSScoreParameters.useTopNSignals).getEmbeddedParameter().getValue() : -1;
      msmsMzTolerance = msmsParam.getValue(MSMSScoreParameters.msmsTolerance);
    }

    checkRDBE = parameters.getParameter(FormulaPredictionIonNetworkParameters.rdbeRestrictions)
        .getValue();
    if (checkRDBE) {
      ParameterSet rdbeParameters = parameters.getParameter(
          FormulaPredictionIonNetworkParameters.rdbeRestrictions).getEmbeddedParameters();
      rdbeRange = rdbeParameters.getValue(RDBERestrictionParameters.rdbeRange);
      rdbeIsInteger = rdbeParameters.getValue(RDBERestrictionParameters.rdbeWholeNum);
    }

    checkRatios = parameters.getParameter(FormulaPredictionIonNetworkParameters.elementalRatios)
        .getValue();
    if (checkRatios) {
      final ParameterSet elementRatiosParam = parameters.getParameter(
          FormulaPredictionIonNetworkParameters.elementalRatios).getEmbeddedParameters();
      checkHCRatio = elementRatiosParam.getValue(ElementalHeuristicParameters.checkHC);
      checkMultipleRatios = elementRatiosParam.getValue(ElementalHeuristicParameters.checkMultiple);
      checkNOPSRatio = elementRatiosParam.getValue(ElementalHeuristicParameters.checkNOPS);
    }
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
    List<IonNetwork> nets = IonNetworkLogic.streamNetworks(featureList, true)
        .filter(net -> !net.isUndefined()).toList();
    totalRows = nets.size();
    if (totalRows == 0) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("No annotation networks found in this list. Run MS annotation");
      return;
    }

    // parallel
    nets.forEach(net -> {
      message = "Formula prediction on network " + net.getID();
      if (!isCanceled()) {
        predictFormulasForNetwork(net);
      }
      finishedNets.incrementAndGet();
    });

    logger.finest("Finished formula search for all networks");
    setStatus(TaskStatus.FINISHED);
  }

  @Nullable
  public List<ResultFormula> predictFormulasForNetwork(IonNetwork net) {
    if (handleHigherMz.option() != ValueOption.INCLUDE && handleHigherMz.checkValue(
        net.getNeutralMass())) {
      if (handleHigherMz.option() == ValueOption.EXCLUDE) {
        return null;
      } else if (handleHigherMz.option() == ValueOption.SIMPLIFY) {
        // predict formulas only for network and process all formula
        // sorts and adds them to the ions
        predictFormulasForWholeNetwork(net, sortResults, true);
      }
    } else {
      // run on all rows
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

    Range<Double> massRange = mzTolerance.getToleranceRange(searchedMass);

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

  /**
   * Predict formulas only for the neutral mass of the network. Then score all rows. This is
   * simplified. Some rows might be missing formulas
   *
   * @param net      network to predict formulas for
   * @param sort     sort results
   * @param addToIon add formulas to ions
   */
  private void predictFormulasForWholeNetwork(IonNetwork net, boolean sort, boolean addToIon) {
    final var entries = new ArrayList<>(net.entrySet());

    ArrayList<ResultFormula>[] resultingFormulas = new ArrayList[net.size()];
    for (int i = 0; i < net.size(); i++) {
      resultingFormulas[i] = new ArrayList<>();
    }

    double searchedMass = net.getNeutralMass();
    // correct by ppm offset
    searchedMass += searchedMass * ppmOffset / 1E6;

    Range<Double> massRange = mzTolerance.getToleranceRange(searchedMass);

    IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();
    generator = new MolecularFormulaGenerator(builder, massRange.lowerEndpoint(),
        massRange.upperEndpoint(), elementCounts);

    IMolecularFormula cdkFormula;
    while ((cdkFormula = generator.getNextFormula()) != null) {
      for (int i = 0; i < net.size(); i++) {
        try {
          final FeatureListRow row = entries.get(i).getKey();
          final IonType ion = entries.get(i).getValue().getIonType();
          if (ion.isUndefinedAdduct()) {
            continue;
          }
          // ionized formula
          IMolecularFormula cdkFormulaIon = ion.addToFormula(cdkFormula);

          // correct by ppm offset
          double rowMass = ion.getMass(row.getAverageMZ());
          rowMass += rowMass * ppmOffset / 1E6;

          // Mass is ok, so test other constraints
          checkConstraints(resultingFormulas[i], cdkFormula, cdkFormulaIon, row, ion, rowMass);
        } catch (CloneNotSupportedException e) {
          logger.log(Level.SEVERE, "Cannot copy cdk formula", e);
          throw new MSDKRuntimeException(e);
        }
      }
    }

    for (int i = 0; i < net.size(); i++) {
      final FeatureListRow row = entries.get(i).getKey();
      final IonIdentity ion = entries.get(i).getValue();
      if (!ion.getIonType().isUndefinedAdduct()) {
        ion.clearMolFormulas();
        List<ResultFormula> list = resultingFormulas[i];
        if (!list.isEmpty()) {
          if (sort && sortResults && sorter != null) {
            sorter.sort(list);
          }
          if (addToIon) {
            ion.addMolFormulas(list);
          }
        }
      }
    }
  }

  private void checkConstraints(List<ResultFormula> resultingFormulas,
      IMolecularFormula cdkFormulaNeutralM, IMolecularFormula cdkFormulaIon,
      FeatureListRow featureListRow, IonType ionType, double searchedMass) {
    int charge = ionType.getCharge();

    // Check elemental ratios
    if (checkRatios) {
      boolean check = ElementalHeuristicChecker.checkFormula(cdkFormulaNeutralM, checkHCRatio,
          checkNOPSRatio, checkMultipleRatios);
      if (!check) {
        return;
      }
    }

    Double rdbeValue = RDBERestrictionChecker.calculateRDBE(cdkFormulaNeutralM);
    // Check RDBE condition
    if (checkRDBE && (rdbeValue != null)) {
      boolean check = RDBERestrictionChecker.checkRDBE(rdbeValue, rdbeRange, rdbeIsInteger);
      if (!check) {
        return;
      }
    }

    // Calculate isotope similarity score
    IsotopePattern detectedPattern = featureListRow.getBestIsotopePattern();
    IsotopePattern predictedIsotopePattern = null;
    Float isotopeScore = null;
    if ((checkIsotopes) && (detectedPattern != null)) {
      final double detectedPatternHeight = detectedPattern.getBasePeakIntensity();
      final double minPredictedAbundance = isotopeNoiseLevel / detectedPatternHeight;

      predictedIsotopePattern = predictedPattern.computeIfAbsent(cdkFormulaIon,
          key -> IsotopePatternCalculator.calculateIsotopePattern(cdkFormulaIon,
              minPredictedAbundance, charge, ionType.getPolarity()));

      isotopeScore = IsotopePatternScoreCalculator.getSimilarityScore(detectedPattern,
          predictedIsotopePattern, isotopeMZTolerance, isotopeNoiseLevel);
      if (isotopeScore < minIsotopeScore) {
        return;
      }

    }

    // MS/MS evaluation is slowest, so let's do it last
    Float msmsScore = null;
    Map<DataPoint, String> msmsAnnotations = null;

    // there was a problem in the RoundRobinMoleculaFormulaGenerator (index out of range
    try {
      if (checkMSMS && featureListRow.getMostIntenseFragmentScan() != null) {
        Scan msmsScan = featureListRow.getMostIntenseFragmentScan();
        MassList ms2MassList = msmsScan.getMassList();
        if (ms2MassList == null) {
          setStatus(TaskStatus.ERROR);
          setErrorMessage(
              "The MS/MS scan #" + msmsScan.getScanNumber() + " in file " + msmsScan.getDataFile()
                  .getName() + " does not have a masslist");
          return;
        }

        MSMSScore score = MSMSScoreCalculator.evaluateMSMS(cdkFormulaIon, msmsScan, msmsMzTolerance,
            topNmsmsSignals);

        if (score != null) {
          msmsScore = score.explainedIntensity();
          msmsAnnotations = score.annotation();

          // Check the MS/MS condition
          if (msmsScore < msmsMinScore) {
            return;
          }
        }
      }
    } catch (Exception e) {
      logger.log(Level.WARNING, () -> MessageFormat.format(
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
