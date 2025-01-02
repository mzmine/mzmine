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

package io.github.mzmine.modules.dataprocessing.group_spectral_networking.dreams;

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
import io.github.mzmine.modules.dataprocessing.group_spectral_networking.MainSpectralNetworkingParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractFeatureListTask;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.exceptions.MissingMassListException;
import io.github.mzmine.util.scans.similarity.impl.DreaMS.DreaMSModel;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class DreaMSNetworkingTask extends AbstractFeatureListTask {

    private static final Logger logger = Logger.getLogger(DreaMSNetworkingTask.class.getName());
    private final @NotNull FeatureList[] featureLists;
    private final int minSignals;
    private final double minScore;
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
        // Get parameter values for easier use
        minSignals = subParams.getValue(DreaMSNetworkingParameters.minSignals);
        minScore = subParams.getValue(DreaMSNetworkingParameters.minScore);
        dreamsModelFile = subParams.getValue(
                DreaMSNetworkingParameters.dreaMSModelFile);
        // same folder - same name
        dreamsSettingsFile = DreaMSNetworkingParameters.findModelSettingsFile(
                dreamsModelFile);

        totalItems = Arrays.stream(featureLists).mapToLong(FeatureList::getNumberOfRows).sum();
    }

    @Override
    protected void process() {
        // init model
        description = "Loading model";
        // auto close model after use
        try (var model = new DreaMSModel(dreamsModelFile, dreamsSettingsFile)) {
            description = "Calculating DreaMS similarity";
            // estimate work load - like how many elements to process
            totalItems = Arrays.stream(featureLists).mapToLong(FeatureList::getNumberOfRows).sum();
            // each feature list
            for (FeatureList featureList : featureLists) {
                processFeatureList(featureList, model);
            }
        } catch (ModelNotFoundException | MalformedModelException | IOException e) {
            error("Error in DreaMS model " + e.getMessage(), e);
        }
    }

    private void processFeatureList(FeatureList featureList, DreaMSModel model) {
        List<Scan> scanList = new ArrayList<>();
        List<FeatureListRow> featureListRows = new ArrayList<>();

        for (FeatureListRow row : featureList.getRows()) {
            Scan scan = row.getMostIntenseFragmentScan();
            if (scan == null) {
                continue;
            }
            if (scan.getMassList() == null) {
                throw new MissingMassListException(scan);
            }

            if (scan.getMassList().getNumberOfDataPoints() >= minSignals) {
                // add scan here because the model needs precursor mz and scan polarity
                // later it will extract mass list again for signals
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
        description = "Calculate DreaMS similarity";
//    Convert the similarity matrix to a R2RMap
        R2RMap<R2RSimpleSimilarity> relationsMap = convertMatrixToR2RMap(featureListRows,
                similarityMatrix);
        R2RNetworkingMaps rowMaps = featureList.getRowMaps();
        rowMaps.addAllRowsRelationships(relationsMap, Type.DREAMS);
        // stats are currently only available for modified cosine
//    addNetworkStatisticsToRows(featureList, rowMaps);
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
                                Type.DREAMS, similarityScore);
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
