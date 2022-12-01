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

package io.github.mzmine.modules.visualization.chromatogramandspectra;

import com.google.common.util.concurrent.AtomicDouble;
import io.github.mzmine.datamodel.MergedMassSpectrum;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.scans.SpectraMerging;
import io.github.mzmine.util.scans.SpectraMerging.IntensityMergingType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class SpectraMergingThread extends AbstractTask {

  private final Collection<RawDataFile> rawDataFiles;
  private final ScanSelection selection;
  private final Consumer<List<Scan>> consumer;

  private final AtomicDouble progress = new AtomicDouble(0d);

  public SpectraMergingThread(Collection<RawDataFile> rawDataFiles, ScanSelection selection,
      Consumer<List<Scan>> consumer) {
    super(null, Instant.now());

    this.rawDataFiles = rawDataFiles;
    this.selection = selection;
    this.consumer = consumer;
  }

  @Override
  public String getTaskDescription() {
    return "Merging spectra for " + rawDataFiles.size() + " raw data files.";
  }

  @Override
  public double getFinishedPercentage() {
    return progress.get();
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    int i = 0;
    List<Scan> mergedScans = new ArrayList<>();
    for (var file : rawDataFiles) {
      final List<Scan> scans = selection.getMatchingScans(file.getScans());
//      final MergedMassSpectrum merged = SpectraMerging.mergeSpectra(scans,
//          SpectraMerging.defaultMs1MergeTol, IntensityMergingType.MAXIMUM, null, null,
//          Math.min(scans.size() - 1, 5), null);
      i++;
      progress.set(i / (double)rawDataFiles.size());
//      mergedScans.add(merged);
      if(isCanceled()) {
        return;
      }
    }

    consumer.accept(mergedScans);

    setStatus(TaskStatus.FINISHED);
  }
}
