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

package io.github.mzmine.modules.dataprocessing.filter_spectral_entropy;

import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.types.numbers.scores.NormalizedSpectralEntropyScoreType;
import io.github.mzmine.datamodel.features.types.numbers.scores.SpectralEntropyScoreType;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.exceptions.MissingMassListException;
import io.github.mzmine.util.scans.ScanUtils;
import java.time.Instant;
import java.util.Objects;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class FeatureSpectralEntropyTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(FeatureSpectralEntropyTask.class.getName());

  private final ParameterSet parameters;
  private final ModularFeatureList featureList;

  // features counter
  private int finished;
  private int totalRows;

  /**
   * Constructor used to extract all parameters
   *
   * @param featureList    runs this taks on this featureList
   * @param parameters     user parameters
   * @param storage        memory mapping is only used for memory intensive data that should be
   *                       stored for later processing - like spectra, feature data, ... so storage
   *                       is likely null here to process all in memory
   * @param moduleCallDate used internally to track the order of applied methods
   */
  public FeatureSpectralEntropyTask(FeatureList featureList, ParameterSet parameters,
      @Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);
    this.featureList = (ModularFeatureList) featureList;
    this.parameters = parameters;
    // Get parameter values for easier use
  }

  @Override
  public String getTaskDescription() {
    return "Calculates the spectral entropy on " + featureList;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    logger.info(getTaskDescription());

    // setup
    totalRows = featureList.getNumberOfRows();

    // precondition checks

    for (FeatureListRow row : featureList.getRows()) {
      // check for cancelled state and stop
      if (isCanceled()) {
        return;
      }
      // insert logic
      processRowAllScans(row);

      // Update progress
      finished++;
    }

    // add to project
    addAppliedMethodsAndResultToProject();

    setStatus(TaskStatus.FINISHED);
  }

  /**
   * Uses the most intense fragmentation mass spectrum
   */
  public void processRow(final FeatureListRow row) {
    if (row.hasMs2Fragmentation()) {
      Scan scan = row.getMostIntenseFragmentScan();
      MassList massList = scan.getMassList();
      if (massList == null) {
        setErrorMessage("Missing mass list");
        setStatus(TaskStatus.ERROR);
        throw new MissingMassListException(scan);
      }

      double spectralEntropy = ScanUtils.getSpectralEntropy(massList);
      double normalizedSpectralEntropy = ScanUtils.getNormalizedSpectralEntropy(massList);
      row.set(SpectralEntropyScoreType.class, (float) spectralEntropy);
      row.set(NormalizedSpectralEntropyScoreType.class, (float) normalizedSpectralEntropy);
    }
  }

  public void processRowAllScans(final FeatureListRow row) {
    if (row.hasMs2Fragmentation()) {
      row.getAllFragmentScans().stream().map(Scan::getMassList).filter(Objects::nonNull)
          .mapToDouble(ScanUtils::getSpectralEntropy).min()
          .ifPresent(value -> row.set(SpectralEntropyScoreType.class, (float) value));

      row.getAllFragmentScans().stream().map(Scan::getMassList).filter(Objects::nonNull)
          .mapToDouble(ScanUtils::getNormalizedSpectralEntropy).min()
          .ifPresent(value -> row.set(NormalizedSpectralEntropyScoreType.class, (float) value));
    }
  }


  public void addAppliedMethodsAndResultToProject() {
    // Add task description to feature list
    featureList.addDescriptionOfAppliedTask(
        new SimpleFeatureListAppliedMethod(FeatureSpectralEntropyModule.class, parameters,
            getModuleCallDate()));
  }

  @Override
  public double getFinishedPercentage() {
    return totalRows == 0 ? 0 : finished / (double) totalRows;
  }
}
