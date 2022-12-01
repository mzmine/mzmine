/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.datamodel.features.correlation;

import io.github.mzmine.datamodel.features.FeatureListRow;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Spectral similarity computed in MZmine.
 */
public class R2RSpectralSimilarityList extends AbstractRowsRelationship {

  private final Type type;
  private final List<SpectralSimilarity> spectralSim = new ArrayList<>();

  /**
   * @param a    row a
   * @param b    row b
   * @param type the similarity type
   */
  public R2RSpectralSimilarityList(FeatureListRow a, FeatureListRow b, Type type) {
    super(a, b);
    this.type = type;
  }

  public synchronized void addSpectralSim(SpectralSimilarity sim) {
    spectralSim.add(sim);
  }

  public int size() {
    return spectralSim.size();
  }

  public List<SpectralSimilarity> getSpectralSim() {
    return spectralSim;
  }

  public int getMaxOverlap() {
    return spectralSim.stream().mapToInt(SpectralSimilarity::overlap).max().orElse(0);
  }

  public int getMinOverlap() {
    return spectralSim.stream().mapToInt(SpectralSimilarity::overlap).min().orElse(0);
  }

  public double getMeanOverlap() {
    return spectralSim.stream().mapToInt(SpectralSimilarity::overlap).average().orElse(0);
  }

  public double getMeanCosineSim() {
    return spectralSim.stream().mapToDouble(SpectralSimilarity::cosine).average().orElse(0);
  }

  public double getMaxCosineSim() {
    return spectralSim.stream().mapToDouble(SpectralSimilarity::cosine).max().orElse(0);
  }

  public double getMinCosineSim() {
    return spectralSim.stream().mapToDouble(SpectralSimilarity::cosine).min().orElse(0);
  }

  @Override
  public double getScore() {
    return getMaxCosineSim();
  }

  @NotNull
  @Override
  public String getAnnotation() {
    return "cos=" + getScoreFormatted();
  }

  @NotNull
  @Override
  public Type getType() {
    return type;
  }
}
