/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.dataanalysis.anova;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.numbers.stats.AnovaPValueType;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.time.Instant;
import java.util.List;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

public class AnovaTask extends AbstractTask {

  private final ParameterSet parameters;
  private Logger logger = Logger.getLogger(this.getClass().getName());

  private final FeatureList flist;
  private final String groupingColumnName;
  private AnovaCalculation calc;

  public AnovaTask(FeatureList flist, ParameterSet parameters, @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate);
    this.flist = flist;
    this.parameters = parameters;
    this.groupingColumnName = this.parameters.getValue(AnovaParameters.groupingParameter);
  }

  public String getTaskDescription() {
    return "Calculating significance... ";
  }

  public double getFinishedPercentage() {
    return calc != null ? calc.getFinishedPercentage() : 0;
  }

  public void run() {
    if (isCanceled()) {
      return;
    }

    setStatus(TaskStatus.PROCESSING);
    logger.info("Started calculating significance values");

    flist.addRowType(DataTypes.get(AnovaPValueType.class));

    calc = new AnovaCalculation(flist, groupingColumnName);
    calc.process();
    final List<AnovaResult> anovaResults = calc.get();

    anovaResults.forEach(r -> r.row().set(AnovaPValueType.class, r.pValue()));
    flist.getAppliedMethods()
        .add(new SimpleFeatureListAppliedMethod(AnovaModule.class, parameters, moduleCallDate));

  }
}
