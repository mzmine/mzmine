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

package io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.clearionids;


import io.github.msdk.MSDKRuntimeException;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

public class ClearIonIdentitiesTask extends AbstractTask {

  // Logger.
  private static final Logger LOG = Logger.getLogger(ClearIonIdentitiesTask.class.getName());

  private AtomicInteger finishedRows = new AtomicInteger(0);
  private int totalRows;
  private final ModularFeatureList featureList;
  private final ParameterSet parameters;
  private final MZmineProject project;

  /**
   * Create the task.
   *
   * @param parameterSet the parameters.
   */
  public ClearIonIdentitiesTask(final MZmineProject project, final ParameterSet parameterSet,
      final ModularFeatureList featureLists, @NotNull Instant moduleCallDate) {
    super(featureLists.getMemoryMapStorage(), moduleCallDate);
    this.project = project;
    this.featureList = featureLists;
    parameters = parameterSet;
    totalRows = 0;
  }

  @Override
  public double getFinishedPercentage() {
    return totalRows == 0 ? 0 : finishedRows.get() / (double) totalRows;
  }

  @Override
  public String getTaskDescription() {
    return "Clearing ion identities and networks in " + featureList.getName() + " ";
  }

  @Override
  public void run() {
    try {
      setStatus(TaskStatus.PROCESSING);
      LOG.info("Clearing ion identities and networks in " + featureList.getName());

      // filter
      doFiltering(featureList, finishedRows);

      // Done.
      setStatus(TaskStatus.FINISHED);
      LOG.info("Clearing ion identities and networks in " + featureList);
    } catch (Exception t) {
      LOG.log(Level.SEVERE, "Clearing ion identities and networks error", t);
      setStatus(TaskStatus.ERROR);
      setErrorMessage(t.getMessage());
      throw new MSDKRuntimeException(t);
    }
  }


  /**
   * Delete all networks smaller min size
   * 
   * @param pkl
   * @param finishedRows pointer to finished networks
   * @throws Exception
   */
  public static void doFiltering(FeatureList pkl, AtomicInteger finishedRows) throws Exception {
    pkl.stream().filter(FeatureListRow::hasIonIdentity).forEach(r -> {
      r.clearIonIdentites();
    });
  }

}
