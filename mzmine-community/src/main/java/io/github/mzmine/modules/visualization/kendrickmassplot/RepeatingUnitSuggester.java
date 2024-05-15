/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.ADAPChromatogramBuilderParameters;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.restrictions.elements.ElementalHeuristicChecker;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.restrictions.rdbe.RDBERestrictionChecker;
import io.github.mzmine.parameters.ParameterUtils;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.MathUtils;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleCollection;
import it.unimi.dsi.fastutil.doubles.DoubleComparator;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.ListView;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.config.Isotopes;
import org.openscience.cdk.formula.MolecularFormulaGenerator;
import org.openscience.cdk.formula.MolecularFormulaRange;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.interfaces.IIsotope;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public class RepeatingUnitSuggester {

  private final Logger logger = Logger.getLogger(this.getClass().getName());

  private final FeatureList featureList;
  private final MZTolerance mzTolerance;
  private final ListView<String> listView;
  private final ObservableList<String> itemList;
  private Task<ObservableList<String>> loadTask;

  public RepeatingUnitSuggester(FeatureList featureList) {
    this.featureList = featureList;
    listView = new ListView<>();
    itemList = FXCollections.observableArrayList();
    listView.setItems(itemList);
    listView.setPrefHeight(300);
    mzTolerance = extractMzToleranceFromPreviousMethods();
    loadItems();
  }

  private void loadItems() {
    loadTask = new Task<>() {
      @Override
      protected ObservableList<String> call() {
        return suggestRepeatingUnit();
      }

      @Override
      protected void succeeded() {
        super.succeeded();
        itemList.setAll(getValue());
      }

    };

    new Thread(loadTask).start();
  }

  public Task<ObservableList<String>> getLoadItemsTask() {
    return loadTask;
  }

  public ListView<String> getListView() {
    return listView;
  }

  private ObservableList<String> suggestRepeatingUnit() {
    ObservableList<String> list = FXCollections.observableArrayList();
    //get transformed mzValues
    double[] mzValues = extractMzValues();

    // Calculate the frequency of all m/z deltas
    Map<Double, DoubleList> deltaMap = calculateDeltaFrequencies(mzValues);

    List<Double> topFiveDeltas = findTopNDeltaMedians(deltaMap, 5);
    List<Double> filteredDeltas = filterMultimers(topFiveDeltas);

    for (Double delta : filteredDeltas) {
      List<String> predictedFormulas = predictFormula(delta);
      for (String predictedFormula : predictedFormulas) {
      if (!predictedFormula.isBlank() && !list.contains(predictedFormula)) {
        list.add(delta + ": " + predictedFormula);
      }
      }

    }
    return list;
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
        .sorted(Comparator.comparingInt(list -> list.size()))
        // only topN deltas with most occurrence
        .limit(topN)
        // calculate median of mz
        .map(DoubleCollection::toDoubleArray)
        .map(MathUtils::calcMedian)
        .toList();
  }

  private List<String> predictFormula(double mass) {
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
      Map<IMolecularFormula, Double> bestFormulas = new HashMap<>();

      IMolecularFormula formula;
      while ((formula = mfg.getNextFormula()) != null) {
        double formulaMass = MolecularFormulaManipulator.getMass(formula);
        double massError = Math.abs(formulaMass - mass);

        Double rdbeValue = RDBERestrictionChecker.calculateRDBE(formula);
        boolean rdbeValid = rdbeValue != null && RDBERestrictionChecker.checkRDBE(rdbeValue,
            Range.closed(0.0, 20.0), false);
        boolean elementValid = ElementalHeuristicChecker.checkFormula(formula, true, true, true);
        boolean nitrogenRuleValid = passesSimpleNitrogenHeuristic(formula, ifac);
        if (formula.contains(f)) {
          elementValid = true;
        }
        if (rdbeValid && elementValid && nitrogenRuleValid) {
          bestFormulas.put(formula, massError);
        }
      }

      if (!bestFormulas.isEmpty()) {
        List<String> bestFormulasStrings = new ArrayList<>();
        for (Entry<IMolecularFormula, Double> entry : bestFormulas.entrySet()) {
          bestFormulasStrings.add(MolecularFormulaManipulator.getString(entry.getKey()) + " (Δ "
              + MZmineCore.getConfiguration().getMZFormat().format(entry.getValue()) + ")");
        }
        return bestFormulasStrings;

      } else {
        logger.log(Level.WARNING, "No valid formula found for mass: " + mass);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return new ArrayList<>();
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

}