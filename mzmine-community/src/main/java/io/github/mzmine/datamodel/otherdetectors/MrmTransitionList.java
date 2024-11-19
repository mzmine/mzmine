package io.github.mzmine.datamodel.otherdetectors;

import io.github.mzmine.datamodel.featuredata.FeatureDataUtils;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.types.FeatureDataType;
import io.github.mzmine.datamodel.features.types.otherdectectors.MrmTransitionListType;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Groups several {@link MrmTransition}s in the
 * {@link io.github.mzmine.datamodel.features.ModularDataModel} for {@link ModularFeature}s.
 */
public final class MrmTransitionList {

  private final double q1mass;

  /**
   * Immutable list of the transitions. cannot be modified. Sorted by ascending q3 mass.
   */
  @NotNull
  private final List<@NotNull MrmTransition> transitions;

  /**
   * The quantifier transition. Not necessarily the
   * {@link io.github.mzmine.datamodel.featuredata.IonTimeSeries} on the
   * {@link ModularFeature#getFeatureData()} of this feature after project loading.
   */
  @NotNull
  private MrmTransition quantifier;

  /**
   * @param transitions The transitions of this transition list. The
   *                    {@link MrmTransitionList#quantifier()} is set by determining the most
   *                    intense {@link MrmTransition#chromatogram()}. All transitions must have the
   *                    same q1 mass.
   */
  public MrmTransitionList(@NotNull final List<@NotNull MrmTransition> transitions) {
    final Optional<@NotNull MrmTransition> max = transitions.stream()
        .max(Comparator.comparingDouble(mrm -> FeatureDataUtils.getHeight(mrm.chromatogram())));
    final double q1Mass = transitions.getFirst().q1mass();

    this(q1Mass, transitions, max.get());
  }

  /**
   * @param q1mass      The q1 mass of all transitions in this list.
   * @param transitions All transitions must have the same q1 mass.
   * @param quantifier  The quantifier of this transition list.
   */
  public MrmTransitionList(double q1mass, @NotNull List<MrmTransition> transitions,
      @NotNull MrmTransition quantifier) {
    if (transitions.stream().anyMatch(t -> Double.compare(t.q1mass(), q1mass) != 0)) {
      throw new IllegalArgumentException("Transition list contains different Q1 masses");
    }

    this.q1mass = q1mass;
    this.transitions = transitions.stream().filter(Objects::nonNull)
        .sorted(Comparator.comparingDouble(MrmTransition::q3mass)).toList();
    this.quantifier = quantifier;
  }

  public double q1mass() {
    return q1mass;
  }

  /**
   * An immutable copy of this transitions, sorted by ascending q3 mass.
   */
  @NotNull
  public List<@NotNull MrmTransition> transitions() {
    return List.copyOf(transitions);
  }

  @NotNull
  public MrmTransition transition(int index) {
    return transitions.get(index);
  }

  @NotNull
  public MrmTransition quantifier() {
    return quantifier;
  }

  /**
   * Sets the quantifier of this transition list after checking that the transition is in this list.
   * If {@param feature} is not null, it is checked, that the feature contains this transition list
   * and then updates the feature data. Throws an exception otherwise.
   *
   * @param newQuantifier The new quantifier. Must be in this list.
   * @param feature       The feature to which this transition belongs. If not null, the feature is
   *                      updated.
   */
  public void setQuantifier(@NotNull MrmTransition newQuantifier,
      @Nullable final ModularFeature feature) {
    if (!transitions.contains(newQuantifier)) {
      throw new IllegalArgumentException(
          "The given quantifier %s is not in this transition list (%s).".formatted(newQuantifier,
              transitions.stream().map(MrmTransition::toString).collect(Collectors.joining(", "))));
    }

    if (feature != null && feature.get(MrmTransitionListType.class) != this) {
      throw new IllegalArgumentException("The given feature goes not contain this mrm transition.");
    }

    quantifier = newQuantifier;

    if (feature != null) {
      feature.set(FeatureDataType.class, quantifier.chromatogram());
      // important note: the m/zs in all transitions are the mzs of the precursor mass
      FeatureDataUtils.recalculateIonSeriesDependingTypes(feature);
    }
  }

  /**
   * @return The q3 masses of all transitions. Sorted by ascending mass.
   */
  public double[] q3Masses() {
    return transitions.stream().mapToDouble(MrmTransition::q3mass).toArray();
  }
}
