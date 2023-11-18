package io.github.mzmine.datamodel.features.correlation;

import io.github.mzmine.datamodel.features.FeatureListRow;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class R2RImageSimilarityList extends AbstractRowsRelationship {

  private final Type type;
  private final List<Double> similarities = new ArrayList<>();

  public R2RImageSimilarityList(FeatureListRow a, FeatureListRow b, Type type) {
    super(a, b);
    this.type = type;
  }

  public synchronized void addSimilarity(Double sim) {
    similarities.add(sim);
  }

  public int size() {
    return similarities.size();
  }

  public List<Double> getSimilarities() {
    return similarities;
  }

  public double getMaxSimilarity() {
    return similarities.stream().mapToDouble(Double::doubleValue).max().orElse(0);
  }

  public double getMinSimilarity() {
    return similarities.stream().mapToDouble(Double::doubleValue).min().orElse(0);
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
