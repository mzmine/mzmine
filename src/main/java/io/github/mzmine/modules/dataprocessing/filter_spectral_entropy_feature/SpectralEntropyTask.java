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

package io.github.mzmine.modules.dataprocessing.filter_spectral_entropy_feature;

import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.types.numbers.scores.NormalizedSpectralEntropyType;
import io.github.mzmine.datamodel.features.types.numbers.scores.SpectralEntropyType;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.exceptions.MissingMassListException;
import io.github.mzmine.util.scans.ScanUtils;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

public class SpectralEntropyTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(SpectralEntropyTask.class.getName());
  private final ModularFeatureList[] featureLists;
  private final AtomicLong processed = new AtomicLong(0);
  private long total;

  SpectralEntropyTask(final @NotNull ParameterSet parameters,
      @NotNull final Instant moduleCallDate) {
    super(null, moduleCallDate);
    featureLists = parameters.getParameter(SpectralEntropyParameters.featureLists).getValue()
        .getMatchingFeatureLists();
  }


  @Override
  public void run() {
    for (ModularFeatureList featureList : featureLists) {
      for (FeatureListRow row : featureList.getRows()) {
        if (row.hasMs2Fragmentation()) {
          total++;

          Scan ms2 = row.getMostIntenseFragmentScan();
          if (ms2.getMassList() == null) {
            throw new MissingMassListException(ms2);
          }
        }
      }
    }
    if (total == 0) {
      setStatus(TaskStatus.FINISHED);
      logger.warning("There were no MS/MS scans in the selected feature lists");
      return;
    }

    setStatus(TaskStatus.PROCESSING);

    Arrays.stream(featureLists).map(FeatureList::getRows).flatMap(Collection::stream)
        .filter(FeatureListRow::hasMs2Fragmentation).parallel().forEach(row -> {
          Scan ms2 = row.getMostIntenseFragmentScan();
          MassList masses = ms2.getMassList();
          if (masses != null) {
            double spectralEntropy = ScanUtils.getSpectralEntropy(masses);
            double nse = ScanUtils.getNormalizedSpectralEntropy(masses);
            row.set(SpectralEntropyType.class, (float) spectralEntropy);
            row.set(NormalizedSpectralEntropyType.class, (float) nse);
          }
          processed.incrementAndGet();
        });

    setStatus(TaskStatus.FINISHED);
  }

  @Override
  public String getTaskDescription() {
    return String.format("Calculating the spectral entropy for row %d/%d", processed.get(), total);
  }

  @Override
  public double getFinishedPercentage() {
    return total == 0 ? 0 : processed.get() / (double) total;
  }
}
