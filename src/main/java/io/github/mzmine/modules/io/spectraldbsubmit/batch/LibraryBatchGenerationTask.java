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

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.modules.io.spectraldbsubmit.batch.HandleChimericMsMsParameters.ChimericMsOption;
import io.github.mzmine.modules.io.spectraldbsubmit.formats.GnpsJsonGenerator;
import io.github.mzmine.modules.io.spectraldbsubmit.formats.MSPEntryGenerator;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.exceptions.MissingMassListException;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.scans.ScanUtils;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.SpectralDBEntry;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Exports all files needed for GNPS
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class LibraryBatchGenerationTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(LibraryBatchGenerationTask.class.getName());
  private final ModularFeatureList[] flists;
  private final int minSignals;
  private final ScanSelector scanExport;
  private final File outFile;
  private final SpectralLibraryExportFormats format;
  private final Map<DBEntryField, Object> metadataMap;
  private final boolean handleChimerics;
  private double allowedOtherSignalSum = 0d;
  private MZTolerance mzTolChimericsMainIon;
  private MZTolerance mzTolChimericsIsolation;
  private ChimericMsOption handleChimericsOption;

  public long totalRows = 0;
  public AtomicInteger finishedRows = new AtomicInteger(0);
  public AtomicInteger exported = new AtomicInteger(0);
  public AtomicInteger doubleMatches = new AtomicInteger(0);
  private String description = "Batch exporting spectral library";

  public LibraryBatchGenerationTask(final MZmineProject project, final ParameterSet parameters,
      final Instant moduleCallDate) {
    super(null, moduleCallDate);
    flists = parameters.getValue(LibraryBatchGenerationParameters.flists).getMatchingFeatureLists();
    minSignals = parameters.getValue(LibraryBatchGenerationParameters.minSignals);
    scanExport = parameters.getValue(LibraryBatchGenerationParameters.scanExport);
    format = parameters.getValue(LibraryBatchGenerationParameters.exportFormat);
    String exportFormat = format.getExtension();
    File file = parameters.getValue(LibraryBatchGenerationParameters.file);
    outFile = FileAndPathUtil.getRealFilePath(file, exportFormat);

    // metadata as a map
    LibaryMetadataParameters meta = parameters.getParameter(
        LibraryBatchGenerationParameters.metadata).getEmbeddedParameters();
    metadataMap = meta.asMap();

    //
    handleChimerics = parameters.getValue(LibraryBatchGenerationParameters.handleChimerics);
    if (handleChimerics) {
      HandleChimericMsMsParameters param = parameters.getParameter(
          LibraryBatchGenerationParameters.handleChimerics).getEmbeddedParameters();
      allowedOtherSignalSum = param.getValue(HandleChimericMsMsParameters.allowedOtherSignals);
      mzTolChimericsIsolation = param.getValue(HandleChimericMsMsParameters.isolationWindow);
      mzTolChimericsMainIon = param.getValue(HandleChimericMsMsParameters.mainMassWindow);
      handleChimericsOption = param.getValue(HandleChimericMsMsParameters.option);
    }
  }

  @Override
  public double getFinishedPercentage() {
    return totalRows == 0 ? 0 : (double) finishedRows.get() / totalRows;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    totalRows = Arrays.stream(flists).mapToLong(ModularFeatureList::getNumberOfRows).sum();

    try (var writer = Files.newBufferedWriter(outFile.toPath(), StandardCharsets.UTF_8)) {
      for (int i = 0; i < flists.length; i++) {
        var flist = flists[i];
        description = "Exporting entries for feature list " + flist.getName();
        for (var row : flist.getRows()) {
          processRow(writer, row);
        }

        finishedRows.incrementAndGet();
      }
    } catch (IOException e) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Could not open file " + outFile + " for writing.");
      logger.log(Level.WARNING,
          String.format("Error writing libary file: %s. Message: %s", outFile.getAbsolutePath(),
              e.getMessage()), e);
      return;
    }

    setStatus(TaskStatus.FINISHED);
  }

  private void processRow(final BufferedWriter writer, final FeatureListRow row)
      throws IOException {
    List<Scan> scans = row.getAllFragmentScans();
    List<CompoundDBAnnotation> matches = row.getCompoundAnnotations();
    if (scans.isEmpty() || matches.isEmpty()) {
      return;
    }

    // handle chimerics
    final Map<RawDataFile, ChimericPrecursorResult> chimericMap;
    if (handleChimerics) {
      chimericMap = checkChimericPrecursorIsolation(row, scans);
      if (ChimericMsOption.SKIP.equals(handleChimericsOption)) {
        scans = scans.stream().filter(
                scan -> ChimericPrecursorResult.PASSED.equals(chimericMap.get(scan.getDataFile())))
            .toList();
      }
    } else {
      chimericMap = Map.of();
    }

    String lastName = null;
    int exported = 0;

    List<DataPoint[]> spectra = scans.stream().map(Scan::getMassList)
        .map(ScanUtils::extractDataPoints).toList();

    for (var match : matches) {
      // first entry for the same molecule reflect the most common ion type, usually M+H
      if (Objects.equals(match.getCompoundName(), lastName)) {
        continue;
      }

      lastName = match.getCompoundName();

      // filter matches
      for (int i = 0; i < spectra.size(); i++) {
        final DataPoint[] dataPoints = spectra.get(i);
        if (dataPoints.length < minSignals) {
          continue;
        }

        // add instrument type etc by parameter
        SpectralDBEntry entry = new SpectralDBEntry(scans.get(i), match, dataPoints);
        entry.putAll(metadataMap);
        if (ChimericMsOption.FLAG.equals(handleChimericsOption)) {
          ChimericPrecursorResult chimeric = chimericMap.getOrDefault(scans.get(i).getDataFile(),
              ChimericPrecursorResult.PASSED);
          entry.putIfNotNull(DBEntryField.QUALITY_CHIMERIC, chimeric);
        }
        exportEntry(writer, entry);
      }
    }
  }

  private Map<RawDataFile, ChimericPrecursorResult> checkChimericPrecursorIsolation(
      final FeatureListRow row, final List<Scan> scans) {
    // all data files from fragment scans to score if is chimeric
    Map<RawDataFile, ChimericPrecursorResult> chimericMap = new HashMap<>();
    for (Scan scan : scans) {
      RawDataFile raw = scan.getDataFile();
      chimericMap.computeIfAbsent(raw, key -> scoreChimericIsolation(row, raw));
    }
    return chimericMap;
  }

  private ChimericPrecursorResult scoreChimericIsolation(final FeatureListRow row,
      final RawDataFile raw) {
    Feature feature = row.getFeature(raw);
    if (feature == null) {
      return ChimericPrecursorResult.PASSED;
    }
    MassList massList = feature.getRepresentativeScan().getMassList();
    if (massList == null) {
      throw new MissingMassListException(feature.getRepresentativeScan());
    }

    // check for signals in isolation range
    return ChimericPrecursorResult.check(massList, row.getAverageMZ(), mzTolChimericsMainIon,
        mzTolChimericsIsolation, allowedOtherSignalSum);
  }

  private void exportEntry(final BufferedWriter writer, final SpectralDBEntry entry)
      throws IOException {
    switch (format) {
      case msp -> exportMsp(writer, entry);
      case json -> exportGnpsJson(writer, entry);
    }
  }

  private void exportGnpsJson(final BufferedWriter writer, final SpectralDBEntry entry)
      throws IOException {
    String json = GnpsJsonGenerator.generateJSON(entry);
    writer.append(json).append("\n");
  }

  private void exportMsp(final BufferedWriter writer, final SpectralDBEntry entry)
      throws IOException {
    String msp = MSPEntryGenerator.createMSPEntry(entry);
    writer.append(msp);
  }

  @Override
  public String getTaskDescription() {
    return description;
  }

}
