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

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.annotations.LipidMatchListType;
import io.github.mzmine.gui.mainwindow.MZmineTab;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;
import io.github.mzmine.parameters.ParameterSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javafx.collections.ObservableList;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import org.jetbrains.annotations.NotNull;

public class EquivalentCarbonNumberTab extends MZmineTab {

  private final ParameterSet parameters;
  private final FeatureList featureList;
  private final ObservableList<FeatureListRow> featureListRows;

  public EquivalentCarbonNumberTab(String title, boolean showBinding, boolean defaultBindingState,
      ParameterSet parameters, FeatureList featureList) {
    super(title, showBinding, defaultBindingState);
    this.parameters = parameters;
    this.featureList = featureList;
    this.featureListRows = featureList.getRows();
    init();
  }

  public EquivalentCarbonNumberTab(String title, ParameterSet parameters, FeatureList featureList) {
    super(title, true, false);
    this.parameters = parameters;
    this.featureList = featureList;
    this.featureListRows = featureList.getRows();
    init();
  }


  public EquivalentCarbonNumberTab(String title, ParameterSet parameters, FeatureList featureList,
      ObservableList<FeatureListRow> featureListRows) {
    super(title, true, false);
    this.parameters = parameters;
    this.featureList = featureList;
    this.featureListRows = featureListRows;
    init();
  }

  private void init() {
    buildEcnModelPlots();
    ScrollPane scrollPane = new ScrollPane(buildEcnModelPlots());
    scrollPane.setFitToWidth(true);
    scrollPane.setFitToHeight(true);
    this.setContent(scrollPane);
  }

  @Override
  public @NotNull Collection<? extends RawDataFile> getRawDataFiles() {
    return List.of();
  }

  @Override
  public @NotNull Collection<? extends FeatureList> getFeatureLists() {
    return List.of(featureList);
  }

  @Override
  public @NotNull Collection<? extends FeatureList> getAlignedFeatureLists() {
    return List.of();
  }

  @Override
  public void onRawDataFileSelectionChanged(Collection<? extends RawDataFile> rawDataFiles) {

  }

  @Override
  public void onFeatureListSelectionChanged(Collection<? extends FeatureList> featureLists) {

  }

  @Override
  public void onAlignedFeatureListSelectionChanged(Collection<? extends FeatureList> featureLists) {

  }

  private GridPane buildEcnModelPlots() {
    List<FeatureListRow> rowsWithLipidID = featureListRows.stream()
        .filter(this::rowHasMatchedLipidSignals).toList();

    List<MatchedLipid> bestLipidMatches = new ArrayList<>();
    for (FeatureListRow featureListRow : rowsWithLipidID) {
      if (featureListRow instanceof ModularFeatureListRow) {
        bestLipidMatches.add(featureListRow.get(LipidMatchListType.class).get(0));
      }
    }
    return new EquivalentCarbonNumberModelGridPane(rowsWithLipidID, bestLipidMatches);
  }

  private boolean rowHasMatchedLipidSignals(FeatureListRow row) {
    List<MatchedLipid> matches = row.get(LipidMatchListType.class);
    return matches != null && !matches.isEmpty();
  }

}
