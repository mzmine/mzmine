/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.types.annotations.LipidMatchListType;
import io.github.mzmine.gui.mainwindow.MZmineTab;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.modules.dataprocessing.id_lipidid.annotation_modules.LipidAnnotationParameters;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This module is designed to plot Equivalent Carbon Number (ECN) models for feature lists after
 * they were annotated using
 * {@link
 * io.github.mzmine.modules.dataprocessing.id_lipidid.annotation_modules.LipidAnnotationModule}. The
 * ECN plot provides a visualization of lipid annotations based on their equivalent carbon number.
 * <p>
 * The module extracts lipid annotations from the provided feature lists, groups them by lipid class
 * and number of DBEs and creates ECN plots for each group. If no lipid annotations are present, it
 * alerts the user to run the
 * {@link
 * io.github.mzmine.modules.dataprocessing.id_lipidid.annotation_modules.LipidAnnotationModule}
 * module first.
 * <p>
 */
public class EquivalentCarbonNumberModule implements MZmineRunnableModule {

  @Override
  public @NotNull String getName() {
    return "Lipid ECN (Equivalent Carbon Number) plot";
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return EquivalentCarbonNumberParameters.class;
  }


  @Override
  public @NotNull String getDescription() {
    return "This module plots the equivalent carbon number models of a selected feature list";
  }

  @Override
  public @NotNull ExitCode runModule(@NotNull MZmineProject project,
      @NotNull ParameterSet parameters, @NotNull Collection<Task> tasks,
      @NotNull Instant moduleCallDate) {

    FeatureList[] featureLists = parameters.getParameter(
            parameters.getParameter(LipidAnnotationParameters.featureLists)).getValue()
        .getMatchingFeatureLists();
    for (FeatureList featureList : featureLists) {
      List<FeatureListRow> rowsWithLipidID = featureList.getRows().stream()
          .filter(this::rowHasMatchedLipidSignals).toList();
      if (!rowsWithLipidID.isEmpty()) {
        MZmineTab tab = new EquivalentCarbonNumberTab("ECN models for " + featureList.getName(),
            parameters, featureList);
        MZmineCore.getDesktop().addTab(tab);
      } else {
        Alert alert = new Alert(AlertType.WARNING);
        alert.setTitle("No Lipid Annotations found in " + featureList.getName());
        alert.setContentText("No Lipid Annotations found in " + featureList.getName()
                             + ". Did you run the Lipid Annotation module?");
        alert.showAndWait();
      }
    }

    return ExitCode.OK;
  }

  @Override
  public @NotNull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.VISUALIZATIONFEATURELIST;
  }

  private boolean rowHasMatchedLipidSignals(FeatureListRow row) {
    List<MatchedLipid> matches = row.get(LipidMatchListType.class);
    return matches != null && !matches.isEmpty();
  }
}
