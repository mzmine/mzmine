package io.github.mzmine.modules.visualization.lipidannotationsummary;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.gui.mainwindow.MZmineTab;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidannotationmodules.LipidAnnotationParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import java.time.Instant;
import java.util.Collection;
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
      MZmineTab tab = new LipidAnnotationSummaryTab("Lipid annotation summary", parameters,
          featureList);
      MZmineCore.getDesktop().addTab(tab);
    }

    return ExitCode.OK;
  }

  @Override
  public @NotNull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.VISUALIZATIONFEATURELIST;
  }
}
