/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.modules.visualization.kendrickmassplot;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.ADAPChromatogramBuilderParameters;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.restrictions.elements.ElementalHeuristicChecker;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.restrictions.rdbe.RDBERestrictionChecker;
import io.github.mzmine.parameters.ParameterUtils;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.MathUtils;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleCollection;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.concurrent.Task;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.config.Isotopes;
import org.openscience.cdk.formula.MolecularFormulaGenerator;
import org.openscience.cdk.formula.MolecularFormulaRange;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.interfaces.IIsotope;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public class RepeatingUnitSuggester extends Task<List<RepeatingUnit>> {

  private static final Logger logger = Logger.getLogger(RepeatingUnitSuggester.class.getName());

  private final FeatureList featureList;
  private final MZTolerance mzTolerance;

  public RepeatingUnitSuggester(FeatureList featureList) {
    this.featureList = featureList;
    mzTolerance = extractMzToleranceFromPreviousMethods();
  }

  /**
   * Creates a new suggester, sets a callback consumer for the results on success and starts on a
   * new thread.
   *
   * @param resultConsumerOnFxThread consumes results only on success. Called from fx Thread
   * @return this task in case error handling needs to be added
   */
  public static Task<List<RepeatingUnit>> createOnNewThread(FeatureList featureList,
      Consumer<List<RepeatingUnit>> resultConsumerOnFxThread) {
    var suggester = new RepeatingUnitSuggester(featureList);
    suggester.setOnSucceeded(_ -> resultConsumerOnFxThread.accept(suggester.getValue()));
    // run prediction after setting consumer
    suggester.predictOnNewThread();
    return suggester;
  }

  public void predictOnNewThread() {
    new Thread(this).start();
  }

  private List<RepeatingUnit> suggestRepeatingUnit() {
    //get transformed mzValues
    double[] mzValues = extractMzValues();

    // Calculate the frequency of all m/z deltas
    Map<Double, DoubleList> deltaMap = calculateDeltaFrequencies(mzValues);

    List<Double> topFiveDeltas = findTopNDeltaMedians(deltaMap, 5);
    List<Double> filteredDeltas = filterMultimers(topFiveDeltas);

    Set<RepeatingUnit> uniqueFormulas = new HashSet<>();
    for (Double delta : filteredDeltas) {
      List<RepeatingUnit> predictedFormulas = predictFormula(delta);
      uniqueFormulas.addAll(predictedFormulas);
    }
    return new ArrayList<>(uniqueFormulas);
  }

  /*This method takes into account the detected charge state of a feature list row.
   If the charge was detected it will be considered when calculating the repeating unit*/
  private double[] extractMzValues() {
    double[] mzValues = featureList.getRows().stream().mapToDouble(FeatureListRow::getAverageMZ)
        .toArray();
    int[] charges = featureList.getRows().stream().mapToInt(FeatureListRow::getRowCharge).toArray();

    if (mzValues.length != charges.length) {
      logger.log(Level.WARNING, "The length of mzValues and charge arrays must be the same");
      throw new IllegalArgumentException(
          "The length of mzValues and charge arrays must be the same");
    }

    double[] neutralMasses = new double[mzValues.length];

    for (int i = 0; i < mzValues.length; i++) {
      int charge = 1;
      if (charges[i] != 0) {
        charge = charges[i];
      }
      neutralMasses[i] = mzValues[i] * charge;
    }
    return neutralMasses;
  }

  private Map<Double, DoubleList> calculateDeltaFrequencies(double[] masses) {
    Map<Double, DoubleList> deltaMap = new HashMap<>();
    double tolerance = mzTolerance.getMzTolerance();
    if (tolerance == 0) {
      tolerance = mzTolerance.getMzToleranceForMass(200);
    }

    // set a limit of maximum features to compare
    int maxFeaturesToCheck = Math.min(30000, masses.length);
    for (int i = 0; i < maxFeaturesToCheck - 1; i++) {
      for (int j = i + 1; j < maxFeaturesToCheck; j++) {
        double delta = Math.round(Math.abs(masses[j] - masses[i]) / tolerance) * tolerance;
        if (delta > 0) {
          deltaMap.computeIfAbsent(delta, _ -> new DoubleArrayList()).add(delta);
        }
      }
    }
    return deltaMap;
  }

  private List<Double> findTopNDeltaMedians(Map<Double, DoubleList> deltaMap, int topN) {
    // Get the top N entries based on the size of the DoubleList
    return deltaMap.values().stream()
        .sorted(Comparator.comparing(List::size, Comparator.reverseOrder()))
        // only topN deltas with most occurrence
        .limit(topN)
        // calculate median of mz
        .map(DoubleCollection::toDoubleArray).map(MathUtils::calcMedian).toList();
  }


  private List<RepeatingUnit> predictFormula(double mass) {
    IChemObjectBuilder builder = DefaultChemObjectBuilder.getInstance();
    double minMass = mzTolerance.getToleranceRange(mass).lowerEndpoint();
    double maxMass = mzTolerance.getToleranceRange(mass).upperEndpoint();

    try {
      Isotopes ifac = Isotopes.getInstance();
      IIsotope c = ifac.getMajorIsotope("C");
      IIsotope h = ifac.getMajorIsotope("H");
      IIsotope n = ifac.getMajorIsotope("N");
      IIsotope o = ifac.getMajorIsotope("O");
      IIsotope p = ifac.getMajorIsotope("P");
      IIsotope s = ifac.getMajorIsotope("S");
      IIsotope f = ifac.getMajorIsotope("F");

      MolecularFormulaRange mfRange = new MolecularFormulaRange();
      mfRange.addIsotope(c, 0, 20);
      mfRange.addIsotope(h, 0, 50);
      mfRange.addIsotope(n, 0, 10);
      mfRange.addIsotope(o, 0, 10);
      mfRange.addIsotope(p, 0, 5);
      mfRange.addIsotope(s, 0, 5);
      mfRange.addIsotope(f, 0, 10);

      MolecularFormulaGenerator mfg = new MolecularFormulaGenerator(builder, minMass, maxMass,
          mfRange);
      List<RepeatingUnit> bestFormulas = new ArrayList<>();

      IMolecularFormula formula;
      while ((formula = mfg.getNextFormula()) != null) {
        Double rdbeValue = RDBERestrictionChecker.calculateRDBE(formula);
        boolean rdbeValid = rdbeValue != null && RDBERestrictionChecker.checkRDBE(rdbeValue,
            Range.closed(0.0, 20.0), false);
        boolean elementValid = ElementalHeuristicChecker.checkFormula(formula, true, true, true);
        boolean nitrogenRuleValid = passesSimpleNitrogenHeuristic(formula, ifac);
        if (formula.contains(f)) {
          elementValid = true;
        }
        if (rdbeValid && elementValid && nitrogenRuleValid) {
          bestFormulas.add(RepeatingUnit.create(formula, mass));
        }
      }

      if (bestFormulas.isEmpty()) {
        logger.info("No valid formula found for mass: " + mass);
      }
      return bestFormulas;
    } catch (Exception e) {
      logger.log(Level.WARNING, e.getMessage(), e);
    }
    return List.of();
  }

  private boolean passesSimpleNitrogenHeuristic(IMolecularFormula formula, Isotopes ifac) {
    return MolecularFormulaManipulator.getElementCount(formula, ifac.getMajorIsotope("N"))
        <= MolecularFormulaManipulator.getElementCount(formula, ifac.getMajorIsotope("C"));
  }


  private List<Double> filterMultimers(List<Double> deltas) {
    if (deltas.isEmpty()) {
      return deltas;
    }

    // Get the first delta (this will always be included in the result list)
    Double firstDelta = deltas.getFirst();

    // Filter the remaining deltas
    List<Double> filteredDeltas = deltas.stream().skip(1)
        .filter(delta -> deltas.stream().noneMatch(otherDelta -> isMultimer(delta, otherDelta)))
        .collect(Collectors.toList());

    // Add the first delta at the beginning of the list
    filteredDeltas.addFirst(firstDelta);
    return filteredDeltas;
  }

  private boolean isMultimer(double delta, double baseDelta) {
    if (delta <= baseDelta) {
      return false; // delta must be greater than baseDelta to be a potential multimer
    }
    double ratio = delta / baseDelta;
    return Math.abs(ratio - Math.round(ratio))
        < mzTolerance.getMzTolerance(); // Check if delta is an approximate integer multiple of baseDelta
  }

  /*
   *  Extract mz tolerance from ADAP Chromatogram Builder
   * */
  private MZTolerance extractMzToleranceFromPreviousMethods() {

    try {
      Collection<FeatureListAppliedMethod> appliedMethods = Objects.requireNonNull(featureList)
          .getAppliedMethods();

      return ParameterUtils.getValueFromAppliedMethods(appliedMethods,
              ADAPChromatogramBuilderParameters.class, ADAPChromatogramBuilderParameters.mzTolerance)
          .orElse(new MZTolerance(0.005, 15));
    } catch (Exception e) {
      logger.log(Level.WARNING,
          " Could not extract previously used mz tolerance, will apply default settings. "
              + e.getMessage());
    }
    return new MZTolerance(0.005, 15);
  }

  @Override
  protected List<RepeatingUnit> call() throws Exception {
    return suggestRepeatingUnit();
  }
}