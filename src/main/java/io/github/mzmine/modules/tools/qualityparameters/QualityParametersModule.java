package io.github.mzmine.modules.tools.qualityparameters;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.PeakList;
import io.github.mzmine.datamodel.data.ModularFeatureList;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import java.util.Collection;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class QualityParametersModule implements MZmineRunnableModule {

  public static final String DESCRIPTION = "Calculates quality parameters such as FWHM, asymmetry factor, tailing factor, S/N ratio.";
  public static final String NAME = "Quality parameters";

  @Nonnull
  @Override
  public String getDescription() {
    return DESCRIPTION;
  }

  @Nonnull
  @Override
  public ExitCode runModule(@Nonnull MZmineProject project, @Nonnull ParameterSet parameters,
      @Nonnull Collection<Task> tasks) {
    for (PeakList peakList : parameters.getParameter(QualityParametersParameters.peakLists)
        .getValue().getMatchingPeakLists()) {
      runModule(peakList, parameters);
    }
    return ExitCode.OK;
  }

  public ExitCode runModule(PeakList[] peakLists, ParameterSet parameters) {
    for(PeakList peakList : peakLists){
      runModule(peakList, parameters);
    }
    return ExitCode.OK;
  }

  public ExitCode runModule(PeakList peakList, ParameterSet parameters) {
    MZmineCore.getTaskController().addTask(new QualityParametersTask(peakList, parameters));
    return ExitCode.OK;
  }

  public ExitCode runModule(ModularFeatureList[] featureLists, ParameterSet parameters) {
    for(ModularFeatureList featureList : featureLists){
      runModule(featureList, parameters);
    }
    return ExitCode.OK;
  }

  public ExitCode runModule(ModularFeatureList featureList, ParameterSet parameters) {
    MZmineCore.getTaskController().addTask(new QualityParametersTask(featureList, parameters));
    return ExitCode.OK;
  }

  @Nonnull
  @Override
  public MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.TOOLS;
  }

  @Nonnull
  @Override
  public String getName() {
    return NAME;
  }

  @Nullable
  @Override
  public Class<? extends ParameterSet> getParameterSetClass() {
    return QualityParametersParameters.class;
  }
}
