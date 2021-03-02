package io.github.mzmine.modules.dataprocessing.featdet_smoothing2;

import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.MemoryMapStorage;
import java.util.Collection;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class SmoothingModule implements MZmineProcessingModule {

  private static final String name = "Smoothing";

  @Nonnull
  @Override
  public String getName() {
    return name;
  }

  @Nullable
  @Override
  public Class<? extends ParameterSet> getParameterSetClass() {
    return SmoothingParameters.class;
  }

  @Nonnull
  @Override
  public String getDescription() {
    return "Smoothes intensity along the retentio time and/or mobility dimension.";
  }

  @Nonnull
  @Override
  public ExitCode runModule(@Nonnull MZmineProject project, @Nonnull ParameterSet parameters,
      @Nonnull Collection<Task> tasks) {

    ModularFeatureList[] flists = parameters.getParameter(SmoothingParameters.featureLists)
        .getValue().getMatchingFeatureLists();

    boolean[] isIms = new boolean[flists.length];
    boolean onlyIMS = true;
    for (int i = 0; i < flists.length; i++) {
      if (flists[i].getRawDataFile(0) instanceof IMSRawDataFile) {
        isIms[i] = true;
      } else {
        onlyIMS = false;
      }
    }

    if (onlyIMS) {
      for (var flist : flists) {
        tasks.add(new SmoothingTask(project, flist, MemoryMapStorage.forFeatureList(), parameters));
      }
    } else {
      final MemoryMapStorage storage = MemoryMapStorage.forFeatureList();
      for (int i = 0; i < flists.length; i++) {
        if (isIms[i] == false) {
          tasks.add(new SmoothingTask(project, flists[i], storage, parameters));
        } else {
          tasks.add(
              new SmoothingTask(project, flists[i], MemoryMapStorage.forFeatureList(), parameters));
        }
      }
    }

    return ExitCode.OK;
  }

  @Nonnull
  @Override
  public MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.FEATUREDETECTION;
  }
}
