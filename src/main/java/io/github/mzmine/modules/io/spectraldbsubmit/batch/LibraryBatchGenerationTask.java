/*
 * Copyright 2006-2022 The MZmine Development Team
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
/*
 * This module was prepared by Abi Sarvepalli, Christopher Jensen, and Zheng Zhang at the Dorrestein
 * Lab (University of California, San Diego).
 *
 * It is freely available under the GNU GPL licence of MZmine2.
 *
 * For any questions or concerns, please refer to:
 * https://groups.google.com/forum/#!forum/molecular_networking_bug_reports
 *
 * Credit to the Du-Lab development team for the initial commitment to the MGF export module.
 */

package io.github.mzmine.modules.io.spectraldbsubmit.batch;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.ionidnetworking.IonNetworkLibrary;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.io.File;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Exports all files needed for GNPS
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class LibraryBatchGenerationTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(LibraryBatchGenerationTask.class.getName());
  private final MZmineProject project;
  private final ParameterSet parameters;
  private final ModularFeatureList[] flists;
  private final boolean exportGnpsJson;
  private final boolean exportMsp;
  private final int minSignals;
  private final ScanSelector scanExport;
  private final File outFile;
  private final String metadataSeparator;
  private final File metadataFile;
  private final IonNetworkLibrary ions;

  public int totalRows = 0;
  public AtomicInteger finishedRows = new AtomicInteger(0);
  private String description = "Batch exporting spectral library";

  public LibraryBatchGenerationTask(final MZmineProject project, final ParameterSet parameters,
      final Instant moduleCallDate) {
    super(null, moduleCallDate);
    this.project = project;
    this.parameters = parameters;
    flists = parameters.getValue(LibraryBatchGenerationParameters.flists).getMatchingFeatureLists();
    exportGnpsJson = parameters.getValue(LibraryBatchGenerationParameters.exportGnpsJson);
    exportMsp = parameters.getValue(LibraryBatchGenerationParameters.exportMsp);
    minSignals = parameters.getValue(LibraryBatchGenerationParameters.minSignals);
    scanExport = parameters.getValue(LibraryBatchGenerationParameters.scanExport);
    outFile = parameters.getValue(LibraryBatchGenerationParameters.file);
    metadataFile = parameters.getValue(LibraryBatchGenerationParameters.metadata);
    metadataSeparator = parameters.getValue(LibraryBatchGenerationParameters.fieldSeparator);
    var ions = parameters.getParameter(LibraryBatchGenerationParameters.ions)
        .getEmbeddedParameters();
    this.ions = new IonNetworkLibrary(ions);
  }

  @Override
  public double getFinishedPercentage() {
    return totalRows == 0 ? 0 : (double) finishedRows.get() / totalRows;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    description = "Importing metadata";

//    ConcurrentHashMap<FeatureListRow,> matches = new ConcurrentHashMap();

    for (ModularFeatureList flist : flists) {
//      flist.stream().parallel().forEach(row -> {
//        List<matchRow(row, metadata, )
//        matches.put(row, match);
//      });
    }

    setStatus(TaskStatus.FINISHED);
  }

//  public String getMspEntry() {
//    String msp = MSPEntryGenerator.createMSPEntry(param, dps);
//  }

  @Override
  public String getTaskDescription() {
    return description;
  }

}
