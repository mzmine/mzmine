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
import io.github.mzmine.datamodel.identities.iontype.IonPart.IonPartStringFlavor;
import java.util.Comparator;
import org.jetbrains.annotations.NotNull;

/**
 * A lightweight reference to an {@link IonPart} that only carries the name, the charge of a single
 * item and the direction of the count. Matching against a real {@link IonPart} is done on these
 * three fields alone, so the mass, the formula and the count magnitude are deliberately ignored.
 * <p>
 * Its use is the representation of the user definable {@link IonTypeRanking} in the
 * {@link FeatureListPreferences}, so reproducible sorting of {@link IonType}s. That also means a
 * reference is independent of the referenced {@link IonPart} being defined in the global ion
 * library. And mathing is done by name, charge, and sing alone.
 * <p>
 * decision: name plus charge plus count direction is the smallest key that still separates ion
 * parts users care about, e.g. Fe+2 from Fe+3 and +H from -H. Ignoring mass and formula makes a
 * reference stable even when the global ion part definition changes its formula spelling or its
 * delta mass. Ignoring the count magnitude means one reference covers +H and +2H alike, so the
 * ranking stays short. The charge state is not ranked at all, prefilters reject an ion type whose
 * charge does not match the row.
 *
 * @param name         the {@link IonPart#name()}, blank only for {@link IonParts#SILENT_CHARGE}
 * @param singleCharge the {@link IonPart#singleCharge()}, so the charge of a single item. Both +H
 *                     and +2H are single charge +1.
 * @param countSign    the direction of {@link IonPart#count()}, -1 for a loss and +1 for an
 *                     addition. Normalized in the constructor, so any count may be passed in. A
 *                     count of 0, as used by {@link IonPartDefinition}, counts as an addition to
 *                     match {@link IonPart#isAddition()}.
 */
public record IonPartReference(@NotNull String name, int singleCharge, int countSign) {

  /**
   * Sorts by descending absolute charge so charged adducts come before neutral modifications, then
   * alphabetically by name. Additions come before losses of the same part.
   */
  public static final Comparator<IonPartReference> SORTER = Comparator.comparingInt(
          (IonPartReference ref) -> Math.abs(ref.singleCharge())).reversed()
      .thenComparing(IonPartReference::name).thenComparingInt(IonPartReference::singleCharge)
      .thenComparing(Comparator.comparingInt(IonPartReference::countSign).reversed());

  public IonPartReference(@NotNull final String name, final int singleCharge, final int countSign) {
    this.name = name.trim();
    this.singleCharge = singleCharge;
    // only the direction is part of the key, see the record javadoc
    this.countSign = countSign < 0 ? -1 : 1;
  }

  public static @NotNull IonPartReference of(@NotNull final IonPart part) {
    return new IonPartReference(part.name(), part.singleCharge(), part.count());
  }

  /**
   * @return the same reference in the other direction, e.g. +H2O for -H2O
   */
  public @NotNull IonPartReference withCountSign(final int countSign) {
    return new IonPartReference(name, singleCharge, countSign);
  }

  /**
   * @return true if the part has the same name, the same single charge and the same count
   * direction, disregarding mass, formula and count magnitude
   */
  public boolean matches(@NotNull final IonPart part) {
    return singleCharge == part.singleCharge() && countSign == (part.count() < 0 ? -1 : 1)
        && name.equals(part.name());
  }

  public boolean isNeutralModification() {
    return singleCharge == 0;
  }

  public boolean isLoss() {
    return countSign < 0;
  }

  /**
   * @return true for {@link IonParts#SILENT_CHARGE}, the only part with a blank name
   */
  public boolean isSilentCharge() {
    return name.isBlank();
  }

  /**
   * The polarity this part contributes to an ion type, so the sign of single charge times count
   * direction. Both +H and the electron loss of [M]+ contribute +1, both -H and a chloride addition
   * contribute -1.
   *
   * @return -1, 0 for a neutral modification, or +1
   */
  public int chargePolaritySign() {
    return Integer.signum(singleCharge * countSign);
  }

  /**
   * @return the count direction, the name and the charge, e.g. +H+, -H+, +Fe+2, +Cl-, or -H2O for a
   * neutral loss. Renders like {@link IonPart#toString(IonPartStringFlavor)} with
   * {@link IonPart.IonPartStringFlavor#SIMPLE_WITH_CHARGE}, so the direction is always explicit.
   */
  @Override
  public @NotNull String toString() {
    // blank name is reserved for the silent charge, see IonParts#SILENT_CHARGE
    if (name.isBlank()) {
      return isLoss() ? "(undefined negative charge carrier)"
          : "(undefined positive charge carrier)";
    }
    return IonUtils.getSignedNumberOmit1(countSign) + name + IonUtils.getSignedNumberOmit1(
        singleCharge);
  }
}
