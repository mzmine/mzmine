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

package io.github.mzmine.modules.dataprocessing.id_lipidid.utils;

import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_lipidid.annotation_modules.LipidAnnotationChainParameters;
import io.github.mzmine.modules.dataprocessing.id_lipidid.annotation_modules.LipidAnnotationParameters;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.LipidFragmentationRule;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.species_level.SpeciesLevelAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.ILipidClass;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidClassDescription;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public class LipidDatabaseCalculator {

  private static final LipidFactory LIPID_FACTORY = new LipidFactory();

  private int minChainLength;
  private int maxChainLength;
  private int minDoubleBonds;
  private int maxDoubleBonds;
  private Boolean onlySearchForEvenChains;
  private MZTolerance mzTolerance;
  private ILipidClass[] selectedLipids;

  private final ObservableList<LipidClassDescription> tableData = FXCollections.observableArrayList();

  public LipidDatabaseCalculator(ParameterSet parameters, ILipidClass[] selectedLipids) {

    this.minChainLength = parameters.getParameter(LipidAnnotationParameters.lipidChainParameters)
        .getEmbeddedParameters().getValue(LipidAnnotationChainParameters.minChainLength);
    this.maxChainLength = parameters.getParameter(LipidAnnotationParameters.lipidChainParameters)
        .getEmbeddedParameters().getValue(LipidAnnotationChainParameters.maxChainLength);
    this.minDoubleBonds = parameters.getParameter(LipidAnnotationParameters.lipidChainParameters)
        .getEmbeddedParameters().getValue(LipidAnnotationChainParameters.minDBEs);
    this.maxDoubleBonds = parameters.getParameter(LipidAnnotationParameters.lipidChainParameters)
        .getEmbeddedParameters().getValue(LipidAnnotationChainParameters.maxDBEs);
    this.onlySearchForEvenChains = parameters.getParameter(
            LipidAnnotationParameters.lipidChainParameters).getEmbeddedParameters()
        .getValue(LipidAnnotationChainParameters.onlySearchForEvenChainLength);
    this.mzTolerance = parameters.getValue(LipidAnnotationParameters.mzTolerance);
    this.selectedLipids = selectedLipids;
  }

  public ObservableList<LipidClassDescription> createTableData() {
    int id = 1;
    for (ILipidClass selectedLipid : selectedLipids) {
      // TODO starting point to extend for better oxidized lipid support
      int numberOfAdditionalOxygens = 0;
      int minTotalChainLength = minChainLength * selectedLipid.getChainTypes().length;
      int maxTotalChainLength = maxChainLength * selectedLipid.getChainTypes().length;
      int minTotalDoubleBonds = minDoubleBonds * selectedLipid.getChainTypes().length;
      int maxTotalDoubleBonds = maxDoubleBonds * selectedLipid.getChainTypes().length;
      for (int chainLength = minTotalChainLength; chainLength <= maxTotalChainLength;
          chainLength++) {
        if (onlySearchForEvenChains && chainLength % 2 != 0) {
          continue;
        }
        for (int chainDoubleBonds = minTotalDoubleBonds; chainDoubleBonds <= maxTotalDoubleBonds;
            chainDoubleBonds++) {

          if (chainLength / 2 < chainDoubleBonds || chainLength == 0) {
            continue;
          }
          // Prepare a lipid instance
          SpeciesLevelAnnotation lipid = LIPID_FACTORY.buildSpeciesLevelLipid(selectedLipid,
              chainLength, chainDoubleBonds, numberOfAdditionalOxygens);
          if (lipid == null) {
            continue;
          }
          List<LipidFragmentationRule> fragmentationRules = Arrays.asList(
              selectedLipid.getFragmentationRules());
          StringBuilder fragmentationRuleSB = new StringBuilder();
          fragmentationRules.stream().forEach(rule -> {
            fragmentationRuleSB.append(rule.toString()).append("\n");
          });
          StringBuilder exactMassSB = new StringBuilder();
          Set<IonizationType> ionizationTypes = fragmentationRules.stream()
              .map(LipidFragmentationRule::getIonizationType).collect(Collectors.toSet());
          for (IonizationType ionizationType : ionizationTypes) {
            double mz = MolecularFormulaManipulator.getMass(lipid.getMolecularFormula(),
                AtomContainerManipulator.MonoIsotopic) + ionizationType.getAddedMass();
            exactMassSB.append(ionizationType.getAdductName()).append(" ")
                .append(MZmineCore.getConfiguration().getMZFormat().format(mz)).append("\n");
          }
          tableData.add(new LipidClassDescription(String.valueOf(id), // id
              selectedLipid.getName(), // lipid class
              MolecularFormulaManipulator.getString(lipid.getMolecularFormula()), // molecular
              // formula
              lipid.getAnnotation(),
              // abbr
              exactMassSB.toString(), // exact mass
              // mass
              "", // info
              "", // status
              fragmentationRuleSB.toString())); // msms fragments
          id++;
        }
      }
    }
    return tableData;
  }

  public void checkInterferences() {
    for (int i = 0; i < tableData.size(); i++) {
      LipidClassDescription lipidClassDescription = tableData.get(i);
      Map<String, Double> ionSpecificMzValues = extractIonNotationMzValuesFromTable(
          lipidClassDescription);
      for (Entry<String, Double> entry : ionSpecificMzValues.entrySet()) {
        for (int j = 0; j < tableData.size(); j++) {
          if (i == j) {
            continue;
          }
          StringBuilder sb = new StringBuilder();
          LipidClassDescription lipidClassDescriptionCompare = tableData.get(j);
          Map<String, Double> ionSpecificMzValuesCompare = extractIonNotationMzValuesFromTable(
              lipidClassDescriptionCompare);
          for (Entry<String, Double> entryCompare : ionSpecificMzValuesCompare.entrySet()) {
            double valueOne = entry.getValue();
            double valueTwo = entryCompare.getValue();
            if (valueOne == valueTwo && isSamePolarity(entry.getKey(), entryCompare.getKey())) {
              if (!sb.isEmpty()) {
                sb.append("\n");
              }
              sb.append(entryCompare.getKey()).append(" interference with ")
                  .append(lipidClassDescription.getAbbreviation()).append(" ")
                  .append(entry.getKey());
            } else if (mzTolerance.checkWithinTolerance(valueOne, valueTwo) && isSamePolarity(
                entry.getKey(), entryCompare.getKey())) {
              double delta = valueOne - valueTwo;
              if (!sb.isEmpty()) {
                sb.append("\n");
              }
              sb.append(entryCompare.getKey()).append(" possible interference with ")
                  .append(lipidClassDescription.getAbbreviation()).append(" ")
                  .append(entry.getKey()).append(" \u0394 ")
                  .append(MZmineCore.getConfiguration().getMZFormat().format(delta));
            }
          }
          if (!sb.isEmpty()) {
            lipidClassDescriptionCompare.setInfo(
                lipidClassDescriptionCompare.getInfo() + "\n" + sb);
          }
        }
      }
    }
  }

  private boolean isSamePolarity(String key, String key2) {
    return ((key.contains("]+") && key2.contains("]+")) || (key.contains("]-") && key2.contains(
        "]-")));
  }

  private Map<String, Double> extractIonNotationMzValuesFromTable(
      LipidClassDescription lipidClassDescription) {
    Map<String, Double> ionSpecificMzValues = new HashMap<>();
    String allPairs = lipidClassDescription.getExactMass();
    String[] pairs = allPairs.split("\n");
    for (String s : pairs) {
      String[] pair = s.split(" ");
      if (pair.length > 1) {
        ionSpecificMzValues.put(pair[0], Double.parseDouble(pair[1]));
      }
    }
    return ionSpecificMzValues;
  }

  public ObservableList<LipidClassDescription> getTableData() {
    return tableData;
  }

  public MZTolerance getMzTolerance() {
    return mzTolerance;
  }
}
