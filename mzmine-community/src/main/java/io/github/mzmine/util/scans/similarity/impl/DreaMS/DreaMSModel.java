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

package io.github.mzmine.util.scans.similarity.impl.DreaMS;

import ai.djl.MalformedModelException;
import ai.djl.inference.Predictor;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDArrays;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.TranslateException;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.util.scans.similarity.impl.ms2deepscore.EmbeddingBasedSimilarity;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DreaMSModel extends EmbeddingBasedSimilarity implements AutoCloseable {

    private static final Logger logger = Logger.getLogger(DreaMSModel.class.getName());
    private final DreaMSSpectrumTensorizer spectrumTensorizer;
    private final NDManager ndManager;
    private final Predictor<NDList, NDList> predictor;
    private final ZooModel<NDList, NDList> model;

    public DreaMSModel(File modelFilePath, File settingsFilePath)
            throws ModelNotFoundException, MalformedModelException, IOException {
        this(modelFilePath.toPath(), settingsFilePath.toPath());
    }

    public DreaMSModel(Path modelFilePath, Path settingsFilePath)
            throws ModelNotFoundException, MalformedModelException, IOException {
        /*
         * Predicts the DreaMS embedding
         * Model is autocloseable
         */
        Criteria<NDList, NDList> criteria = Criteria.builder().setTypes(NDList.class, NDList.class)
                .optModelPath(modelFilePath)
                .optOption("mapLocation", "true") // this model requires mapLocation for GPU
                .optProgress(new ProgressBar()).build();
        model = criteria.loadModel();

        DreaMSSettings settings = DreaMSSettings.load(settingsFilePath.toFile());
        this.spectrumTensorizer = new DreaMSSpectrumTensorizer(settings);
        this.ndManager = NDManager.newBaseManager();
        this.predictor = model.newPredictor();
    }

    /**
     * Predicts a DreaMS embedding from a tensorized spectrum.
     */
    public NDArray predictEmbeddingFromTensors(float[][][] tensorizedSpectra) throws TranslateException {

        // Convert 3D float[][][] to 3D NDList
        NDList tensorizedList = new NDList();
        for (float[][] spectrum : tensorizedSpectra) {
            NDArray slice = ndManager.create(spectrum);
            tensorizedList.add(slice);
        }
        NDArray tensorizedArray = NDArrays.stack(tensorizedList);

        // Predict embeddings
        NDList predictions = predictor.predict(new NDList(tensorizedArray));

        // Assuming the predictor outputs a single NDArray, return it
        return predictions.singletonOrThrow();
    }

    /**
     * Predicts a DreaMS embedding from a MassSpectrum.
     */
    @Override
    public NDArray predictEmbedding(List<? extends MassSpectrum> scans) throws TranslateException {
        float[][][] tensorizedSpectra = spectrumTensorizer.tensorizeSpectra(scans);
        return predictEmbeddingFromTensors(tensorizedSpectra);
    }

    @Override
    public void close() {
        try {
            ndManager.close();
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Could not close ND manager: " + ex.getMessage(), ex);
        }
        try {
            predictor.close();
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Could not close predictor: " + ex.getMessage(), ex);
        }
        try {
            model.close();
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Could not close model: " + ex.getMessage(), ex);
        }
    }

    public DreaMSSpectrumTensorizer getSpectrumTensorizer() {
        return spectrumTensorizer;
    }
}
