/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

import static io.github.mzmine.modules.dataprocessing.group_spectral_networking.SpectralNetworkingTask.addNetworkStatisticsToRows;

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
import io.github.mzmine.datamodel.features.correlation.RowsRelationship.Type;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.filenames.DirectoryParameter;
import io.github.mzmine.taskcontrol.AbstractFeatureListTask;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.scans.similarity.impl.ms2deepscore.MS2DeepscoreModel;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
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
class MS2DeepscoreTask extends AbstractFeatureListTask {

  private static final Logger logger = Logger.getLogger(MS2DeepscoreTask.class.getName());
  private final @NotNull FeatureList[] featureLists;
  private final int minSignals;
  private final double minScore;
  private Path ms2deepscoreModelFile;
  private Path ms2deepscoreSettingsFile;
  private final boolean downloadModel;
  private final File downloadDirectory;

  private String description;

  /**
   * Constructor is used to extract all parameters
   *
   * @param featureLists data source is featureList
   * @param parameters   user parameters
   */
  public MS2DeepscoreTask(MZmineProject project, @NotNull FeatureList[] featureLists,
      ParameterSet parameters, @Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate,
      @NotNull Class<? extends MZmineModule> moduleClass) {
    super(storage, moduleCallDate, parameters, moduleClass);
    this.featureLists = featureLists;
    // Get parameter values for easier use
    minSignals = parameters.getValue(MS2DeepscoreParameters.minSignals);
    minScore = parameters.getValue(MS2DeepscoreParameters.minScore);
    ms2deepscoreModelFile = parameters.getValue(MS2DeepscoreParameters.ms2deepscoreModelFile)
        .toPath();
    ms2deepscoreSettingsFile = parameters.getValue(MS2DeepscoreParameters.ms2deepscoreSettingsFile)
        .toPath();
    downloadModel = parameters.getValue(MS2DeepscoreParameters.downloadDirectory);
    if (downloadModel) {
      final DirectoryParameter directoryParameter = parameters.getParameter(
          MS2DeepscoreParameters.downloadDirectory).getEmbeddedParameter();
      downloadDirectory = directoryParameter.getValue();
    } else {
      downloadDirectory = null;
    }
  }

  @Override
  protected void process() {
    description = "Downloading model";
    if (downloadModel) {
      logger.info("Downloading MS2Deepscore model");
      ms2deepscoreSettingsFile = DownloadMS2DeepscoreModel.downloadSettings(downloadDirectory)
          .toPath();

      ms2deepscoreModelFile = DownloadMS2DeepscoreModel.downloadModel(downloadDirectory).toPath();
    }
    // init model
    description = "Loading model";
    MS2DeepscoreModel model;
    try {
      model = new MS2DeepscoreModel(ms2deepscoreModelFile, ms2deepscoreSettingsFile);
    } catch (ModelNotFoundException | MalformedModelException | IOException e) {
      throw new RuntimeException(e);
    }
    description = "Calculating MS2Deepscore similarity";
    // estimate work load - like how many elements to process
    totalItems = Arrays.stream(featureLists).mapToLong(FeatureList::getNumberOfRows).sum();
    // each feature list
    for (FeatureList featureList : featureLists) {
      processFeatureList(featureList, model);
    }

    // increment progress
    incrementFinishedItems();
  }

  private void processFeatureList(FeatureList featureList, MS2DeepscoreModel model) {

    List<Scan> scanList = new ArrayList<>();
    List<FeatureListRow> featureListRows = new ArrayList<>();

    for (FeatureListRow row : featureList.getRows()) {
      Scan scan = row.getMostIntenseFragmentScan();
      if (scan == null) {
        continue;
      }
      if (scan.getMassList().getNumberOfDataPoints() >= minSignals) {
        scanList.add(scan);
        featureListRows.add(row);
      }
    }
    Scan[] scans = scanList.toArray(new Scan[0]);

    float[][] similarityMatrix;
    try {
      similarityMatrix = model.predictMatrixSymmetric(scans);
    } catch (TranslateException e) {
      throw new RuntimeException(e);
    }
    description = "Calculate MS2Deepscore similarity";
//    Convert the similarity matrix to a R2RMap
    R2RMap<R2RSimpleSimilarity> relationsMap = convertMatrixToR2RMap(featureListRows,
        similarityMatrix);
    R2RNetworkingMaps rowMaps = featureList.getRowMaps();
    rowMaps.addAllRowsRelationships(relationsMap, Type.MS2Deepscore);
    addNetworkStatisticsToRows(featureList, rowMaps);

  }

  public R2RMap<R2RSimpleSimilarity> convertMatrixToR2RMap(List<FeatureListRow> featureListRow,
      float[][] similarityMatrix) {
    final R2RMap<R2RSimpleSimilarity> relationsMap = new R2RMap<>();
    for (int i = 0; i < featureListRow.size(); i++) {
      for (int j = 0; j < featureListRow.size(); j++) {
        if (i != j) {
          float similarityScore = similarityMatrix[i][j];
          if (similarityScore > minScore) {
            var r2r = new R2RSimpleSimilarity(featureListRow.get(i), featureListRow.get(j),
                Type.MS2Deepscore, similarityScore);
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
