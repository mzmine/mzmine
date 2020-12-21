package io.github.mzmine.util.deconvolution;

import java.util.Set;

/**
 *
 * @param <R> Return type, e.g. Set<Y data type>
 * @param <X> X-value type
 * @param <Y> Y-value type
 */
public interface IXYResolver<R, X, Y> {

  /**
   * See implementing classes for more detailed information on possible restrictions on x and y data
   * such as ordering.
   *
   * @param x domain values of the data to be resolved.
   * @param y range values of the data to be resolved.
   * @return Collection of a Set of indices for each resolved peak.
   */
  public Set<R> resolveToYData(X x, Y y);
}
