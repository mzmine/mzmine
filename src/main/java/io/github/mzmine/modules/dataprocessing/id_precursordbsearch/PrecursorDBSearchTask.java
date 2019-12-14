/*
 * Copyright 2006-2020 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.id_precursordbsearch;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.github.mzmine.datamodel.PeakList;
import io.github.mzmine.datamodel.PeakListRow;
import io.github.mzmine.datamodel.impl.SimplePeakListAppliedMethod;
import io.github.mzmine.gui.Desktop;
import io.github.mzmine.gui.impl.HeadLessDesktop;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.PrecursorDBPeakIdentity;
import io.github.mzmine.util.spectraldb.entry.SpectralDBEntry;
import io.github.mzmine.util.spectraldb.parser.AutoLibraryParser;
import io.github.mzmine.util.spectraldb.parser.LibraryEntryProcessor;
import io.github.mzmine.util.spectraldb.parser.UnsupportedFormatException;

/**
 * Search for possible precursor m/z . All rows average m/z against local
 * spectral database
 * 
 * @author
 *
 */
class PrecursorDBSearchTask extends AbstractTask {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private final PeakList peakList;
    private final File dataBaseFile;
    private ParameterSet parameters;

    private MZTolerance mzTol;
    private boolean useRT;
    private RTTolerance rtTol;

    private List<AbstractTask> tasks;
    private int totalTasks;
    private AtomicInteger matches = new AtomicInteger(0);

    public PrecursorDBSearchTask(PeakList peakList, ParameterSet parameters) {
        this.peakList = peakList;
        this.parameters = parameters;
        dataBaseFile = parameters
                .getParameter(PrecursorDBSearchParameters.dataBaseFile)
                .getValue();
        mzTol = parameters
                .getParameter(PrecursorDBSearchParameters.mzTolerancePrecursor)
                .getValue();
        useRT = parameters.getParameter(PrecursorDBSearchParameters.rtTolerance)
                .getValue();
        rtTol = !useRT ? null
                : parameters
                        .getParameter(PrecursorDBSearchParameters.rtTolerance)
                        .getEmbeddedParameter().getValue();
    }

    /**
     * @see io.github.mzmine.taskcontrol.Task#getFinishedPercentage()
     */
    @Override
    public double getFinishedPercentage() {
        if (totalTasks == 0 || tasks == null)
            return 0;
        return ((double) totalTasks - tasks.size()) / totalTasks;
    }

    /**
     * @see io.github.mzmine.taskcontrol.Task#getTaskDescription()
     */
    @Override
    public String getTaskDescription() {
        return "Identifiy possible precursor  m/z in " + peakList
                + " using database " + dataBaseFile.getAbsolutePath();
    }

    /**
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        setStatus(TaskStatus.PROCESSING);
        int count = 0;
        try {
            tasks = parseFile(dataBaseFile);
            totalTasks = tasks.size();
            if (!tasks.isEmpty()) {
                // wait for the tasks to finish
                while (!isCanceled() && !tasks.isEmpty()) {
                    for (int i = 0; i < tasks.size(); i++) {
                        if (tasks.get(i).isFinished()
                                || tasks.get(i).isCanceled()) {
                            tasks.remove(i);
                            i--;
                        }
                    }
                    // wait for all sub tasks to finish
                    try {
                        Thread.sleep(100);
                    } catch (Exception e) {
                        cancel();
                    }
                }
                // cancelled
                if (isCanceled()) {
                    tasks.stream().forEach(AbstractTask::cancel);
                }
            } else {
                setStatus(TaskStatus.ERROR);
                setErrorMessage("DB file was empty - or error while parsing "
                        + dataBaseFile);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Could not read file " + dataBaseFile, e);
            setStatus(TaskStatus.ERROR);
            setErrorMessage(e.toString());
        }
        logger.info("Added " + matches.get()
                + " matches to possible precursors in library: "
                + dataBaseFile.getAbsolutePath());

        // Add task description to peakList
        peakList.addDescriptionOfAppliedTask(new SimplePeakListAppliedMethod(
                "Possible precursor identification using MS/MS spectral database "
                        + dataBaseFile,
                parameters));

        // Repaint the window to reflect the change in the feature list
        Desktop desktop = MZmineCore.getDesktop();
        if (!(desktop instanceof HeadLessDesktop))
            desktop.getMainWindow().repaint();

        setStatus(TaskStatus.FINISHED);
    }

    /**
     * Load all library entries from data base file
     * 
     * @param dataBaseFile
     * @return
     */
    private List<AbstractTask> parseFile(File dataBaseFile)
            throws UnsupportedFormatException, IOException {
        //
        List<AbstractTask> tasks = new ArrayList<>();
        AutoLibraryParser parser = new AutoLibraryParser(100,
                new LibraryEntryProcessor() {
                    @Override
                    public void processNextEntries(List<SpectralDBEntry> list,
                            int alreadyProcessed) {

                        AbstractTask task = new AbstractTask() {
                            private int total = peakList.getNumberOfRows();
                            private int done = 0;

                            @Override
                            public void run() {
                                for (PeakListRow row : peakList.getRows()) {
                                    if (this.isCanceled())
                                        break;
                                    for (SpectralDBEntry db : list) {
                                        if (this.isCanceled())
                                            break;

                                        if (checkRT(row,
                                                (Double) db
                                                        .getField(
                                                                DBEntryField.RT)
                                                        .orElse(null))
                                                && checkMZ(row,
                                                        db.getPrecursorMZ())) {
                                            // add identity
                                            row.addPeakIdentity(
                                                    new PrecursorDBPeakIdentity(
                                                            db,
                                                            PrecursorDBSearchModule.MODULE_NAME),
                                                    false);
                                            matches.getAndIncrement();
                                        }
                                    }
                                    done++;
                                }

                                if (!this.isCanceled())
                                    setStatus(TaskStatus.FINISHED);
                            }

                            @Override
                            public String getTaskDescription() {
                                return "Checking for precursors: "
                                        + alreadyProcessed + 1 + " - "
                                        + alreadyProcessed + list.size();
                            }

                            @Override
                            public double getFinishedPercentage() {
                                if (total == 0)
                                    return 0;
                                return done / (double) total;
                            }
                        };

                        // start last task
                        MZmineCore.getTaskController().addTask(task);
                        tasks.add(task);
                    }
                });

        // return tasks
        parser.parse(this, dataBaseFile);
        return tasks;
    }

    protected boolean checkMZ(PeakListRow row, Double mz) {
        return mz != null && mzTol.checkWithinTolerance(row.getAverageMZ(), mz);
    }

    protected boolean checkRT(PeakListRow row, Double rt) {
        // if no rt is in the library still use
        return !useRT || rtTol == null
                || rtTol.checkWithinTolerance(row.getAverageRT(), rt);
    }

}
