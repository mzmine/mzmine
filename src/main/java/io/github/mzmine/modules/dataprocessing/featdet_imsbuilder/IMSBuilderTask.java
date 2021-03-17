package io.github.mzmine.modules.dataprocessing.featdet_imsbuilder;

import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.MobilityScanDataType;
import io.github.mzmine.datamodel.data_access.MobilityScanDataAccess;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.util.MemoryMapStorage;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class IMSBuilderTask extends AbstractTask {

  private final IMSRawDataFile file;
  private final ParameterSet parameters;
  private final ScanSelection scanSelection;
  private int stepProcessed = 0;
  private int stepTotal = 0;
  private int currentStep = 1;
  private final int steps = 4;

  public IMSBuilderTask(@Nullable MemoryMapStorage storage, @Nonnull final IMSRawDataFile file,
      @Nonnull final
      ParameterSet parameters) {
    super(storage);

    this.file = file;
    this.parameters = parameters;
    scanSelection = parameters.getParameter(IMSBuilderParameters.scanSelection).getValue();
  }

  @Override
  public String getTaskDescription() {
    return "Running feature detection on " + file.getName();
  }

  @Override
  public double getFinishedPercentage() {
    return stepProcessed / (double) stepTotal * (currentStep / (double) steps);
  }

  @Override
  public void run() {

    final MobilityScanDataAccess access = EfficientDataAccess
        .of(file, MobilityScanDataType.CENTROID, scanSelection);

    while(access.hasNextFrame()) {
      final Frame frame = access.nextFrame();

      while (access.hasNextMobilityScan()) {

      }
    }
  }
}
