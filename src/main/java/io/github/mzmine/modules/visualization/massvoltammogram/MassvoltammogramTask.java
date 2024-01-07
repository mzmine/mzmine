/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.visualization.massvoltammogram;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.massvoltammogram.io.MassvoltammogramFromFeatureListParameters;
import io.github.mzmine.modules.visualization.massvoltammogram.io.MassvoltammogramFromFileParameters;
import io.github.mzmine.modules.visualization.massvoltammogram.utils.Massvoltammogram;
import io.github.mzmine.modules.visualization.massvoltammogram.utils.ReactionMode;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.time.Instant;
import org.jetbrains.annotations.NotNull;

public class MassvoltammogramTask extends AbstractTask {

  //Parameter.
  private final ReactionMode reactionMode;
  private final double delayTime; //In s.
  private final double potentialRampSpeed; //In mV/s.
  private final Range<Double> potentialRange;
  private final double stepSize; //In mV.
  private final Range<Double> mzRange;
  private ScanSelection scanSelection;

  //Raw Data.
  private RawDataFile file;
  private ModularFeatureList featureList;

  //The massvoltammogram.
  private Massvoltammogram massvoltammogram;

  public MassvoltammogramTask(@NotNull ParameterSet parameters, @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate);

    //Setting up the parameter that depend on a raw data file or a feature list being chosen.
    if (parameters instanceof MassvoltammogramFromFileParameters) {

      file = parameters.getValue(MassvoltammogramFromFileParameters.files)
          .getMatchingRawDataFiles()[0];
      scanSelection = parameters.getValue(MassvoltammogramFromFileParameters.scanSelection);
      featureList = null;

    } else if (parameters instanceof MassvoltammogramFromFeatureListParameters) {

      featureList = parameters.getValue(MassvoltammogramFromFeatureListParameters.featureLists)
          .getMatchingFeatureLists()[0];
      file = null;
      scanSelection = null;
    }

    //Setting up the remaining parameters.
    potentialRampSpeed = parameters.getValue(MassvoltammogramFromFileParameters.potentialRampSpeed);
    stepSize = parameters.getValue(MassvoltammogramFromFileParameters.stepSize);
    potentialRange = parameters.getValue(MassvoltammogramFromFileParameters.potentialRange);
    mzRange = parameters.getValue(MassvoltammogramFromFileParameters.mzRange);
    reactionMode = parameters.getValue(MassvoltammogramFromFileParameters.reactionMode);
    delayTime = parameters.getValue(MassvoltammogramFromFileParameters.delayTime);
  }

  @Override
  public String getTaskDescription() {
    return "Creating the massvoltammogram.";
  }

  /**
   * @return Returns the progress from the Massvoltammogram class, returns 0 if the massvoltammogram
   * is still null.
   */
  @Override
  public double getFinishedPercentage() {
    if (massvoltammogram != null) {
      return massvoltammogram.getProgress();
    } else {
      return 0;
    }
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    //Initializing the massvoltammogram.
    if (file != null) {

      massvoltammogram = new Massvoltammogram(file, scanSelection, reactionMode, delayTime,
          potentialRampSpeed, potentialRange, stepSize, mzRange);

    } else if (featureList != null) {

      massvoltammogram = new Massvoltammogram(featureList, reactionMode, delayTime,
          potentialRampSpeed, potentialRange, stepSize, mzRange);

    } else {

      setStatus(TaskStatus.ERROR);
      setErrorMessage("No data source is selected.");
      return;
    }

    //Drawing the massvoltammogram from the entered data.
    massvoltammogram.draw();

    //Adding the massvoltammogram to a new MZmineTab.
    final MassvoltammogramTab mvTab = new MassvoltammogramTab("Massvoltammogram", massvoltammogram);
    MZmineCore.getDesktop().addTab(mvTab);

    //Setting the task status to finished.
    setStatus(TaskStatus.FINISHED);
  }
}
