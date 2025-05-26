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

package io.github.mzmine.modules.dataprocessing.group_spectral_networking.ms2deepscore;

import ai.djl.MalformedModelException;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.translate.TranslateException;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.correlation.R2RMap;
import io.github.mzmine.datamodel.features.correlation.R2RNetworkingMaps;
import io.github.mzmine.datamodel.features.correlation.R2RSimpleSimilarity;
import io.github.mzmine.datamodel.features.correlation.RowsRelationship;
import io.github.mzmine.datamodel.features.correlation.RowsRelationship.Type;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.dataprocessing.group_spectral_networking.MainSpectralNetworkingParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractFeatureListTask;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.exceptions.MissingMassListException;
import io.github.mzmine.util.scans.FragmentScanSelection;
import io.github.mzmine.util.scans.similarity.impl.ms2deepscore.MS2DeepscoreModel;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author niekdejonge
 */
public class MS2DeepscoreNetworkingTask extends AbstractFeatureListTask {

  private static final Logger logger = Logger.getLogger(MS2DeepscoreNetworkingTask.class.getName());
  private final @NotNull FeatureList[] featureLists;
  private final int minSignals;
  private final double minScore;
  private final File ms2deepscoreModelFile;
  private final File ms2deepscoreSettingsFile;
  private final FragmentScanSelection scanMergeSelect;
  private String description;

  /**
   * Constructor is used to extract all parameters
   *
   * @param featureLists   data source is featureList
   * @param mainParameters user parameters {@link MainSpectralNetworkingParameters}
   */
  public MS2DeepscoreNetworkingTask(MZmineProject project, @NotNull FeatureList[] featureLists,
      ParameterSet mainParameters, @Nullable MemoryMapStorage storage,
      @NotNull Instant moduleCallDate, @NotNull Class<? extends MZmineModule> moduleClass) {
    super(storage, moduleCallDate, mainParameters, moduleClass);
    var subParams = mainParameters.getEmbeddedParameterValue(
        MainSpectralNetworkingParameters.algorithms);

    this.scanMergeSelect = subParams.getParameter(
            MS2DeepscoreNetworkingParameters.spectraMergeSelect)
        .createFragmentScanSelection(getMemoryMapStorage());

    this.featureLists = featureLists;
    // Get parameter values for easier use
    minSignals = subParams.getValue(MS2DeepscoreNetworkingParameters.minSignals);
    minScore = subParams.getValue(MS2DeepscoreNetworkingParameters.minScore);
    ms2deepscoreModelFile = subParams.getValue(
        MS2DeepscoreNetworkingParameters.ms2deepscoreModelFile);
    // same folder - same name
    ms2deepscoreSettingsFile = MS2DeepscoreNetworkingParameters.findModelSettingsFile(
        ms2deepscoreModelFile);

    totalItems = Arrays.stream(featureLists).mapToLong(FeatureList::getNumberOfRows).sum();
  }

  @Override
  protected void process() {
    // init model
    description = "Loading model";
    // auto close model after use
    try (var model = new MS2DeepscoreModel(ms2deepscoreModelFile, ms2deepscoreSettingsFile)) {
      description = "Calculating MS2Deepscore similarity";
      // estimate work load - like how many elements to process
      totalItems = Arrays.stream(featureLists).mapToLong(FeatureList::getNumberOfRows).sum();
      // each feature list
      for (FeatureList featureList : featureLists) {
        processFeatureList(featureList, model);
      }
    } catch (ModelNotFoundException | MalformedModelException | IOException e) {
      error("Error in MS2Deepscore model " + e.getMessage(), e);
    }
  }

  private void processFeatureList(FeatureList featureList, MS2DeepscoreModel model) {

    List<Scan> scanList = new ArrayList<>();
    List<FeatureListRow> featureListRows = new ArrayList<>();

    for (FeatureListRow row : featureList.getRows()) {
      // add scan here because the model needs precursor mz and scan polarity
      // later it will extract mass list again for signals
      final Scan scan = getScanAndApplyPrechecks(row, scanMergeSelect, minSignals);
      if (scan != null) {
        scanList.add(scan);
        featureListRows.add(row);
      }
    }

    float[][] similarityMatrix;
    try {
      similarityMatrix = model.predictMatrixSymmetric(scanList);
    } catch (TranslateException e) {
      throw new RuntimeException(e);
    }
    description = "Calculate MS2Deepscore similarity";
//    Convert the similarity matrix to a R2RMap
    R2RMap<R2RSimpleSimilarity> relationsMap = convertMatrixToR2RMap(featureListRows,
        similarityMatrix, minScore, Type.MS2Deepscore);
    R2RNetworkingMaps rowMaps = featureList.getRowMaps();
    rowMaps.addAllRowsRelationships(relationsMap, Type.MS2Deepscore);
    // stats are currently only available for modified cosine
//    addNetworkStatisticsToRows(featureList, rowMaps);

  }

  @Nullable
  public static Scan getScanAndApplyPrechecks(final @NotNull FeatureListRow row,
      final @NotNull FragmentScanSelection scanMergeSelect, final int minSignals) {
    List<Scan> scans = scanMergeSelect.getAllFragmentSpectra(row);
    if (scans.isEmpty()) {
      return null;
    }
    // currently we are only considering one scan per row. otherwise the matrix operations need to be changed
    final Scan scan = scans.getFirst();
    if (scan.getPrecursorMz() == null) {
      return null;
    }

    if (scan.getMassList() == null) {
      throw new MissingMassListException(scan);
    }

    if (scan.getMassList().getNumberOfDataPoints() >= minSignals) {
      return scan;
    }
    return null;
  }

  public static R2RMap<R2RSimpleSimilarity> convertMatrixToR2RMap(
      List<FeatureListRow> featureListRow, float[][] similarityMatrix, double minScore,
      RowsRelationship.Type rowsRelationshipType) {
    final R2RMap<R2RSimpleSimilarity> relationsMap = new R2RMap<>();
    for (int i = 0; i < featureListRow.size(); i++) {
      for (int j = 0; j < featureListRow.size(); j++) {
        if (i != j) {
          float similarityScore = similarityMatrix[i][j];
          if (similarityScore > minScore) {
            var r2r = new R2RSimpleSimilarity(featureListRow.get(i), featureListRow.get(j),
                rowsRelationshipType, similarityScore);
            relationsMap.add(featureListRow.get(i), featureListRow.get(j), r2r);
          }
        }
      }
    }
    return relationsMap;
  }

  @Override
  public String getTaskDescription() {
    return description;
  }

  @Override
  protected @NotNull List<FeatureList> getProcessedFeatureLists() {
    return List.of(featureLists);
  }

}
