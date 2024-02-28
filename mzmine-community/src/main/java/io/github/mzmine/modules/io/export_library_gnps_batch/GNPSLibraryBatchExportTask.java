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

package io.github.mzmine.modules.io.export_library_gnps_batch;

import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.modules.io.spectraldbsubmit.formats.MGFEntryGenerator;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.SpectralLibrary;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntry;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;


public class GNPSLibraryBatchExportTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(GNPSLibraryBatchExportTask.class.getName());
  private final String separator = "\t";

  public final List<DBEntryField> columns = List.of(DBEntryField.FILENAME, DBEntryField.PEPTIDE_SEQ,
      DBEntryField.NAME, DBEntryField.PRECURSOR_MZ, DBEntryField.INSTRUMENT_TYPE,
      DBEntryField.ION_SOURCE, DBEntryField.SCAN_NUMBER, DBEntryField.SMILES, DBEntryField.INCHI,
      DBEntryField.INCHIKEY, DBEntryField.CHARGE, DBEntryField.POLARITY, DBEntryField.PUBMED,
      DBEntryField.ACQUISITION, DBEntryField.EXACT_MASS, DBEntryField.DATA_COLLECTOR,
      DBEntryField.ION_TYPE, DBEntryField.CAS, DBEntryField.PRINCIPAL_INVESTIGATOR);
  // fields that are not captured by MZmine so far
  public final Map<String, String> constColumns = Map.of("INTEREST", "N/A", "LIBQUALITY", "1",
      "GENUS", "N/A", "SPECIES", "N/A", "STRAIN", "N/A");

  private final List<SpectralLibrary> libraries;
  private final File tsvFile;
  private final File mgfFile;

  private int totalEntries = 0;
  private int finishedEntries = 0;

  public GNPSLibraryBatchExportTask(ParameterSet parameters,
      final @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate);
    var file = parameters.getValue(GNPSLibraryBatchExportParameters.filename);
    this.tsvFile = FileAndPathUtil.getRealFilePath(file, "tsv");
    this.mgfFile = FileAndPathUtil.getRealFilePath(file, "mgf");
    libraries = parameters.getValue(GNPSLibraryBatchExportParameters.libraries)
        .getMatchingLibraries();
  }

  @Override
  public String getTaskDescription() {
    return "Creating files for GNPS batch library submission";
  }

  @Override
  public double getFinishedPercentage() {
    return totalEntries == 0 ? 0 : finishedEntries / (double) totalEntries;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    totalEntries = libraries.stream().mapToInt(SpectralLibrary::getNumEntries).sum();

    try {
      if (!FileAndPathUtil.createDirectory(mgfFile.getParentFile())) {
        setStatus(TaskStatus.ERROR);
        setErrorMessage("Cannot create directory for file " + mgfFile);
        return;
      }
    } catch (Exception e) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Cannot create directory for file " + mgfFile);
      return;
    }

    try (BufferedWriter mgfWriter = Files.newBufferedWriter(mgfFile.toPath(),
        StandardCharsets.UTF_8)) {
      try (BufferedWriter tsvWriter = Files.newBufferedWriter(tsvFile.toPath(),
          StandardCharsets.UTF_8)) {
        writeHeader(tsvWriter);

        // write the files
        exportLibraries(mgfWriter, tsvWriter);
      } catch (IOException e) {
        setStatus(TaskStatus.ERROR);
        setErrorMessage("Error during compound annotations csv export to " + tsvFile);
        return;
      }
    } catch (IOException e) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Error during compound annotations csv export to " + mgfFile);
      return;
    }

    setStatus(TaskStatus.FINISHED);
  }

  private void writeHeader(final BufferedWriter tsvWriter) throws IOException {
    var hs1 = columns.stream().map(DBEntryField::getGnpsBatchSubmissionID);
    var hs2 = constColumns.keySet().stream();
    var header = Stream.concat(hs1, hs2).collect(Collectors.joining(separator));
    tsvWriter.append(header).append("\n");
  }

  private void exportLibraries(final BufferedWriter mgfWriter, final BufferedWriter tsvWriter)
      throws IOException {
    var ncols = columns.size() + constColumns.size();
    final String[] values = new String[ncols];
    // add constant values
    var cvs = constColumns.values().toArray(String[]::new);
    System.arraycopy(cvs, 0, values, values.length - cvs.length, cvs.length);

    List<String> errors = new ArrayList<>();

    int brokenAndSkippedEntries = 0;

    for (var library : libraries) {
      for (var entry : library.getEntries()) {
        errors.clear();
        // count up first to start with entry 1
        finishedEntries++;
        var mgfEntry = MGFEntryGenerator.createMGFEntry(entry, finishedEntries);
        mgfWriter.append(mgfEntry).append("\n");

        // write header
        for (int i = 0; i < columns.size(); i++) {
          DBEntryField column = columns.get(i);
          try {
            values[i] = getValueOrDefaultOrThrow(entry, column);
          } catch (Exception ex) {
            errors.add(ex.getMessage());
          }
        }
        if (errors.isEmpty()) {
          // write line
          String line = String.join(separator, values);
          tsvWriter.append(line).append("\n");
        } else {
          for (final String error : errors) {
            logger.info("Error in tsv entry for " + entry + ": " + error);
          }
          brokenAndSkippedEntries++;
        }
      }
    }

    if (brokenAndSkippedEntries > 0) {
      logger.info("There were %d broken entries - see log. Exported: %d/%d".formatted(
          brokenAndSkippedEntries, totalEntries - brokenAndSkippedEntries, totalEntries));
    } else {
      logger.info("All entries exported successfully. Total=" + totalEntries);
    }
  }

  private String getValueOrDefaultOrThrow(final SpectralLibraryEntry entry,
      final DBEntryField field) {
    if (field == DBEntryField.SCAN_NUMBER) {
      return String.valueOf(finishedEntries);
    }
    var value = String.valueOf(entry.getOrElse(field, ""));
    if (value.isBlank()) {
      return getDefaultValueOrThrow(field);
    }
    if (field == DBEntryField.POLARITY) {
      // ensure polarity
      return PolarityType.parseFromString(value) == PolarityType.NEGATIVE ? "Negative" : "Positive";
    }
    // TODO handle instrument in parameters?
    return value;
  }

  /**
   * @return default GNPS value for field
   */
  public String getDefaultValueOrThrow(DBEntryField field) {
    return switch (field) {
      case SCAN_NUMBER -> String.valueOf(finishedEntries);
      case PEPTIDE_SEQ -> "*..*";
      case CAS, PUBMED, SMILES, INCHI, INCHIKEY, PRINCIPAL_INVESTIGATOR -> "N/A";
      case CHARGE, EXACT_MASS -> "0";
      case POLARITY -> "Positive";
      case ACQUISITION -> "Crude";
      case FILENAME -> mgfFile.getName();
      default -> throw new UnsupportedOperationException(
          field + " is is a required value but was not set");
    };
  }
}