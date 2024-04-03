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
import java.nio.file.Paths;
import java.util.ArrayList;

public class MS2DeepscoreModel {
    /**
     * Predicts the MS2Deepscore similarity
     */
    private final ZooModel<NDList, NDList> model;

    public MS2DeepscoreModel(String modelFilePath) throws ModelNotFoundException, MalformedModelException, IOException {
//        todo load settings as well.
        Criteria<NDList, NDList> criteria = Criteria.builder()
                .setTypes(NDList.class, NDList.class)
                .optModelPath(Paths.get(modelFilePath))
                .optOption("mapLocation", "true") // this model requires mapLocation for GPU
                .optProgress(new ProgressBar()).build();

        this.model = criteria.loadModel();
    }

    public NDList predict(NDArray spectrumNDArray1,
                          NDArray spectrumNDArray2,
                          NDArray metadataNDArray1,
                          NDArray metadataNDArray2
    ) throws TranslateException {

        Predictor<NDList, NDList> predictor = model.newPredictor();
        return predictor.predict(new NDList(spectrumNDArray1, spectrumNDArray2, metadataNDArray1, metadataNDArray2));
    }

    public NDList tensorizeSpectrum(Scan Spectrum){
//        Tensorizes a spectrum to be able to use as input for the model
//        todo write the method for tensorization, using the settings.
        return new NDList();
    }

    public Double predictPair(Scan Spectrum1, Scan Spectrum2){
//    Predict the similarity between two spectra
//        todo write method
        return 1.0;
    }
    public Double[][] predictMatrix(ArrayList<Scan> spectra1, ArrayList<Scan> spectra2){
//    Predict the similarity between two lists of spectra
        //        todo write method
        return new Double[3][3];
    }
}
