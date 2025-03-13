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

package io.github.mzmine.modules.dataprocessing.dummy_example;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.*;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.types.numbers.scores.DummyOneScoreType;
import io.github.mzmine.datamodel.features.types.numbers.scores.DummyTwoScoreType;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractFeatureListTask;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.exceptions.MissingMassListException;
import io.github.mzmine.util.scans.ScanUtils;
import org.apache.poi.ss.formula.Formula;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.time.Instant;
import java.util.List;
import java.util.logging.Logger;

/**
 * The task will be scheduled by the TaskController. Progress is calculated from the
 * finishedItems/totalItems
 */
class DummyFeatureListTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(DummyFeatureListTask.class.getName());

  private final ModularFeatureList featureList;
  private final MZmineProject project;
  private final ParameterSet parameters;

  //feature counters
  private  int finished;
  private int totalRows;

  /**
   * Constructor is used to extract all parameters
   *
   * @param featureList data source is featureList
   * @param parameters  user parameters
   */
  public DummyFeatureListTask(MZmineProject project, FeatureList featureList,
                              ParameterSet parameters, @Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);
    this.featureList = (ModularFeatureList) featureList;
    this.project = project;
    this.parameters = parameters;
  }

  @Override
  public String getTaskDescription() {
    return "--------------Runs task on: " + featureList;
  }

  @Override
  public double getFinishedPercentage() {
    return totalRows ==0? 0 : finished/(double)totalRows;
  }

  //ACTUAL logic of our module
  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    logger.info(getTaskDescription());
    //setup
    totalRows = featureList.getNumberOfRows();

    for (FeatureListRow row : featureList.getRows()){
      if (isCanceled()){
        return;
      }
      //logic (we can extract to a new method for simplicity)
      if(row.hasMs2Fragmentation()) {
        MassSpectrum scan = row.getMostIntenseFragmentScan();
        //MassList massList = scan.getMassList();

        /*if (massList == null ){
          //If there's no mass list then it finishes
          setErrorMessage("Missing mass list!!!");
          setStatus(TaskStatus.ERROR);
          throw new MissingMassListException(scan);
        }*/
        //EXAMPLE
        double lowestIntensity = ScanUtils.getLowestIntensity(scan);
        PolarityType highestIntensity = ScanUtils.getPolarity(scan);
        //To show in the feature list table
        row.set(DummyOneScoreType.class, (float) lowestIntensity);
        row.set(DummyTwoScoreType.class, (float) highestIntensity.getSign());

      }

      //update progress
      finished++;
    }

    //add to project
    addAppliedMethodsAndResultToProject();

    setStatus(TaskStatus.FINISHED);
  }

  public void addAppliedMethodsAndResultToProject(){
    //add task description in feature list
    //this appears on the batch queue
    featureList.addDescriptionOfAppliedTask(new SimpleFeatureListAppliedMethod(DummyFeatureListModule.class, parameters, getModuleCallDate()));
  }
}
