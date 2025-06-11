/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.group_spectral_networking.dreams;

import static io.github.mzmine.modules.dataprocessing.group_spectral_networking.ms2deepscore.MS2DeepscoreNetworkingTask.convertMatrixToR2RMap;
import static io.github.mzmine.modules.dataprocessing.group_spectral_networking.ms2deepscore.MS2DeepscoreNetworkingTask.getScanAndApplyPrechecks;
import static io.github.mzmine.util.collections.CollectionUtils.argsortReversed;
import static io.github.mzmine.util.scans.similarity.impl.ms2deepscore.EmbeddingBasedSimilarity.dotProduct;

import ai.djl.MalformedModelException;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDArrays;
import ai.djl.ndarray.NDList;
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
import io.github.mzmine.modules.dataprocessing.group_spectral_networking.MainSpectralNetworkingParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractFeatureListTask;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.scans.FragmentScanSelection;
import io.github.mzmine.util.scans.similarity.impl.DreaMS.DreaMSModel;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class DreaMSNetworkingTask extends AbstractFeatureListTask {

  private static final Logger logger = Logger.getLogger(DreaMSNetworkingTask.class.getName());
  private final @NotNull FeatureList[] featureLists;
  private final FragmentScanSelection scanMergeSelect;
  private int processedItems = 0, totalItems;
  private final double minScore;
  private final Integer numNeighbors;
  private final double minScoreNeighbors;
  private final int batchSize;
  private final File dreamsModelFile;
  private final File dreamsSettingsFile;
  private String description;

  /**
   * Constructor is used to extract all parameters
   *
   * @param featureLists   data source is featureList
   * @param mainParameters user parameters {@link MainSpectralNetworkingParameters}
   */
  public DreaMSNetworkingTask(MZmineProject project, @NotNull FeatureList[] featureLists,
      ParameterSet mainParameters, @Nullable MemoryMapStorage storage,
      @NotNull Instant moduleCallDate, @NotNull Class<? extends MZmineModule> moduleClass) {
    super(storage, moduleCallDate, mainParameters, moduleClass);
    var subParams = mainParameters.getEmbeddedParameterValue(
        MainSpectralNetworkingParameters.algorithms);

    this.featureLists = featureLists;
    // Get parameter values
    this.scanMergeSelect = subParams.getParameter(DreaMSNetworkingParameters.spectraMergeSelect)
        .createFragmentScanSelection(getMemoryMapStorage());

    minScore = subParams.getValue(DreaMSNetworkingParameters.minScore);
    if (subParams.getValue(DreaMSNetworkingParameters.kNN)) {
      numNeighbors = subParams.getEmbeddedParameterValue(DreaMSNetworkingParameters.kNN)
          .getValue(DreaMSNetworkingKNNParameter.numNeighbors);
      minScoreNeighbors = subParams.getEmbeddedParameterValue(DreaMSNetworkingParameters.kNN)
          .getValue(DreaMSNetworkingKNNParameter.minScoreNeighbors);
    } else {
      minScoreNeighbors = 0;
      numNeighbors = null;
    }
    batchSize = subParams.getValue(DreaMSNetworkingParameters.batchSize);
    dreamsModelFile = subParams.getValue(DreaMSNetworkingParameters.dreaMSModelFile);
    // same folder - same name
    dreamsSettingsFile = DreaMSNetworkingParameters.findModelSettingsFile(dreamsModelFile);

    // Get the total number of fragmentation spectra in all feature lists
    totalItems = (int) Arrays.stream(featureLists)
        .flatMap(featureList -> featureList.getRows().stream())
        .map(FeatureListRow::getMostIntenseFragmentScan).filter(Objects::nonNull).count();
  }

  @Override
  protected void process() {
    // init model
    description = "Loading model";
    // auto close model after use
    try (var model = new DreaMSModel(dreamsModelFile, dreamsSettingsFile)) {
      description = "Calculating DreaMS similarity";
      // each feature list
      for (FeatureList featureList : featureLists) {
        processFeatureList(featureList, model);
      }
    } catch (ModelNotFoundException | MalformedModelException | IOException e) {
      error("Error in DreaMS model " + e.getMessage(), e);
    }
  }

  private void processFeatureList(FeatureList featureList, DreaMSModel model) {
    description = "Calculating DreaMS similarities";
    List<Scan> scanList = new ArrayList<>();
    List<FeatureListRow> featureListRows = new ArrayList<>();

    for (FeatureListRow row : featureList.getRows()) {

      final Scan scan = getScanAndApplyPrechecks(row, scanMergeSelect, 1);
      if (scan != null) {
        scanList.add(scan);
        featureListRows.add(row);
      }
    }

    // Predict the matrix of pairwise DreaMS similarities
    float[][] similarityMatrix;
    try {
      // Pre-process mass spectra
      float[][][] tensorizedSpectra = model.getSpectrumTensorizer().tensorizeSpectra(scanList);

      // Process the tensorizedSpectra in batches
      int totalSpectra = tensorizedSpectra.length;
      NDList allPredictions = new NDList();

      for (int start = 0; start < totalSpectra; start += batchSize) {
        // Determine the end index for the current batch
        int end = Math.min(start + batchSize, totalSpectra);

        // Extract the batch as a 3D slice
        float[][][] batchSlices = new float[end - start][][];
        System.arraycopy(tensorizedSpectra, start, batchSlices, 0, end - start);

        // Predict embeddings for the current batch
        processedItems += end - start;
        NDArray batchPredictions = model.predictEmbeddingFromTensors(batchSlices);

        // Add the predictions for the current batch to all predictions
        allPredictions.add(batchPredictions);
      }

      // Combine all predictions into a single NDArray (stacked along the batch dimension)
      NDArray embeddings = NDArrays.concat(allPredictions);

      // Compute DreaMS similarities
      similarityMatrix = dotProduct(embeddings, embeddings);
    } catch (TranslateException e) {
      throw new RuntimeException(e);
    }

    // Choose numNeighbors nearest neighbors for each spectrum and retain all similarities above minScore
    if (numNeighbors != null) {
      similarityMatrix = toKNNMatrix(similarityMatrix, numNeighbors, minScore);
    }

    // Convert the similarity matrix to a R2RMap
    R2RMap<R2RSimpleSimilarity> relationsMap = convertMatrixToR2RMap(featureListRows,
        similarityMatrix, numNeighbors != null ? minScoreNeighbors : minScore, Type.DREAMS);
    R2RNetworkingMaps rowMaps = featureList.getRowMaps();
    rowMaps.addAllRowsRelationships(relationsMap, Type.DREAMS);

    // stats are currently only available for modified cosine
    // addNetworkStatisticsToRows(featureList, rowMaps);
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  public double getFinishedPercentage() {
    if (totalItems == 0) {
      return 0;
    } else {
      return (double) processedItems / totalItems;
    }
  }

  @Override
  public String getTaskDescription() {
    return description;
  }

  @Override
  protected @NotNull List<FeatureList> getProcessedFeatureLists() {
    return List.of(featureLists);
  }

  public static float[][] toKNNMatrix(float[][] matrix, int k, double retainElementsAbove) {
    // Create a new matrix to store the result
    int n = matrix.length;
    float[][] knnMatrix = new float[n][n];

    // Iterate through each row
    for (int i = 0; i < n; i++) {
      // Get the indices sorted by values in descending order
      int[] sortedIndices = argsortReversed(matrix[i]);

      // Retain the k largest non-diagonal elements and any element above retainElementsAbove
      int count = 0;
      for (int j = 0; j < n; j++) {
        int col = sortedIndices[j];

        if (count >= k && matrix[i][col] <= retainElementsAbove) {
          break;
        }

        if (col != i) { // Skip the diagonal
          knnMatrix[i][col] = matrix[i][col];
          knnMatrix[col][i] = matrix[i][col]; // Enforce symmetry
          count++;
        }
      }

      // Retain the diagonal as it is
      knnMatrix[i][i] = matrix[i][i];
    }

    return knnMatrix;
  }

}
