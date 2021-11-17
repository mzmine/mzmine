/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package io.github.mzmine.modules.dataprocessing.id_spectraldbsearch.preloaded;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.types.annotations.SpectralLibraryMatchesType;
import io.github.mzmine.modules.dataprocessing.id_spectraldbsearch.RowsSpectralMatchTask;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.spectraldb.entry.SpectralDBEntry;
import io.github.mzmine.util.spectraldb.entry.SpectralLibrary;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

class LocalSpectralDBSearchTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(LocalSpectralDBSearchTask.class.getName());
  private final List<SpectralLibrary> libraries;
  private final FeatureList[] featureLists;
  private final ParameterSet parameters;
  private final AtomicInteger finishedRows = new AtomicInteger(0);
  private int totalRows = 0;

  public LocalSpectralDBSearchTask(ParameterSet parameters, @NotNull Date moduleCallDate) {
    super(null, moduleCallDate); // no new data stored -> null
    this.featureLists = parameters.getParameter(
        LocalSpectralDBSearchParameters.peakLists)
        .getValue().getMatchingFeatureLists();
    this.parameters = parameters;
    libraries = parameters.getParameter(LocalSpectralDBSearchParameters.libraries).getValue();
  }

  @Override
  public double getFinishedPercentage() {
    return totalRows == 0 ? 0 : finishedRows.get() / (double) totalRows;
  }

  @Override
  public String getTaskDescription() {
    return "Spectral database identification in " + featureLists.length
           + " feature lists using libraries " +
           libraries.stream().map(Objects::toString).collect(Collectors.joining(", "));
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    List<FeatureListRow> rows = new ArrayList<>();
    // add row type
    for (var flist : featureLists) {
      flist.addRowType(new SpectralLibraryMatchesType());
      rows.addAll(flist.getRows());
    }
    totalRows = rows.size();

    // combine libraries
    List<SpectralDBEntry> entries = new ArrayList<>();
    for (var lib : libraries) {
      entries.addAll(lib.getEntries());
    }

    logger.info(() -> String
        .format("Comparing %d library spectra to %d feature list rows", entries.size(),
            rows.size()));

    // use sub task for all the parameters
    RowsSpectralMatchTask matcher = RowsSpectralMatchTask
        .createSubTask(parameters, entries, null, getModuleCallDate());

    // run in parallel
    rows.stream().parallel().forEach(row -> {
      if (!isCanceled()) {
        matcher.matchRowToLibrary(row);
        finishedRows.incrementAndGet();
      }
    });

    logger.info(() -> String.format("library matches=%d (Errors:%d); rows=%d; library entries=%d",
        matcher.getCount(), matcher.getErrorCount(), rows.size(), entries.size()));

    // Add task description to peakList
    for (var flist : featureLists) {
      flist.addDescriptionOfAppliedTask(new SimpleFeatureListAppliedMethod(
          "Spectral library matching with libraries: " + libraries.stream().map(Objects::toString)
              .collect(Collectors.joining(", ")),
          PreloadedLocalSpectralDBSearchModule.class, parameters, getModuleCallDate()));
    }
    setStatus(TaskStatus.FINISHED);
  }

}
