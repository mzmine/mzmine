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
import io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.ADAPChromatogramBuilderParameters;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.restrictions.elements.ElementalHeuristicChecker;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.restrictions.rdbe.RDBERestrictionChecker;
import io.github.mzmine.parameters.ParameterUtils;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
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
  private final MZTolerance mzTolerenace;
  private final ListView<String> listView;
  private final ObservableList<String> itemList;
  private Task<ObservableList<String>> loadTask;

  public RepeatingUnitSuggester(FeatureList featureList) {
    this.featureList = featureList;
    listView = new ListView<>();
    itemList = FXCollections.observableArrayList();
    listView.setItems(itemList);
    listView.setPrefHeight(300);
    mzTolerenace = extractMzToleranceFromPreviousMethods();
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
    Map<Double, Integer> deltaFrequency = calculateDeltaFrequencies(mzValues);

    List<Double> topFiveDeltas = findTopDeltas(deltaFrequency, 5);
    List<Double> filteredDeltas = filterMultimers(topFiveDeltas);

    for (Double delta : filteredDeltas) {
      String predictedFormula = predictFormula(delta);
      if (!predictedFormula.isBlank() && !list.contains(predictedFormula)) {
        list.add(predictedFormula + " (m/z Δ " + delta + ")");
      }
    }
    return list;
  }

  /*This method takes into account the detected charge state of a feature list row.
   If the charge was detected it will be considered when calculating the repeating unit*/
  private double[] extractMzValues() {
    double[] mzValues = featureList.getRows().stream()
        .flatMapToDouble(row -> DoubleStream.of(row.getAverageMZ())).toArray();
    int[] charges = featureList.getRows().stream()
        .flatMapToInt(row -> IntStream.of(row.getRowCharge())).toArray();

    if (mzValues.length != charges.length) {
      throw new IllegalArgumentException(
          "The length of mzValues and charge arrays must be the same");
    }

    double[] products = new double[mzValues.length];

    for (int i = 0; i < mzValues.length; i++) {
      int charge = 1;
      if (charges[i] != 0) {
        charge = charges[i];
      }
      products[i] = mzValues[i] * charge;
    }
    return products;
  }

  private Map<Double, Integer> calculateDeltaFrequencies(double[] masses) {
    Map<Double, Integer> frequencyMap = new HashMap<>();
    for (int i = 0; i < masses.length; i++) {
      for (int j = i + 1; j < masses.length; j++) {
        double delta = Math.abs(
            Math.round((masses[j] - masses[i]) * 1000) / 1000.0); // Round to 3 decimal places
        if (delta > 0) {
          frequencyMap.merge(delta, 1, Integer::sum);
        }
      }
    }
    return frequencyMap;
  }

  private List<Double> findTopDeltas(Map<Double, Integer> frequencyMap, int topN) {
    return frequencyMap.entrySet().stream()
        .sorted(Map.Entry.<Double, Integer>comparingByValue().reversed()).limit(topN)
        .map(Map.Entry::getKey).collect(Collectors.toList());
  }

  private String predictFormula(double mass) {
    IChemObjectBuilder builder = DefaultChemObjectBuilder.getInstance();
    double minMass = mzTolerenace.getToleranceRange(mass).lowerEndpoint();
    double maxMass = mzTolerenace.getToleranceRange(mass).upperEndpoint();

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
      mfRange.addIsotope(c, 0, 50);
      mfRange.addIsotope(h, 0, 100);
      mfRange.addIsotope(n, 0, 50);
      mfRange.addIsotope(o, 0, 50);
      mfRange.addIsotope(p, 0, 10);
      mfRange.addIsotope(s, 0, 10);
      mfRange.addIsotope(f, 0, 100);

      MolecularFormulaGenerator mfg = new MolecularFormulaGenerator(builder, minMass, maxMass,
          mfRange);
      IMolecularFormula bestFormula = null;
      double bestMassError = Double.MAX_VALUE;

      IMolecularFormula formula;
      while ((formula = mfg.getNextFormula()) != null) {
        double formulaMass = MolecularFormulaManipulator.getMass(formula);
        double massError = Math.abs(formulaMass - mass);

        Double rdbeValue = RDBERestrictionChecker.calculateRDBE(formula);
        boolean rdbeValid = rdbeValue != null && RDBERestrictionChecker.checkRDBE(rdbeValue,
            Range.closed(0.0, 20.0), false);
        boolean elementValid = ElementalHeuristicChecker.checkFormula(formula, true, true, true);
        if (formula.contains(f)) {
          elementValid = true;
        }
        if (massError < bestMassError && rdbeValid && elementValid) {
          bestMassError = massError;
          bestFormula = formula;
        }
      }

      if (bestFormula != null) {
        return MolecularFormulaManipulator.getString(bestFormula);
      } else {
        logger.log(Level.WARNING, "No valid formula found for mass: " + mass);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return "";
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
        < mzTolerenace.getMzTolerance(); // Check if delta is an approximate integer multiple of baseDelta
  }

  /*
   *  Extract mz tolerance from ADAP Chromatogram Builder
   * */
  private MZTolerance extractMzToleranceFromPreviousMethods() {

    try {
      Collection<FeatureListAppliedMethod> appliedMethods = Objects.requireNonNull(featureList)
          .getAppliedMethods();

      boolean hasChromatograms = appliedMethods.stream().anyMatch(
          appliedMethod -> appliedMethod.getParameters().getClass()
              .equals(ADAPChromatogramBuilderParameters.class));
      if (hasChromatograms) {
        return ParameterUtils.getValueFromAppliedMethods(appliedMethods,
                ADAPChromatogramBuilderParameters.class, ADAPChromatogramBuilderParameters.mzTolerance)
            .orElse(new MZTolerance(0.005, 15));
      }
    } catch (Exception e) {
      logger.log(Level.WARNING,
          " Could not extract previously used mz tolerance, will apply default settings. "
              + e.getMessage());
    }
    return new MZTolerance(0.005, 15);
  }

}