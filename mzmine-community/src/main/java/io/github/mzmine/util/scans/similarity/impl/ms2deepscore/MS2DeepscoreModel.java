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

package io.github.mzmine.util.scans.similarity.impl.ms2deepscore;

import ai.djl.MalformedModelException;
import ai.djl.inference.Predictor;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.TranslateException;
import io.github.mzmine.datamodel.MassSpectrum;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MS2DeepscoreModel extends EmbeddingBasedSimilarity implements AutoCloseable {

  private static final Logger logger = Logger.getLogger(MS2DeepscoreModel.class.getName());
  private final MS2DeepscoreSpectrumTensorizer spectrumTensorizer;
  private final NDManager ndManager;
  private final Predictor<NDList, NDList> predictor;
  private final ZooModel<NDList, NDList> model;

  public MS2DeepscoreModel(File modelFilePath, File settingsFilePath)
      throws ModelNotFoundException, MalformedModelException, IOException {
    this(modelFilePath.toPath(), settingsFilePath.toPath());
  }

  public MS2DeepscoreModel(Path modelFilePath, Path settingsFilePath)
      throws ModelNotFoundException, MalformedModelException, IOException {
//        todo load settings as well.
    Criteria<NDList, NDList> criteria = Criteria.builder().setTypes(NDList.class, NDList.class)
        .optModelPath(modelFilePath)
        .optOption("mapLocation", "true") // this model requires mapLocation for GPU
        .optProgress(new ProgressBar()).build();

    /*
     * Predicts the MS2Deepscore embedding
     * Model is autocloseable
     */
    model = criteria.loadModel();
    MS2DeepscoreSettings settings = MS2DeepscoreSettings.load(settingsFilePath.toFile());
    // TODO just read json to record and compare those
    if (!Arrays.deepToString(settings.additionalMetadata()).equals(
        "[[StandardScaler, {metadata_field=precursor_mz, mean=0.0, standard_deviation=1000.0}], [CategoricalToBinary, {metadata_field=ionmode, entries_becoming_one=positive, entries_becoming_zero=negative}]]")) {
      throw new RuntimeException(
          "The model uses an additional metadata format that is not supported. Please use the default MS2Deepscore model or ask the developers for support.");
    }
    this.spectrumTensorizer = new MS2DeepscoreSpectrumTensorizer(settings);
    this.ndManager = NDManager.newBaseManager();
    this.predictor = model.newPredictor();
  }


  public NDArray predictEmbeddingFromTensors(TensorizedSpectra tensorizedSpectra)
      throws TranslateException {
    NDList predictions = predictor.predict(
        new NDList(ndManager.create(tensorizedSpectra.tensorizedFragments()),
            ndManager.create(tensorizedSpectra.tensorizedMetadata())));
    return predictions.getFirst();
  }

  @Override
  public NDArray predictEmbedding(List<? extends MassSpectrum> scans) throws TranslateException {
    TensorizedSpectra tensorizedSepctra = spectrumTensorizer.tensorizeSpectra(scans);
    return predictEmbeddingFromTensors(tensorizedSepctra);
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
}
