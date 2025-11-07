package io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.wavelet;

public enum EdgeDetectors {
  LOCAL_MIN, ABS_MIN, SLOPE;

  public EdgeDetector create(int tol) {
    return switch (this) {
      case LOCAL_MIN -> new LocalMinimumEdgeDetector(tol);
      case ABS_MIN -> new AbsoluteMinimumEdgeDetector(tol);
      case SLOPE -> new SlopeEdgeDetector(tol + 2);
    };
  }
}
