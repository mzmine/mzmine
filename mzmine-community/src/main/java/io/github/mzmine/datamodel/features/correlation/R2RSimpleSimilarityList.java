/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import org.jetbrains.annotations.NotNull;

/**
 * Simple list of similarities
 */
public class R2RSimpleSimilarityList extends AbstractRowsRelationship {

  private final Type type;

  // for multiple features
  private final DoubleList similarities = new DoubleArrayList();

  /**
   * @param a    row a
   * @param b    row b
   * @param type the similarity type
   */
  public R2RSimpleSimilarityList(FeatureListRow a, FeatureListRow b, Type type) {
    super(a, b);
    this.type = type;
  }

  public synchronized void addSimilarity(double sim) {
    if (Double.isNaN(sim)) {
      similarities.add(0);
    } else {
      similarities.add(sim);
    }
  }

  public int size() {
    return similarities.size();
  }

  public DoubleList getSimilarities() {
    return similarities;
  }

  public double getMaxSimilarity() {
    if (similarities.size() == 1) {
      return similarities.getDouble(0);
    } else if (similarities.isEmpty()) {
      return 0d;
    }
    return similarities.doubleStream().max().orElse(0.0);
  }

  public double getMinSimilarity() {
    if (similarities.size() == 1) {
      return similarities.getDouble(0);
    } else if (similarities.isEmpty()) {
      return 0d;
    }
    return similarities.doubleStream().min().orElse(0.0);
  }

  public double getAverageSimilarity() {
    if (similarities.size() == 1) {
      return similarities.getDouble(0);
    } else if (similarities.isEmpty()) {
      return 0d;
    }
    return similarities.doubleStream().average().orElse(0.0);
  }

  @Override
  public double getScore() {
    return getMaxSimilarity();
  }

  @Override
  public @NotNull String getType() {
    return type.toString();
  }

  @Override
  public @NotNull String getAnnotation() {
    return "sim=" + getScoreFormatted();
  }
}
