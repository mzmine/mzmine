package io.github.mzmine.util.scans.PeakPickingModel;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.DataType;
import ai.djl.translate.Batchifier;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;

public class PeakPickingTranslator implements Translator<double[], PeakPickingOutput> {

  @Override
    public NDList processInput(TranslatorContext ctx, double[] input) {
    NDManager manager = ctx.getNDManager();
    NDArray ndar = manager.create(input);
    ndar = ndar.toType(DataType.FLOAT32, true);
    ndar = ndar.reshape(1,input.length);
    return new NDList(ndar);
  }

  @Override
  public PeakPickingOutput processOutput(TranslatorContext ctx, NDList output) {
    float[] predictedLabels = output.get(0).toFloatArray();
    //The original output is of shape (numFeatures,2)
    NDArray predictedRanges = output.get(1);
    float[] leftBounds = predictedRanges.get(":,0").toFloatArray();
    float[] rightBounds =  predictedRanges.get(":,1").toFloatArray();

    float[] predictedPeaks = output.get(2).toFloatArray();
    PeakPickingOutput outputRecord = new PeakPickingOutput(predictedLabels, predictedPeaks,
        leftBounds, rightBounds);
    return outputRecord;
  }

  @Override
  public Batchifier getBatchifier() {
    return Batchifier.STACK;
  }

}
