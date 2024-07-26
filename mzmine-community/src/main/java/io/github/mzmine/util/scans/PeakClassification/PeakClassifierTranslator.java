package io.github.mzmine.util.scans.PeakClassification;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;
import ai.djl.translate.Batchifier;

public class PeakClassifierTranslator implements Translator<double[][], double[]>{
        private final NDManager manager;

        //receivs NDManager from model 
        public PeakClassifierTranslator(NDManager manager){
            this.manager = manager;
        }

        //translates double[][] into NDList as input
        @Override
        public NDList processInput(TranslatorContext ctx, double[][] input){
        NDArray array = this.manager.create(input);
        return new NDList(array);
        }

        //translate output NDList back to double[]
        @Override
        public double[] processOutput(TranslatorContext ctx, NDList list){
            NDArray array = list.singletonOrThrow();
            return array.toDoubleArray();
        }

        //Implements batch processing. The STACK batchifier stacks input data along a new dimension to obtain a tensor of shape (batchSize, oldDimensions)
        @Override
        public Batchifier getBatchifier(){
            return Batchifier.STACK; 
        }   
}
