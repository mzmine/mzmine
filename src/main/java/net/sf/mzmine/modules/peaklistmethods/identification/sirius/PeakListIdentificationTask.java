/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package net.sf.mzmine.modules.peaklistmethods.identification.sirius;

import io.github.msdk.datamodel.IonAnnotation;
import io.github.msdk.id.sirius.SiriusIonAnnotation;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

import javax.annotation.Nonnull;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.impl.HeadLessDesktop;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.ExceptionUtils;
import net.sf.mzmine.util.PeakListRowSorter;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;

import org.slf4j.LoggerFactory;

public class PeakListIdentificationTask extends AbstractTask {

  // Logger.
  private static final org.slf4j.Logger logger = LoggerFactory.getLogger(PeakListIdentificationTask.class);

  // Counters.
  private int numItems;
  private final CountDownLatch latch;

  // Thread controller
  private final Semaphore semaphore;

  private final ParameterSet parameters;
  private final PeakList peakList;
  private PeakListRow currentRow;

  /**
   * Create the identification task.
   * 
   * @param parameters task parameters.
   * @param list peak list to operate on.
   */
  PeakListIdentificationTask(final ParameterSet parameters, final PeakList list) {
    peakList = list;
    numItems = 0;
    currentRow = null;
    this.parameters = parameters;

    int threadsAmount = parameters.getParameter(PeakListIdentificationParameters.THREADS_AMOUNT).getValue();
    semaphore = new Semaphore(threadsAmount);
    latch = new CountDownLatch(list.getNumberOfRows());
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

        // Identify the peak list rows starting from the biggest peaks.
        final PeakListRow[] rows = peakList.getRows();
        Arrays.sort(rows, new PeakListRowSorter(SortingProperty.Area, SortingDirection.Descending));

        // Initialize counters.
        numItems = rows.length;

        // Process rows.
        for (int index = 0; !isCanceled() && index < numItems;) {
          try {
            semaphore.acquire();
            logger.debug("Semaphore ACQUIRED");
            Thread th = new Thread(new SiriusThread(rows[index++], parameters, semaphore, latch));
            th.setDaemon(true);
            th.start();
          } catch (InterruptedException e) {
            logger.error("The thread was interrupted");
            e.printStackTrace();
          }
        }

        // Wait till all rows are processed
        latch.await();
        if (!isCanceled()) {
          setStatus(TaskStatus.FINISHED);
        }
      } catch (Throwable t) {

        final String msg = "Could not search ";
        logger.warn(msg, t);
        setStatus(TaskStatus.ERROR);
        setErrorMessage(msg + ": " + ExceptionUtils.exceptionToString(t));
      }
    }
  }

  /**
   * Adds peak identities to requested row
   * @param annotations list of IonAnnotations
   * @param row to add identities
   * @param amount of identities to be added from list
   */
  public synchronized static void addSiriusCompounds(@Nonnull List<IonAnnotation> annotations, @Nonnull PeakListRow row, int amount) {
    for (int i = 0; i < amount; i++) { //todo: add howManyTopResultsToStore
      SiriusIonAnnotation annotation = (SiriusIonAnnotation) annotations.get(i);
      SiriusCompound compound = new SiriusCompound(annotation, annotation.getSiriusScore());
      row.addPeakIdentity(compound, false);
    }
    notifyRow(row);
  }

  /**
   * Method notifies object about content update
   * @param row to be notified
   */
  private static void notifyRow(PeakListRow row) {
    MZmineCore.getProjectManager().getCurrentProject().notifyObjectChanged(row, false);

    Desktop desktop = MZmineCore.getDesktop();
    if (!(desktop instanceof HeadLessDesktop))
      desktop.getMainWindow().repaint();
  }
}
