package io.github.mzmine.datamodel.features.correlation;

import io.github.mzmine.datamodel.features.FeatureListRow;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Image similarity computed in MZmine.
 */
public class R2RImageSimilarityList extends AbstractRowsRelationship {

  private final Type type;
  private final DoubleList similarities = new DoubleArrayList();

  /**
   * @param a    row a
   * @param b    row b
   * @param type the similarity type
   */
  public R2RImageSimilarityList(FeatureListRow a, FeatureListRow b, Type type) {
    super(a, b);
    this.type = type;
  }

  public synchronized void addSimilarity(double sim) {
    similarities.add(sim);
  }

  public int size() {
    return similarities.size();
  }

  public List<Double> getSimilarities() {
    return similarities;
  }

  public double getMaxSimilarity() {
    return similarities.doubleStream().max().orElse(0.0);
  }

  public double getMinSimilarity() {
    return similarities.doubleStream().min().orElse(0.0);
  }

  @Override
  public double getScore() {
    return getMaxSimilarity();
  }

  @Override
  public @NotNull Type getType() {
    return type;
  }

  @Override
  public @NotNull String getAnnotation() {
    return "sim=" + getScoreFormatted();
  }
}
