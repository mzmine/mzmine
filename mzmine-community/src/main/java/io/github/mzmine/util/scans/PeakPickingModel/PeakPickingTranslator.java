package io.github.mzmine.util.scans.PeakPickingModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;
import ai.djl.translate.Batchifier;

public class PeakPickingTranslator implements Translator<double[], Map<String, double[]>>{
    private final NDManager manager;
    public PeakPickingTranslator(NDManager manager){
        this.manager = manager;
    }
    
    @Override
    public NDList processInput(TranslatorContext ctx, double[] input){
        NDArray array = this.manager.create(input);
        return new NDList(array);
    }
    
    @Override
    public Map<String, double[]> processOutput(TranslatorContext ctx, NDList output){
        Map<String, double[]> outputMap = new HashMap<>();
        double[] predictedLabels = output.get(0).toDoubleArray(); 
        outputMap.put("probs", predictedLabels);
        double[] predictedPeaks = output.get(1).toDoubleArray();
        outputMap.put("peaks", predictedPeaks);
        //predictedRanges is a (2,n) tensor that is flattend
        double[] predictedRanges = output.get(2).transpose().toDoubleArray();
        int numFeatures = predictedLabels.length;
        //The original output is of shape (numFeatures,2). After flattening 
        //double[] leftBounds = IntStream.range(0,numFeatures).map(i -> i*2).mapToDouble(i -> predictedRanges[i]).toArray(); 
        double[] leftBounds = Arrays.copyOfRange(predictedRanges, 0, numFeatures);
        outputMap.put("left", leftBounds);
        //double[] rightBounds = IntStream.range(0,numFeatures).map(i -> i*2 +1).mapToDouble(i -> predictedRanges[i]).toArray(); 
        double[] rightBounds = Arrays.copyOfRange(predictedRanges, numFeatures, 2*numFeatures);
        outputMap.put("right", rightBounds);
        return outputMap; 
    }
    
    @Override
    public Batchifier getBatchifier(){
        return Batchifier.STACK;
    }
    
}
