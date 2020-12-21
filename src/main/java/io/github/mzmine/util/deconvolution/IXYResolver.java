package io.github.mzmine.util.deconvolution;

import java.util.Set;

/**
 *
 */
public interface IXYResolver<R, X, Y> {

  /**
   * See implementing classes for more detailed information on possible restrictions on x and y data
   * such as ordering.
   *
   * @param x domain values of the data to be deconvoluted.
   * @param y range values of the data to be deconvoluted.
   * @return Collection of a Set of indices for each resolved peak.
   */
  public Set<R> resolveToYData(X x, Y y);
}
