package io.github.mzmine.util.scans.PeakClassification;

import ai.djl.Model;
import ai.djl.ndarray.NDManager;
import ai.djl.translate.Translator;
import ai.djl.inference.Predictor;
import ai.djl.translate.TranslateException;
import java.util.List;
import java.io.InputStream;



public class PeakClassifierModel {
    private static final String modelDir = "/MLModels/traced_model_float64.pt";
    private final Model model;
    private final NDManager manager;
    private final Translator<double[][], double[]> translator;
    public final Predictor<double[][], double[]> predictor;

    //loads new model and instanciates necessary resources
    public PeakClassifierModel() {
        this.model = Model.newInstance("PeakClassifier");
        try {
            final InputStream resourceAsStream = this.getClass().getResourceAsStream(modelDir);
            model.load(resourceAsStream);
        } catch (Exception e) {
            System.out.println("Encountered exception when loading the model.");
            e.printStackTrace();
        }
        this.manager = NDManager.newBaseManager();
        this.translator = new PeakClassifierTranslator(this.manager);
        this.predictor = model.newPredictor(this.translator);
    }

    //closes all resources
    public void closeModel(){
        this.manager.close();
        this.predictor.close();
        this.model.close();
    }
    //predicts probabilites for a single standard region of shape (2,64)
    public double[] predictFromArray(double[][] inputArray){
        try{
            return this.predictor.predict(inputArray);
        } catch(TranslateException e){
            System.out.println("Encountered exception when trying to predict probabilities.");
            e.printStackTrace();
            return null;
        }
    }

    //predicts from a list of standard regions at once and returns a list of predictions
    public List<double[]> predictFromBatch(List<double[][]> batchInput){
        try{
            return this.predictor.batchPredict(batchInput);
        } catch(TranslateException e){
            System.out.println("Encountered exception when trying to predict a batch of probabilities.");
            e.printStackTrace();;
            return null;
        }
    }
    
}
