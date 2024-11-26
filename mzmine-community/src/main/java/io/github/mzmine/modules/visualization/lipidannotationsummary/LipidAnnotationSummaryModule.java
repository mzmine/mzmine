package io.github.mzmine.modules.visualization.lipidannotationsummary;

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
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import io.mzio.mzmine.datamodel.parameters.ParameterSet;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LipidAnnotationSummaryModule implements MZmineRunnableModule {

  @Override
  public @NotNull String getName() {
    return "Lipid Annotation summary module";
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return LipidAnnotationSummaryParameters.class;
  }


  @Override
  public @NotNull String getDescription() {
    return "This module summarizes the lipid annotation results of a selected feature list";
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
        MZmineTab tab = new LipidAnnotationSummaryTab("Lipid Annotation summary", parameters,
            featureList);
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
