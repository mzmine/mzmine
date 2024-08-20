package io.github.mzmine.util.scans.PeakPickingModel;

import ai.djl.Model;
import ai.djl.translate.Translator;
import ai.djl.inference.Predictor;
import ai.djl.ndarray.NDManager;
import ai.djl.translate.TranslateException;
import java.util.Map;
import java.util.List;
import java.io.InputStream;

public class PeakPickingModel {
    private static final String modelDir = "/MLModels/peakPicking.pt";
    private final Model model;
    private final NDManager manager;
    private final Translator<double[], Map<String,double[]>> translator;
    public final Predictor<double[], Map<String,double[]>> predictor;
    
    public PeakPickingModel(){
        this.model = Model.newInstance("PeakPicking");
        try{
            final InputStream resourceAsStream = this.getClass().getResourceAsStream(modelDir);
            model.load(resourceAsStream);
        } catch (Exception e) {
            System.out.println("Encountered exception when loading the model.");
            e.printStackTrace();
        }
        this.manager = NDManager.newBaseManager();
        this.translator = new PeakPickingTranslator(this.manager);
        this.predictor = model.newPredictor(this.translator);
    }
    
    public void closeModel(){
        this.manager.close();
        this.predictor.close();
        this.model.close();
    }
    
    public Map<String,double[]> singlePrediction(double[] inputArray){
        try{
            return this.predictor.predict(inputArray);
        } catch(TranslateException e){
            System.out.println("Encountered exception when trying to predict a single input.");
            e.printStackTrace();
            return null;
        }
    }
    
    public List<Map<String, double[]>> batchPrediction(List<double[]> inputBatch){
       try{
        return this.predictor.batchPredict(inputBatch);
       } catch (TranslateException e){
        System.out.println("Encountered exception when trying to predict a batch of inputs.");
        e.printStackTrace();
        return null;
       }
    }
    
    public static void main(String[] args){
        PeakPickingModel testModel = new PeakPickingModel();
        double[] testInput = new double[128];
        for (int i=0; i<128; i++){
            testInput[i] = i;
        }
        Map<String, double[]> testPrediction = testModel.singlePrediction(testInput);
        System.out.println(testPrediction);
    }

}
