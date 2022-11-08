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

package io.github.mzmine.modules.visualization.rawdataoverviewims.threads;

import com.google.common.util.concurrent.AtomicDouble;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.scans.SpectraMerging;
import io.github.mzmine.util.scans.SpectraMerging.IntensityMergingType;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;

public class MergeFrameThread extends AbstractTask {

  protected final AtomicDouble progress = new AtomicDouble(0d);
  private IMSRawDataFile file;
  private ScanSelection selection;
  private int mobilityScanBin;
  private Double inputNoiseLevel;
  private Consumer<Frame> onFinished;

  public MergeFrameThread(IMSRawDataFile file, ScanSelection selection, int mobilityScanBin,
      Double inputNoiseLevel, Consumer<Frame> onFinished) {
    super(null, Instant.now());
    this.file = file;
    this.selection = selection;
    this.mobilityScanBin = mobilityScanBin;
    this.inputNoiseLevel = inputNoiseLevel;
    this.onFinished = onFinished;
  }

  @Override
  public String getTaskDescription() {
    return "Calculating merged frame.";
  }

  @Override
  public double getFinishedPercentage() {
    return progress.get();
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    final List<Frame> frames = (List<Frame>) selection.getMatchingScans(file.getFrames());

    // peaks in mobility scans should appear at least a couple of times. When merging a lot of
    // frames, we have a maximum number of peaks bc of the chromatographic peak width and the
    // cycle time. Let's go with 5 for now.
    final int minMobilityPeaks = Math.min(frames.size() - 1, 5);

    final Frame mergedFrame = SpectraMerging.getMergedFrame(null, SpectraMerging.defaultMs1MergeTol,
        frames, mobilityScanBin, IntensityMergingType.MAXIMUM, inputNoiseLevel, null,
        minMobilityPeaks, progress);

    if (isCanceled()) {
      return;
    }
    onFinished.accept(mergedFrame);

    setStatus(TaskStatus.FINISHED);
  }
}
