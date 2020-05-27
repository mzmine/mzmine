package io.github.mzmine.modules.tools.qualityparameters;

import io.github.mzmine.datamodel.PeakList;
import io.github.mzmine.datamodel.data.ModularFeatureList;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;

public class QualityParametersTask extends AbstractTask {

  private final PeakList featureList;
  private final ParameterSet parameterSet;

  private final ModularFeatureList modularFeatureList;

  private double finishedPercentage;

  public QualityParametersTask(PeakList peakList, ParameterSet parameterSet) {
    this.featureList = peakList;
    this.parameterSet = parameterSet;
    modularFeatureList = null;
    setStatus(TaskStatus.WAITING);
    finishedPercentage = 0.d;
  }

  public QualityParametersTask(ModularFeatureList modularFeatureList, ParameterSet parameterSet) {
    this.modularFeatureList = modularFeatureList;
    this.parameterSet = parameterSet;
    featureList = null;
    setStatus(TaskStatus.WAITING);
    finishedPercentage = 0.d;
  }

  @Override
  public String getTaskDescription() {
    return "Calculating quality parameters for feature list " + featureList.getName();
  }

  @Override
  public double getFinishedPercentage() {
    return finishedPercentage;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.WAITING);
    if (featureList != null) {
      QualityParameters.calculateQualityParameters(featureList);
    }
    else if(modularFeatureList != null) {
      QualityParameters.calculateQualityParameters(modularFeatureList);
    }
    finishedPercentage = 100.d;
    setStatus(TaskStatus.FINISHED);
  }

}
