/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.dataprocessing.id_sirius;

import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import io.github.msdk.datamodel.IonAnnotation;
import io.github.msdk.id.sirius.SiriusIonAnnotation;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_sirius.table.SiriusCompound;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.ExceptionUtils;
import io.github.mzmine.util.FeatureListRowSorter;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;

public class PeakListIdentificationTask extends AbstractTask {

  // Logger.
  private static final Logger logger = Logger.getLogger(PeakListIdentificationTask.class.getName());

  // Counters.
  private int numItems;
  private final CountDownLatch latch;

  // Thread controller
  private final Semaphore semaphore;

  // Remote cancel variables
  private final Object cancelLock; // lock
  private boolean cancelled;

  private final ParameterSet parameters;
  private final FeatureList peakList;
  private FeatureListRow currentRow;

  /**
   * Create the identification task.
   *
   * @param parameters task parameters.
   * @param list feature list to operate on.
   */
  PeakListIdentificationTask(final ParameterSet parameters, final FeatureList list, @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate); // no new data stored -> null
    peakList = list;
    numItems = 0;
    currentRow = null;
    this.parameters = parameters;
    cancelLock = new Object();

    int threadsAmount =
        parameters.getParameter(PeakListIdentificationParameters.THREADS_AMOUNT).getValue();
    semaphore = new Semaphore(threadsAmount);
    latch = new CountDownLatch(list.getNumberOfRows());

    int fingerCandidates, siriusCandidates, timer;
    timer = parameters.getParameter(PeakListIdentificationParameters.SIRIUS_TIMEOUT).getValue();
    siriusCandidates =
        parameters.getParameter(PeakListIdentificationParameters.CANDIDATES_AMOUNT).getValue();
    fingerCandidates =
        parameters.getParameter(PeakListIdentificationParameters.CANDIDATES_FINGERID).getValue();

    if (timer <= 0 || siriusCandidates <= 0 || fingerCandidates <= 0 || threadsAmount <= 0) {
      MZmineCore.getDesktop().displayErrorMessage("Sirius parameters can't be negative");
      setStatus(TaskStatus.ERROR);
    }
  }

  @Override
  public double getFinishedPercentage() {

    return numItems == 0 ? 0.0 : (double) (numItems - latch.getCount()) / (double) numItems;
  }

  @Override
  public String getTaskDescription() {

    return "Identification of peaks in " + peakList
        + (currentRow == null ? " using "
            : " (" + MZmineCore.getConfiguration().getMZFormat().format(currentRow.getAverageMZ())
                + " m/z) using SIRIUS");
  }

  @Override
  public void run() {
    if (!isCanceled()) {
      try {
        setStatus(TaskStatus.PROCESSING);

        // Identify the feature list rows starting from the biggest
        // peaks.
        FeatureListRow rows[] = peakList.getRows().toArray(FeatureListRow[]::new);
        Arrays.sort(rows,
            new FeatureListRowSorter(SortingProperty.Area, SortingDirection.Descending));

        // Initialize counters.
        numItems = rows.length;

        // Process rows.
        for (int index = 0; !isCanceled() && index < numItems;) {
          try {
            semaphore.acquire();
            Thread th =
                new Thread(new SiriusThread(rows[index++], parameters, semaphore, latch, this));
            th.setDaemon(true);
            th.start();
          } catch (InterruptedException e) {
            logger.warning("The thread was interrupted");
          }
        }

        if (isCanceled())
          return;

        // Wait till all rows are processed
        latch.await();
        if (!isCanceled()) {
          setStatus(TaskStatus.FINISHED);
        }
      } catch (Throwable t) {
        final String msg = "Could not search ";
        t.printStackTrace();
        setStatus(TaskStatus.ERROR);
        setErrorMessage(msg + ": " + ExceptionUtils.exceptionToString(t));
      }
    }
  }

  /**
   * Adds peak identities to requested row
   *
   * @param annotations list of IonAnnotations
   * @param row to add identities
   * @param amount of identities to be added from list
   */
  public synchronized static void addSiriusCompounds(@NotNull List<IonAnnotation> annotations,
      @NotNull FeatureListRow row, int amount) {
    for (int i = 0; i < amount; i++) {
      SiriusIonAnnotation annotation = (SiriusIonAnnotation) annotations.get(i);
      SiriusCompound compound = new SiriusCompound(annotation);
      row.addFeatureIdentity(compound, false);
    }
  }

  /**
   *
   */
  public void remoteCancel(String context) {
    synchronized (cancelLock) {
      if (!cancelled) {
        cancelled = true;
        setErrorMessage(context);
        setStatus(TaskStatus.ERROR);
      }
    }
  }
}
