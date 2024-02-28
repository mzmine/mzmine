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

package io.github.mzmine.modules.dataanalysis.spec_chimeric_precursor;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.types.numbers.PrecursorPurityType;
import io.github.mzmine.datamodel.features.types.numbers.SimpleStatistics;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.time.Instant;
import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

public class PrecursorPurityCheckerTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(PrecursorPurityCheckerTask.class.getName());
  private final FeatureList featureList;
  private final ParameterSet parameters;
  private final Double minimumPurity;
  private final MZTolerance mainSignalMzTol;
  private final MZTolerance isolationMzTol;
  private int totalRows;
  private final AtomicInteger finishedRows = new AtomicInteger(0);

  public PrecursorPurityCheckerTask(FeatureList featureList, ParameterSet parameters,
      @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate);
    this.featureList = featureList;
    this.parameters = parameters;

    minimumPurity = parameters.getValue(HandleChimericMsMsParameters.minimumPrecursorPurity);
    mainSignalMzTol = parameters.getValue(HandleChimericMsMsParameters.mainMassWindow);
    isolationMzTol = parameters.getValue(HandleChimericMsMsParameters.isolationWindow);
  }

  @Override
  public String getTaskDescription() {
    return "Checking for precursor isolation purity";
  }

  @Override
  public double getFinishedPercentage() {
    return totalRows == 0 ? 0 : finishedRows.get() / (double) totalRows;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    featureList.addRowType(new PrecursorPurityType());

    var filteredList = featureList.stream().filter(FeatureListRow::hasMs2Fragmentation).toList();
    totalRows = filteredList.size();
    filteredList.stream().parallel().forEach(this::checkPrecursorPurity);

    // add method to feature list
    featureList.addDescriptionOfAppliedTask(
        new SimpleFeatureListAppliedMethod(PrecursorPurityCheckerModule.class, parameters,
            moduleCallDate));

    logger.info("Finished Precursor purity checker on feature list with %d rows with MS2".formatted(
        totalRows));

    setStatus(TaskStatus.FINISHED);
  }

  private void checkPrecursorPurity(final FeatureListRow row) {
    if (row.hasMs2Fragmentation()) {
      var map = ChimericPrecursorChecker.checkChimericPrecursorIsolation(row, mainSignalMzTol,
          isolationMzTol, minimumPurity);
      String group = map.values().stream()
          .max(Comparator.comparingDouble(ChimericPrecursorResults::purity))
          .map(ChimericPrecursorResults::flag).map(Objects::toString).orElse(null);
      var summary = map.values().stream().mapToDouble(ChimericPrecursorResults::purity)
          .summaryStatistics();
      SimpleStatistics stats = new SimpleStatistics(summary, group);
      row.set(PrecursorPurityType.class, stats);
    }
    finishedRows.incrementAndGet();
  }
}
