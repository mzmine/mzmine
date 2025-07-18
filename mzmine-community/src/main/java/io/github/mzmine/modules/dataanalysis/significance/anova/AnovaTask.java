/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.modules.dataanalysis.significance.anova;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.numbers.stats.AnovaPValueType;
import io.github.mzmine.datamodel.statistics.FeaturesDataTable;
import io.github.mzmine.modules.dataanalysis.utils.StatisticUtils;
import io.github.mzmine.modules.visualization.projectmetadata.MetadataColumnDoesNotExistException;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.statistics.AbundanceDataTablePreparationConfig;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

public class AnovaTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(AnovaTask.class.getName());
  private final ParameterSet parameters;
  private final FeatureList flist;
  private final String groupingColumnName;
  private final AbundanceDataTablePreparationConfig tablePrepConfig;
  private AnovaTest calc;
  private int processed;

  public AnovaTask(FeatureList flist, ParameterSet parameters, @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate);
    this.flist = flist;
    this.parameters = parameters;
    this.groupingColumnName = this.parameters.getValue(AnovaParameters.groupingParameter);
    tablePrepConfig = parameters.getParameter(AnovaParameters.abundanceDataTablePreparation)
        .createConfig();
  }

  @Override
  public @NotNull String getTaskDescription() {
    return "Calculating ANOVA values for %d rows".formatted(flist.getNumberOfRows());
  }

  public double getFinishedPercentage() {
    return (double) processed / flist.getNumberOfRows();
  }

  public void run() {
    if (isCanceled()) {
      return;
    }

    setStatus(TaskStatus.PROCESSING);
    logger.info("Started calculating significance values");

    flist.addRowType(DataTypes.get(AnovaPValueType.class));
    try {
      // extract and prepare data
      final FeaturesDataTable dataTable = StatisticUtils.extractAbundancesPrepareData(
          flist.getRows(), flist.getRawDataFiles(), tablePrepConfig);

      final MetadataColumn<?> column = ProjectService.getMetadata()
          .getColumnByName(groupingColumnName);
      calc = new AnovaTest(dataTable, column);
    } catch (MetadataColumnDoesNotExistException e) {
      setErrorMessage(e.getMessage());
      logger.log(Level.WARNING, e.getMessage(), e);
      setStatus(TaskStatus.ERROR);
      return;
    }

    final List<AnovaResult> anovaResults = flist.getRows().stream().map(row -> {
      if (isCanceled()) {
        return null;
      }
      processed++;
      return calc.test(row);
    }).filter(Objects::nonNull).toList();

    anovaResults.forEach(r -> r.row().set(AnovaPValueType.class, r.pValue()));
    flist.getAppliedMethods()
        .add(new SimpleFeatureListAppliedMethod(AnovaModule.class, parameters, moduleCallDate));

    setStatus(TaskStatus.FINISHED);
  }
}
