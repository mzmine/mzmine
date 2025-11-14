/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.wavelet;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.AnyXYProvider;
import io.github.mzmine.javafx.util.color.Vision;
import io.github.mzmine.util.color.SimpleColorPalette;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;

public class Test extends Application {

  private static final SimpleColorPalette palette = SimpleColorPalette.DEFAULT.get(
      Vision.NORMAL_VISION);

  public static void main(String[] args) {
    launch(args);
  }

  SimpleXYChart getChart() {

    final WaveletResolver kernel5 = new WaveletResolver(
        new double[]{1d, 1.5d, 2d, 3d, 5d, 8d, 10d}, 5, null, 1E3, 0.1, 5,
        AdvancedWaveletParameters.DEFAULT_NOISE_WINDOW,
        AdvancedWaveletParameters.DEFAULT_MIN_FITTING_SCALES, true, FeatureList.createDummy(),
        new WaveletResolverParameters());

    final WaveletResolver kernel3 = new WaveletResolver(
        new double[]{1d, 1.5d, 2d, 3d, 5d, 8d, 10d}, 5, null, 1E3, 0.1, 3,
        AdvancedWaveletParameters.DEFAULT_NOISE_WINDOW,
        AdvancedWaveletParameters.DEFAULT_MIN_FITTING_SCALES, true, FeatureList.createDummy(),
        new WaveletResolverParameters());

    final WaveletResolver kernel2 = new WaveletResolver(
        new double[]{1d, 1.5d, 2d, 3d, 5d, 8d, 10d}, 5, null, 1E3, 0.1, 2,
        AdvancedWaveletParameters.DEFAULT_NOISE_WINDOW,
        AdvancedWaveletParameters.DEFAULT_MIN_FITTING_SCALES, true, FeatureList.createDummy(),
        new WaveletResolverParameters());

    final SimpleXYChart<AnyXYProvider> chart = new SimpleXYChart<>("Wavelets");
    chart.setStickyZeroRangeAxis(false);

//    chart.addDataset(generateProvider(kernel5.generateMexicanHat(1024, 1), "kernel 5 scale 1"));
//    chart.addDataset(generateProvider(kernel5.generateMexicanHat(1024, 3), "kernel 5 scale 3"));
//    chart.addDataset(generateProvider(kernel5.generateMexicanHat(256, 10), "kernel 5 scale 10"));
    chart.addDataset(generateProvider(kernel5.generateMexicanHat(256, 24), "kernel 5 scale 24"));
    chart.addDataset(generateProvider(kernel3.generateMexicanHat(256, 24), "kernel 3 scale 24"));
//    chart.addDataset(generateProvider(kernel3.generateMexicanHat(256, 10), "kernel 3 scale 10"));
//    chart.addDataset(generateProvider(kernel2.generateMexicanHat(256, 10), "kernel 2 scale 10"));
    chart.addDataset(generateProvider(kernel2.generateMexicanHat(256, 24), "kernel 2 scale 24"));
//    chart.addDataset(generateProvider(kernel1.generateMexicanHat(1024, 3), "kernel 1 scale 3"));
    return chart;
  }

  private static @NotNull AnyXYProvider generateProvider(double[] y, String name) {
    return new AnyXYProvider(palette.getNextColorAWT(), name, y.length, i -> (double) i, i -> y[i]);
  }

  @Override
  public void start(Stage primaryStage) {
    primaryStage.setTitle("Wavelets");
    BorderPane root = new BorderPane();
    SimpleXYChart chart = getChart();
    root.setCenter(chart);
    primaryStage.setScene(new Scene(root, 500, 600));
    primaryStage.show();
  }
}
