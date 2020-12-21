package io.github.mzmine.util.deconvolution;

import java.util.Set;

public interface IIndexBasedXYResolver<X, Y> {

  public Set<Set<Integer>> resolveToIndices(X x, Y y, int[] indices);
}
