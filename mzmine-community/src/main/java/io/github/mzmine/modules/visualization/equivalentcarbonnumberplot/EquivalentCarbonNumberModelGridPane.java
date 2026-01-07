/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.visualization.equivalentcarbonnumberplot;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.MSMSLipidTools;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.molecular_species.MolecularSpeciesLevelAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.species_level.SpeciesLevelAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.ILipidAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.ILipidClass;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.text.Font;

public class EquivalentCarbonNumberModelGridPane extends GridPane {

  public EquivalentCarbonNumberModelGridPane(List<FeatureListRow> rowsWithLipidID,
      List<MatchedLipid> bestLipidMatches) {
    super();
    // Add rows to the GridPane
    this.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
    Platform.runLater(() -> {
      List<Node> ecnPlots = createEcnPlots(rowsWithLipidID, bestLipidMatches);
      int i = 0;
      for (Node node : ecnPlots) {
        GridPane.setHgrow(node, Priority.ALWAYS);
        GridPane.setVgrow(node, Priority.ALWAYS);
        this.add(node, 0, i);
        i++;
      }
    });

  }

  private List<Node> createEcnPlots(List<FeatureListRow> rowsWithLipidID,
      List<MatchedLipid> matchedLipids) {
    List<Node> ecnPlots = new ArrayList<>();
    Map<ILipidClass, Map<Integer, List<MatchedLipid>>> groupedLipids = matchedLipids.stream()
        .collect(
            Collectors.groupingBy(matchedLipid -> matchedLipid.getLipidAnnotation().getLipidClass(),
                Collectors.groupingBy(matchedLipid -> {
                  ILipidAnnotation lipidAnnotation = matchedLipid.getLipidAnnotation();
                  if (lipidAnnotation instanceof MolecularSpeciesLevelAnnotation molecularAnnotation) {
                    return MSMSLipidTools.getCarbonandDBEFromLipidAnnotaitonString(
                        molecularAnnotation.getAnnotation()).getValue();
                  } else if (lipidAnnotation instanceof SpeciesLevelAnnotation) {
                    return MSMSLipidTools.getCarbonandDBEFromLipidAnnotaitonString(
                        lipidAnnotation.getAnnotation()).getValue();
                  } else {
                    return -1;
                  }
                }, Collectors.toList())));

    // sort by lipid class
    Map<ILipidClass, Map<Integer, List<MatchedLipid>>> sortedGroupedLipids = new TreeMap<>(
        Comparator.comparing(ILipidClass::toString));
    sortedGroupedLipids.putAll(groupedLipids);

    // sort classes by DBEs
    sortedGroupedLipids.forEach((key, value) -> {
      Map<Integer, List<MatchedLipid>> sortedInnerMap = new TreeMap<>(Comparator.naturalOrder());
      sortedInnerMap.putAll(value);
      value.clear();
      value.putAll(sortedInnerMap);
    });

    for (Entry<ILipidClass, Map<Integer, List<MatchedLipid>>> entry : sortedGroupedLipids.entrySet()) {
      Label titleLabel = new Label(entry.getKey().getName());
      titleLabel.setFont(
          new Font(MZmineCore.getConfiguration().getDefaultChartTheme().getMasterFont().getName(),
              20));
      BorderPane borderPane = new BorderPane();
      GridPane.setHgrow(borderPane, Priority.ALWAYS);
      GridPane.setVgrow(borderPane, Priority.ALWAYS);
      borderPane.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
      borderPane.setTop(titleLabel);

      // Create a new GridPane for each lipid class
      GridPane lipidClassTitlePane = new GridPane();
      lipidClassTitlePane.setHgap(5);
      lipidClassTitlePane.setVgap(5);
      GridPane.setHgrow(lipidClassTitlePane, Priority.ALWAYS);
      GridPane.setVgrow(lipidClassTitlePane, Priority.ALWAYS);

      int i = 0;
      for (Entry<Integer, List<MatchedLipid>> integerListEntry : entry.getValue().entrySet()) {
        if (integerListEntry.getValue().size() > 2) {
          EquivalentCarbonNumberDataset ecnDataset = new EquivalentCarbonNumberDataset(
              rowsWithLipidID, rowsWithLipidID.toArray(new FeatureListRow[0]), entry.getKey(),
              integerListEntry.getKey());
          ecnDataset.run();
          EquivalentCarbonNumberChart equivalentCarbonNumberChart = new EquivalentCarbonNumberChart(
              "ECN Model", "Retention time", "Number of Carbons", ecnDataset);
          equivalentCarbonNumberChart.setMinSize(300, 200);
          equivalentCarbonNumberChart.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

          EquivalentCarbonNumberChartPane equivalentCarbonNumberChartPane = new EquivalentCarbonNumberChartPane(
              equivalentCarbonNumberChart, integerListEntry.getKey(), integerListEntry.getValue());
          GridPane.setHgrow(equivalentCarbonNumberChart, Priority.ALWAYS);
          GridPane.setVgrow(equivalentCarbonNumberChart, Priority.ALWAYS);
          GridPane.setHgrow(equivalentCarbonNumberChartPane, Priority.ALWAYS);
          GridPane.setVgrow(equivalentCarbonNumberChartPane, Priority.ALWAYS);
          lipidClassTitlePane.add(equivalentCarbonNumberChartPane, i % 2, i / 2 + 1);
          i++;
        }
      }
      borderPane.setCenter(lipidClassTitlePane);

      // Add the inner GridPane to the list of plots
      if (lipidClassTitlePane.getChildren().size() > 1) {
        ecnPlots.add(borderPane);
      }
    }
    return ecnPlots;
  }

}
