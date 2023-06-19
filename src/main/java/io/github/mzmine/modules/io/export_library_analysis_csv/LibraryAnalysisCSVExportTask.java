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

package io.github.mzmine.modules.io.export_library_analysis_csv;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.isotopes.MassListDeisotoper;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.isotopes.MassListDeisotoperParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.DataPointSorter;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.io.CSVUtils;
import io.github.mzmine.util.maths.similarity.Similarity;
import io.github.mzmine.util.scans.ScanAlignment;
import io.github.mzmine.util.scans.ScanUtils;
import io.github.mzmine.util.scans.similarity.Weights;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.SpectralLibrary;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntry;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;

public class LibraryAnalysisCSVExportTask extends AbstractTask {

  /**
   * Nodes file
   */
  private static final String[] NODES = {"ID", "name", "mz", "mass", "adduct", "formula",
      "ion_mode", "instrument", "instrument_type", "smiles", "inchi", "inchi_key", "signals"};

  /**
   * Edges file with correlations
   */
  private static final String[] LIB = {"IDa", "IDb"};
  private static final String[] SIM_TYPES = {"cos", "modcos", "nl"};
  private static final String[] VALUES = {"matched_n", "matched_rel", "matched_intensity",
      "matched_intensity_a", "matched_intensity_b", "score", "max_contribution",
      "signal_contributions", "signals_contr_gr_0_05"};
  private static final String EMPTY_VALUES = ",,,,,,,,";

  private static final DecimalFormat format = new DecimalFormat("0.000");
  private static final Logger logger = Logger.getLogger(
      LibraryAnalysisCSVExportTask.class.getName());
  private final String fieldSeparator;
  private final MZTolerance mzTol;
  private final Integer minMatchedSignals;
  private final Boolean deisotope;
  private final MassListDeisotoperParameters deisotoperParameters;
  private final AtomicLong processedTypes = new AtomicLong(0);
  private final List<SpectralLibrary> libraries;
  private final Weights weights;
  private final boolean applyRemovePrecursorRange;
  private final MZTolerance removePrecursorRange;
  // parameter values
  private File fileName;
  private long totalTypes = 0;

  public LibraryAnalysisCSVExportTask(ParameterSet parameters, @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate); // no new data stored -> null
    libraries = parameters.getParameter(LibraryAnalysisCSVExportParameters.libraries).getValue()
        .getMatchingLibraries();
    fileName = parameters.getParameter(LibraryAnalysisCSVExportParameters.filename).getValue();
    fieldSeparator = parameters.getParameter(LibraryAnalysisCSVExportParameters.fieldSeparator)
        .getValue();
    weights = parameters.getValue(LibraryAnalysisCSVExportParameters.weight);
    mzTol = parameters.getValue(LibraryAnalysisCSVExportParameters.mzTolerance);
    minMatchedSignals = parameters.getValue(LibraryAnalysisCSVExportParameters.minMatch);
    applyRemovePrecursorRange = parameters.getParameter(
        LibraryAnalysisCSVExportParameters.removePrecursorRange).getValue();
    removePrecursorRange = parameters.getParameter(
        LibraryAnalysisCSVExportParameters.removePrecursorRange).getEmbeddedParameter().getValue();

    deisotope = parameters.getParameter(LibraryAnalysisCSVExportParameters.deisotoping).getValue();
    deisotoperParameters =
        deisotope ? parameters.getParameter(LibraryAnalysisCSVExportParameters.deisotoping)
            .getEmbeddedParameters() : null;
  }


  @Override
  public double getFinishedPercentage() {
    if (totalTypes == 0) {
      return 0;
    }
    return (double) processedTypes.get() / (double) totalTypes;
  }

  @Override
  public String getTaskDescription() {
    return String.format("Exporting library correlations to CSV file: %d / %d",
        processedTypes.get(), totalTypes);
  }

  /**
   * @return true if data point is accepted and matches all requirements
   */
  public boolean filter(double precursorMZ, DataPoint dp) {
    // remove data point if
    return !applyRemovePrecursorRange || !removePrecursorRange.checkWithinTolerance(precursorMZ,
        dp.getMZ());
  }


  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    // Total number of rows
    for (var lib : libraries) {
      totalTypes += lib.size();
    }
    long noMz = 0;
    long lessSignals = 0;
    long containsZeroIntensity = 0;

    // prepare all spectra by filtering and weighting
    List<FilteredSpec> spectra = new ArrayList<>();
    // prepare the spectra
    for (var lib : libraries) {
      for (SpectralLibraryEntry entry : lib.getEntries()) {
        final Double mz = entry.getPrecursorMZ();
        if (mz == null) {
          noMz++;
          continue;
        }
        // filter data points
        boolean containsZero = false;
        List<DataPoint> filtered = new ArrayList<>();
        for (DataPoint dp : entry.getDataPoints()) {
          if (Double.compare(dp.getIntensity(), 0) <= 0) {
            containsZero = true;
            break;
          }
          if (!filter(mz, dp)) {
            continue;
          }
          // apply weights
          filtered.add(weights.getWeighted(dp));
        }
        // filtering finished
        if (containsZero) {
          containsZeroIntensity++;
        } else {
          DataPoint[] filteredArray = filtered.toArray(DataPoint[]::new);
          // remove isotopes
          if (deisotope) {
            filteredArray = MassListDeisotoper.filterIsotopes(filteredArray, deisotoperParameters);
          }
          if (filteredArray.length < minMatchedSignals) {
            lessSignals++;
          } else {
            // sort by intensity for spectral matching later and add
            final DataPoint[] neutralLosses = ScanUtils.getNeutralLossSpectrum(filteredArray, mz);
            Arrays.sort(filteredArray, DataPointSorter.DEFAULT_INTENSITY);
            Arrays.sort(neutralLosses, DataPointSorter.DEFAULT_INTENSITY);
            spectra.add(new FilteredSpec(entry, filteredArray, neutralLosses, mz));
          }
        }
      }
    }

    final int numSpec = spectra.size();
    logger.info(String.format(
        "Prepared all library spectra %d. Filtered %d without precursor m/z; %d below %d signals; %d with zero intensity values",
        numSpec, noMz, lessSignals, minMatchedSignals, containsZeroIntensity));

    List<FilteredSpec[]> pairs = new ArrayList<>();
    for (int i = 0; i < spectra.size() - 1; i++) {
      for (int k = i + 1; k < spectra.size(); k++) {
        pairs.add(new FilteredSpec[]{spectra.get(i), spectra.get(k)});
      }
    }
    totalTypes = pairs.size();

    // minus this task
    int threads = MZmineCore.getConfiguration().getNumOfThreads() - 1;
    int size = (int) Math.ceil(totalTypes / (double) threads);
    final List<List<FilteredSpec[]>> parts = ListUtils.partition(pairs, size);

    List<AbstractTask> tasks = new ArrayList<>(threads);
    ConcurrentLinkedDeque<String> outputList = new ConcurrentLinkedDeque<>();
    for (List<FilteredSpec[]> sub : parts) {
      LibraryAnalysisSubTask task = new LibraryAnalysisSubTask(getModuleCallDate(), this, sub,
          outputList);
      MZmineCore.getTaskController().addTask(task, TaskPriority.HIGH);
      tasks.add(task);
    }

    // write node table
    try {
      writeNodeTable(fileName, spectra);
    } catch (IOException e) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Could not open file " + fileName + " for writing nodes file.");
      logger.log(Level.WARNING, String.format(
          "Error writing spectral similarity CSV format to file: %s for libraries: %s. Message: %s",
          fileName.getAbsolutePath(),
          libraries.stream().map(SpectralLibrary::getName).collect(Collectors.joining(",")),
          e.getMessage()), e);
      return;
    }

    // edges file
    fileName = FileAndPathUtil.getRealFilePath(fileName, "csv");
    // Open file
    try (BufferedWriter writer = Files.newBufferedWriter(fileName.toPath(),
        StandardCharsets.UTF_8)) {

      // header
      for (String s : LIB) {
        writer.append(s).append(fieldSeparator);
      }
      writer.append(
          Arrays.stream(SIM_TYPES).flatMap(sim -> Arrays.stream(VALUES).map(v -> sim + "_" + v))
              .collect(Collectors.joining(fieldSeparator)));
      // add comparison

      writer.append("\n");

      // the sub tasks are populating the outputList
      // track finished tasks
      String line;
      while (tasks.size() > 0 || outputList.size() > 0) {
        Thread.sleep(20);
        // write data
        while (outputList.size() > 0) {
          line = outputList.remove();
          if (line != null) {
            writer.append(line).append("\n");
          }
          processedTypes.incrementAndGet();
        }

        // remove all finished tasks to track progress
        tasks.removeIf(t -> t.isCanceled() || t.isFinished());
      }

    } catch (IOException e) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Could not open file " + fileName + " for writing.");
      logger.log(Level.WARNING, String.format(
          "Error writing spectral similarity CSV format to file: %s for libraries: %s. Message: %s",
          fileName.getAbsolutePath(),
          libraries.stream().map(SpectralLibrary::getName).collect(Collectors.joining(",")),
          e.getMessage()), e);
      return;
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    if (getStatus() == TaskStatus.PROCESSING) {
      setStatus(TaskStatus.FINISHED);
    }
  }

  private void writeNodeTable(File fileName, List<FilteredSpec> spectra) throws IOException {
    final String name = FilenameUtils.removeExtension(fileName.getName());
    File full = new File(fileName.getParentFile(), name + "_nodes.csv");

    // Open file
    try (BufferedWriter writer = Files.newBufferedWriter(full.toPath(), StandardCharsets.UTF_8)) {
      // header
      String header = String.join(fieldSeparator, NODES);
      writer.append(header).append("\n");
      // data
      for (var spec : spectra) {
        StringBuilder line = new StringBuilder();
        final SpectralLibraryEntry ea = spec.entry();
        append(line, ea.getOrElse(DBEntryField.ENTRY_ID, ""));
        append(line, ea.getOrElse(DBEntryField.NAME, ""));
        append(line, ea.getField(DBEntryField.PRECURSOR_MZ).map(Object::toString).orElse(""));
        append(line, ea.getField(DBEntryField.EXACT_MASS).map(Object::toString).orElse(""));
        append(line, ea.getOrElse(DBEntryField.ION_TYPE, ""));
        append(line, ea.getOrElse(DBEntryField.FORMULA, ""));
        append(line, ea.getOrElse(DBEntryField.POLARITY, ""));
        append(line, ea.getOrElse(DBEntryField.INSTRUMENT, ""));
        append(line, ea.getOrElse(DBEntryField.INSTRUMENT_TYPE, ""));
        append(line, ea.getOrElse(DBEntryField.SMILES, ""));
        append(line, ea.getOrElse(DBEntryField.INCHI, ""));
        append(line, ea.getOrElse(DBEntryField.INCHIKEY, ""));
        // last without separator
        line.append(spec.dps().length);
        writer.append(line.toString()).append("\n");
      }
    }
  }

  public String matchToCsvString(FilteredSpec a, FilteredSpec b) {
    String cos = getSimilarity(a.dps(), b.dps(), a.precursorMZ(), b.precursorMZ(), false);
    String modcos = getSimilarity(a.dps(), b.dps(), a.precursorMZ(), b.precursorMZ(), true);
    String nl = getSimilarity(a.neutralLosses(), b.neutralLosses(), a.precursorMZ(),
        b.precursorMZ(), false);

    if (cos == null && modcos == null && nl == null) {
      return null;
    }

    if (cos == null) {
      cos = EMPTY_VALUES;
    }
    if (modcos == null) {
      modcos = EMPTY_VALUES;
    }
    if (nl == null) {
      nl = EMPTY_VALUES;
    }

    StringBuilder line = new StringBuilder();
    // add library specifics
    final SpectralLibraryEntry ea = a.entry();
    final SpectralLibraryEntry eb = b.entry();

    append(line, ea.getOrElse(DBEntryField.ENTRY_ID, ""));
    append(line, eb.getOrElse(DBEntryField.ENTRY_ID, ""));

    // add similarities
    line.append(cos).append(fieldSeparator);
    line.append(modcos).append(fieldSeparator);
    line.append(nl);

    processedTypes.incrementAndGet();
    return line.toString();
  }

  private void append(StringBuilder line, String val) {
    line.append(csvEscape(val)).append(fieldSeparator);
  }

  public String getSimilarity(DataPoint[] sortedA, DataPoint[] sortedB, double precursorMzA,
      double precursorMzB, boolean modAware) {

    // align
    final List<DataPoint[]> aligned = alignDataPoints(sortedA, sortedB, precursorMzA, precursorMzB,
        modAware);
    int matched = calcOverlap(aligned);

    if (matched < minMatchedSignals) {
      return null;
    }

    double matchedRel = matched / (double) aligned.size();

    // cosine
    double[][] diffArray = ScanAlignment.toIntensityArray(aligned);

    final double cosineDivisor = Similarity.cosineDivisor(diffArray);

    int signalsGr0_05 = 0;
    double maxContribution = 0;
    double totalIntensityA = 0;
    double totalIntensityB = 0;
    double explainedIntensityA = 0;
    double explainedIntensityB = 0;
    double cosine = 0;
    double[] contributions = new double[diffArray.length];
    for (int i = 0; i < diffArray.length; i++) {
      final double[] pair = diffArray[i];
      contributions[i] = Similarity.cosineSignalContribution(pair, cosineDivisor);
      cosine += contributions[i];
      totalIntensityA += pair[0];
      totalIntensityB += pair[1];

      if (pair[0] > 0 && pair[1] > 0) {
        // matched
        explainedIntensityA += pair[0];
        explainedIntensityB += pair[1];
      }
      if (contributions[i] >= 0.05) {
        signalsGr0_05++;
      }

      if (contributions[i] > maxContribution) {
        maxContribution = contributions[i];
      }
    }
    explainedIntensityA /= totalIntensityA;
    explainedIntensityB /= totalIntensityB;
    double explainedIntensity = (explainedIntensityA + explainedIntensityB) / 2d;

    // sort by contribution
    final String contributionString = Arrays.stream(contributions).filter(d -> d >= 0.001).boxed()
        .sorted((a, b) -> Double.compare(b, a)).map(format::format).limit(4)
        .collect(Collectors.joining(";"));

    final String line =
        matched + fieldSeparator + format.format(matchedRel) + fieldSeparator + format.format(
            explainedIntensity) + fieldSeparator + format.format(explainedIntensityA)
            + fieldSeparator + format.format(explainedIntensityB) + fieldSeparator + format.format(
            cosine) + fieldSeparator + format.format(maxContribution) + fieldSeparator
            + contributionString + fieldSeparator + format.format(signalsGr0_05);
    return line;
  }

  /**
   * Calculate overlap
   *
   * @return number of aligned signals
   */
  protected int calcOverlap(List<DataPoint[]> aligned) {
    int n = 0;
    for (var pair : aligned) {
      if (pair[0] != null && pair[1] != null) {
        n++;
      }
    }
    return n;
  }

  @NotNull
  private List<DataPoint[]> alignDataPoints(DataPoint[] sortedA, DataPoint[] sortedB,
      double precursorMzA, double precursorMzB, boolean modAware) {
    if (modAware) {
      return ScanAlignment.alignOfSortedModAware(mzTol, sortedA, sortedB, precursorMzA,
          precursorMzB);
    } else {
      return ScanAlignment.alignOfSorted(mzTol, sortedA, sortedB);
    }
  }

  private String csvEscape(String input) {
    return CSVUtils.escape(input, fieldSeparator);
  }

}
