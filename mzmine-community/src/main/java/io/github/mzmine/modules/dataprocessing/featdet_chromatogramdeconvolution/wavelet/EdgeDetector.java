package io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.wavelet;

public interface EdgeDetector {

  int detectLeftMinimum(double[] y, int startIndex);

  int detectRightMinimum(double[] y, int startIndex);

}
