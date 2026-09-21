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

package io.github.mzmine.datamodel.identities.iontype;

import io.github.mzmine.datamodel.features.preferences.FeatureListPreferences;
import io.github.mzmine.modules.dataprocessing.filter_featurelistpreferences.FeatureListPreferencesParameters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

/**
 * User definable ranking of {@link IonType}s. Each {@link IonPart} gets a frequency factor that
 * says how commonly it is observed and the score of a whole ion type is the mean frequency of its
 * parts, reduced by a penalty for multimers. A higher score means the ion type is the more likely
 * explanation.
 * <p>
 * decision: one single ranking holds the charge carriers of both polarities and the neutral
 * modifications. Positive and negative adducts never collide because {@link IonPartReference}
 * carries the charge and the count direction, so +H and -H are separate entries, and a neutral
 * modification only has to be listed once.
 * <p>
 * The default ranking lives in {@link FeatureListPreferences} and may be replaced by the user, see
 * {@link FeatureListPreferencesParameters}. All ion identity networking modules read it from the
 * feature list.
 */
public final class IonTypeRanking {

  /**
   * Frequency used for any {@link IonPart} without an entry in the ranking. Fixed at 0 so that
   * unknown or exotic parts always rank last.
   */
  public static final float UNRANKED_FREQUENCY = 0f;

  /**
   * Score subtracted per additional molecule in a cluster, so [2M+H]+ loses this once and [3M+H]+
   * twice. decision: kept as a code constant instead of a parameter - the value only has to
   * separate a multimer from an equally common monomer and users tune the ranking itself.
   */
  public static final double MULTIMER_PENALTY = 0.37;

  /**
   * Number of charged adducts listed in {@link #toShortSummaryString()} before it is truncated.
   */
  private static final int MAX_SUMMARY_ENTRIES = 6;

  private final @NotNull List<IonPartFrequency> frequencies;
  // lookup by reference, derived from the list above
  private final @NotNull Map<IonPartReference, Float> lookup;

  public IonTypeRanking(@NotNull final List<IonPartFrequency> frequencies) {
    this.frequencies = sortedCopy(frequencies);
    this.lookup = toMap(this.frequencies);
  }

  private static @NotNull List<IonPartFrequency> sortedCopy(
      @NotNull final List<IonPartFrequency> entries) {
    final List<IonPartFrequency> copy = new ArrayList<>(entries);
    copy.sort(IonPartFrequency.MOST_FREQUENT_FIRST);
    return List.copyOf(copy);
  }

  private static @NotNull Map<IonPartReference, Float> toMap(
      @NotNull final List<IonPartFrequency> entries) {
    final Map<IonPartReference, Float> map = HashMap.newHashMap(entries.size());
    for (final IonPartFrequency entry : entries) {
      // first entry wins, the list is sorted most frequent first
      map.putIfAbsent(entry.part(), entry.frequency());
    }
    return Map.copyOf(map);
  }

  /**
   * The ranking shipped with mzmine, used unless the user redefines it in the feature list
   * preferences.
   */
  public static @NotNull IonTypeRanking createDefault() {
    return new IonTypeRanking(new ArrayList<>(List.of(
        // positive
        IonPartFrequency.of(IonParts.H, 1f), //
        IonPartFrequency.of(IonParts.NH4, 0.7f), //
        IonPartFrequency.of(IonParts.NA, 0.71f), //
        IonPartFrequency.of(IonParts.K, 0.35f), //
        IonPartFrequency.of(IonParts.CA, 0.2f), //
        IonPartFrequency.of(IonParts.MG, 0.15f), //
        IonPartFrequency.of(IonParts.FEII, 0.1f), //
        IonPartFrequency.of(IonParts.FEIII, 0.05f), //
        // negative, halides and organic acids are added to the molecule
        IonPartFrequency.of(IonParts.H_MINUS, 1f), //
        IonPartFrequency.of(IonParts.CL, 0.6f), //
        IonPartFrequency.of(IonParts.FORMATE_FA, 0.65f), //
        IonPartFrequency.of(IonParts.ACETATE_AC, 0.45f), //
        IonPartFrequency.of(IonParts.BR, 0.2f), //
        IonPartFrequency.of(IonParts.I, 0.15f), //
        IonPartFrequency.of(IonParts.F, 0.1f), //
        // the [M]+ and [M]- radicals and an undefined charge carrier are far less common than
        // protonation and deprotonation
        IonPartFrequency.of(IonParts.M_PLUS, 0.61f), //
        IonPartFrequency.of(IonParts.M_MINUS, 0.61f), //
        IonPartFrequency.of(IonParts.SILENT_CHARGE, 0.6f), //
        IonPartFrequency.of(IonParts.SILENT_CHARGE.withCount(-1), 0.6f), //
        // neutral
        IonPartFrequency.of(IonParts.H2O, 0.7f), //
        IonPartFrequency.of(IonParts.NH3, 0.35f), //
        IonPartFrequency.of(IonParts.ACN, 0.2f), //
        IonPartFrequency.of(IonParts.CO, 0.2f), //
        IonPartFrequency.of(IonParts.CO2, 0.25f), //
        IonPartFrequency.of(IonParts.H2, 0.2f), //
        IonPartFrequency.of(IonParts.HCL, 0.25f), //
        IonPartFrequency.of(IonParts.FORMIC_ACID, 0.36f), //
        IonPartFrequency.of(IonParts.ACETIC_ACID, 0.21f), //
        IonPartFrequency.of(IonParts.METHANOL, 0.23f), //
        IonPartFrequency.of(IonParts.ETHANOL, 0.16f), //
        IonPartFrequency.of(IonParts.C2H4, 0.05f), //
        IonPartFrequency.of(IonParts.ISO_PROPANOL, 0.04f) //
    )));
  }

  /**
   * The frequency of a single part. Matching only uses name, charge and count direction, see
   * {@link IonPartReference}.
   *
   * @return the configured frequency or {@link #UNRANKED_FREQUENCY} if the part is not listed
   */
  public float frequency(@NotNull final IonPart part) {
    return lookup.getOrDefault(IonPartReference.of(part), UNRANKED_FREQUENCY);
  }

  /**
   * Scores how likely an ion type is the correct explanation. Higher is better.
   * <p>
   * The score is the mean frequency of all {@link IonType#parts()} minus {@link #MULTIMER_PENALTY}
   * per additional molecule. Using the mean rather than the sum keeps ion types with a different
   * number of parts comparable and lets a rarely observed modification drag a common adduct down.
   * <p>
   * decision: the charge state is not scored at all. Prefilters reject an ion type whose charge
   * does not match the row, so a higher charge that survives them is a real observation and must
   * not be penalized against a monomer.
   */
  public double score(@NotNull final IonType ion) {
    final List<IonPart> parts = ion.parts();

    double sum = 0;
    for (final IonPart part : parts) {
      sum += lookup.getOrDefault(IonPartReference.of(part), UNRANKED_FREQUENCY);
    }
    final double mean = parts.isEmpty() ? UNRANKED_FREQUENCY : sum / parts.size();

    final int extraMolecules = Math.max(0, ion.molecules() - 1);
    return mean - MULTIMER_PENALTY * extraMolecules;
  }

  /**
   * @return all ranking entries, sorted most frequent first
   */
  public @NotNull List<IonPartFrequency> getFrequencies() {
    return frequencies;
  }

  public @NotNull IonTypeRanking withFrequencies(@NotNull final List<IonPartFrequency> entries) {
    return new IonTypeRanking(entries);
  }

  /**
   * A short one line summary of the most frequent charged adducts of both polarities, e.g.
   * {@code +H+ > -H+ > +NH4+ > ... > else}. Neutral modifications and the undefined charge carrier
   * are omitted to keep it readable. The trailing "else" stands for every unlisted part with
   * {@link #UNRANKED_FREQUENCY}.
   */
  public @NotNull String toShortSummaryString() {
    final List<IonPartFrequency> entries = frequencies.stream()
        .filter(e -> !e.part().isSilentCharge()).toList();
    if (entries.isEmpty()) {
      return "else";
    }
    final String listed = entries.stream().limit(MAX_SUMMARY_ENTRIES).map(e -> e.part().toString())
        .collect(Collectors.joining(" > "));
    final String truncated = entries.size() > MAX_SUMMARY_ENTRIES ? " > ..." : "";
    return listed + truncated + " > else";
  }

  /**
   * A short one line summary of the most frequent charged adducts of both polarities, e.g.
   * {@code +H+ > -H+ > +NH4+ > ... > else}. Neutral modifications and the undefined charge carrier
   * are omitted to keep it readable. The trailing "else" stands for every unlisted part with
   * {@link #UNRANKED_FREQUENCY}.
   */
  public @NotNull String toFullSummaryString() {
    final String elseFreq = "else (%s)".formatted(UNRANKED_FREQUENCY);
    final List<IonPartFrequency> entries = frequencies.stream()
        .filter(e -> !e.part().isNeutralModification() && !e.part().isSilentCharge()).toList();
    if (entries.isEmpty()) {
      return elseFreq;
    }
    final String listed = entries.stream().map(IonPartFrequency::toString)
        .collect(Collectors.joining(", "));
    return listed + ", " + elseFreq;
  }

  @Override
  public boolean equals(final Object o) {
    return o instanceof IonTypeRanking other && frequencies.equals(other.frequencies);
  }

  @Override
  public int hashCode() {
    return Objects.hash(frequencies);
  }

  @Override
  public String toString() {
    return "ion ranking [%s]".formatted(toFullSummaryString());
  }
}
