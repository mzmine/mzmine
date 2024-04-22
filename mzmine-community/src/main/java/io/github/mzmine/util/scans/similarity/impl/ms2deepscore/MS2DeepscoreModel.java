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

package io.github.mzmine.util.scans.similarity.impl.ms2deepscore;

import ai.djl.MalformedModelException;
import ai.djl.inference.Predictor;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.TranslateException;
import io.github.mzmine.datamodel.Scan;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.util.ArrayList;

public class MS2DeepscoreModel {

  /**
   * Predicts the MS2Deepscore similarity
   */
  private final ZooModel<NDList, NDList> model;

  public MS2DeepscoreModel(URI modelFilePath)
      throws ModelNotFoundException, MalformedModelException, IOException {
//        todo load settings as well.
    Criteria<NDList, NDList> criteria = Criteria.builder().setTypes(NDList.class, NDList.class)
        .optModelPath(Paths.get(modelFilePath))
        .optOption("mapLocation", "true") // this model requires mapLocation for GPU
        .optProgress(new ProgressBar()).build();

    this.model = criteria.loadModel();
  }

  public NDArray predict(NDArray spectrumNDArray1, NDArray metadataNDArray1)
      throws TranslateException {

    Predictor<NDList, NDList> predictor = model.newPredictor();
    NDList predictions = predictor.predict(new NDList(spectrumNDArray1, metadataNDArray1));
    return predictions.getFirst();
  }

  public NDList tensorizeSpectrum(Scan Spectrum) {
//        Tensorizes a spectrum to be able to use as input for the model
//        todo write the method for tensorization, using the settings.
    return new NDList();
  }

  public Double predictPair(Scan Spectrum1, Scan Spectrum2) {
//    Predict the similarity between two spectra
//        todo write method
    return 1.0;
  }

  public Double[][] predictMatrix(ArrayList<Scan> spectra1, ArrayList<Scan> spectra2) {
//    Predict the similarity between two lists of spectra
    //        todo write method
    return new Double[3][3];
  }
}
