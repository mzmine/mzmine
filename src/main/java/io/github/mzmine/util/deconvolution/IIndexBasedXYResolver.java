package io.github.mzmine.util.deconvolution;

import java.util.Set;

/**
 *
 * @param <X> X-value type
 * @param <Y> Y-value type
 */
public interface IIndexBasedXYResolver<X, Y> {

  /**
   * See implementing classes for more detailed information on possible restrictions on x and y data
   * such as ordering.
   *
   * @param x domain values of the data to be resolved.
   * @param y range values of the data to be resolved.
   */
  public Set<Set<Integer>> resolveToIndices(X x, Y y, int[] indices);
}
