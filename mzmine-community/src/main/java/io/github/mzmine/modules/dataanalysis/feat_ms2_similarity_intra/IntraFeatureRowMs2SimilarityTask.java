/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.dataanalysis.feat_ms2_similarity_intra;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.correlation.SpectralSimilarity;
import io.github.mzmine.datamodel.features.types.numbers.IntraFeatureMs2SimilarityType;
import io.github.mzmine.datamodel.features.types.numbers.SimpleStatistics;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.group_spectral_networking.SpectralSignalFilter;
import io.github.mzmine.modules.dataprocessing.group_spectral_networking.modified_cosine.ModifiedCosineSpectralNetworkingTask;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.io.ParallelTextWriterTask;
import io.github.mzmine.util.scans.ScanUtils;
import io.github.mzmine.util.scans.merging.FloatGrouping;
import io.github.mzmine.util.scans.similarity.Weights;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleCollection;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import java.io.File;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class IntraFeatureRowMs2SimilarityTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(
      IntraFeatureRowMs2SimilarityTask.class.getName());

  private final String[] GROUPS = {"<4", "[0.4,0.6)", "[0.6,0.85)", "≥0.85"};
  private final double[] GROUP_THRESHOLDS = {0.4, 0.6, 0.85};
  private final String fieldSeparator = ",";

  private final FeatureList[] featureLists;
  private final File fileName;
  private final MZTolerance mzTol;
  private final int minMatchedSignals;
  private final SpectralSignalFilter signalFilters;
  private final Boolean exportToFile;
  private final NumberFormat scoreFormat = MZmineCore.getConfiguration().getFormats(true)
      .scoreFormat();

  private final AtomicInteger processedRows = new AtomicInteger(0);
  private final ParameterSet parameters;
  private int totalRows = 0;
  private @Nullable ParallelTextWriterTask writerTask;
  private final boolean splitByEnergy;

  public IntraFeatureRowMs2SimilarityTask(ParameterSet parameters,
      @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate);
    this.featureLists = parameters.getParameter(IntraFeatureRowMs2SimilarityParameters.featureLists)
        .getValue().getMatchingFeatureLists();
    exportToFile = parameters.getValue(IntraFeatureRowMs2SimilarityParameters.filename);
    File file = parameters.getEmbeddedParameterValueIfSelectedOrElse(
        IntraFeatureRowMs2SimilarityParameters.filename, null);
    if (file != null) {
      fileName = FileAndPathUtil.getRealFilePath(file, "csv");
    } else {
      fileName = null;
    }
    mzTol = parameters.getValue(IntraFeatureRowMs2SimilarityParameters.mzTol);
    minMatchedSignals = parameters.getValue(
        IntraFeatureRowMs2SimilarityParameters.minMatchedSignals);
    signalFilters = parameters.getValue(IntraFeatureRowMs2SimilarityParameters.signalFilters)
        .createFilter();
    splitByEnergy = parameters.getValue(
        IntraFeatureRowMs2SimilarityParameters.splitByFragmentationEnergy);
    this.parameters = parameters;
  }

  @Override
  public String getTaskDescription() {
    return "Scoring MS2 similarity within features to assess quality.";
  }

  @Override
  public double getFinishedPercentage() {
    return totalRows == 0 ? 0 : processedRows.get() / (double) totalRows;
  }

  @Override
  public void run() {

    setStatus(TaskStatus.PROCESSING);

    // Total number of rows
    for (FeatureList featureList : featureLists) {
      totalRows += featureList.getNumberOfRows();
    }

    if (exportToFile) {
      writerTask = new ParallelTextWriterTask(this, fileName, true);
      String header = String.join(fieldSeparator, new String[]{"row_id", "cosine_similarity"});
      writerTask.appendLine(header);
    }

    // Process feature lists
    for (FeatureList featureList : featureLists) {
      // Cancel?
      if (isCanceled()) {
        return;
      }

      try {
        processFeatureList(featureList);
      } catch (Exception e) {
        setStatus(TaskStatus.ERROR);
        setErrorMessage("Error during intra feature MS2 similarity export " + fileName);
        logger.log(Level.WARNING,
            "Error during compound annotations csv export of feature list: " + featureList.getName()
            + ": " + e.getMessage(), e);
        if (writerTask != null) {
          writerTask.setWriteFinished();
        }
        return;
      }
    }

    if (getStatus() == TaskStatus.PROCESSING) {
      setStatus(TaskStatus.FINISHED);
    }
  }

  private void processFeatureList(FeatureList featureList) {
    long totalMatches = featureList.getRows().stream().filter(FeatureListRow::hasMs2Fragmentation)
        .parallel().mapToLong(row -> {
          int sim = processeRow(row);
          processedRows.incrementAndGet();
          return sim;
        }).sum();
    logger.info("Compared a total of %d MS2 pairs within features".formatted(totalMatches));
    featureList.getAppliedMethods().add(
        new SimpleFeatureListAppliedMethod(IntraFeatureRowMs2SimilarityModule.class, parameters,
            getModuleCallDate()));
  }

  /**
   * Runs in parallel on rows and writes data to file if requested
   *
   * @return number of resulting similarity pairs
   */
  public int processeRow(final FeatureListRow row) {
    List<Scan> scans = row.getAllFragmentScans();
    if (scans.isEmpty()) {
      return 0;
    }

    // split by energy
    Map<FloatGrouping, List<Scan>> byFragmentationEnergy =
        splitByEnergy ? ScanUtils.splitByFragmentationEnergy(scans)
            : Map.of(FloatGrouping.ofUndefined(), scans);

    double[] similarities = byFragmentationEnergy.values().stream().map(this::processScans)
        .flatMapToDouble(DoubleCollection::doubleStream).toArray();

    if (similarities.length == 0) {
      return 0;
    }

    DoubleSummaryStatistics stats = Arrays.stream(similarities).summaryStatistics();
    String group = getGroup(stats.getAverage());
    row.set(IntraFeatureMs2SimilarityType.class, new SimpleStatistics(stats, group));

    String exportData = Arrays.stream(similarities)
        .mapToObj(sim -> row.getID() + fieldSeparator + scoreFormat.format(sim))
        .collect(Collectors.joining("\n"));
    if (writerTask != null) {
      writerTask.appendLine(exportData);
    }
    return similarities.length;
  }

  private String getGroup(final double average) {
    for (int i = 0; i < GROUP_THRESHOLDS.length; i++) {
      if (average < GROUP_THRESHOLDS[i]) {
        return GROUPS[i];
      }
    }
    return GROUPS[GROUPS.length - 1];
  }

  public DoubleList processScans(final List<Scan> scans) {
    // score within group
    List<DataPoint[]> filteredGroup = scans.stream()
        .map(scan -> signalFilters.applyFilterAndSortByIntensity(scan, minMatchedSignals))
        .filter(Objects::nonNull).toList();

    DoubleList similarities = new DoubleArrayList();

    for (int i = 0; i < filteredGroup.size() - 1; i++) {
      DataPoint[] a = filteredGroup.get(i);
      for (int j = i + 1; j < filteredGroup.size(); j++) {
        DataPoint[] b = filteredGroup.get(j);
        SpectralSimilarity sim = ModifiedCosineSpectralNetworkingTask.createMS2Sim(mzTol, a, b, 1,
            Weights.SQRT);
        similarities.add(sim == null ? 0d : sim.cosine());
      }
    }
    return similarities;
  }

}
