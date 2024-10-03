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

package io.github.mzmine.util.scans.PeakPickingModel;

import ai.djl.Model;
import ai.djl.inference.Predictor;
import ai.djl.ndarray.NDManager;
import ai.djl.translate.TranslateException;
import java.io.InputStream;
import java.util.List;

public class PeakPickingModel {
    //NEED TO TRACE MODEL AGAIN WITH (1,128) INPUT SHAPE (this is what _test.py is)
    private static final String modelPath = "/MLModels/currentSave_traced.pth";
    private final Model model;
    private final NDManager manager;
    public final Predictor<double[], PeakPickingOutput> predictor;

    public PeakPickingModel(){
        this.model = Model.newInstance("PeakPicking");
        try{
            final InputStream resourceAsStream = this.getClass().getResourceAsStream(modelPath);
            model.load(resourceAsStream);
        } catch (Exception e) {
            System.out.println("Encountered exception when loading the model.");
            e.printStackTrace();
        }
        this.manager = NDManager.newBaseManager();
        PeakPickingTranslator translator = new PeakPickingTranslator();
        this.predictor = model.newPredictor(translator);
    }

    public void closeModel(){
        this.manager.close();
        this.predictor.close();
        this.model.close();
    }

    public PeakPickingOutput singlePrediction(double[] inputArray){
        try{
            return this.predictor.predict(inputArray);
        } catch(TranslateException e){
            System.out.println("Encountered exception when trying to predict a single input.");
            e.printStackTrace();
            return null;
        }
    }

    public List<PeakPickingOutput> batchPrediction(List<double[]> inputBatch){
       try{
        return this.predictor.batchPredict(inputBatch);
       } catch (TranslateException e){
        System.out.println("Encountered exception when trying to predict a batch of inputs.");
        e.printStackTrace();
        return null;
       }
    }


}
