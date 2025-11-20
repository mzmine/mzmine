/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.datamodel.identities;

import io.github.mzmine.util.MathUtils;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class IonTypeUtils {

  /**
   * [yM+X]+ and [yM+X-H]+ are only different by -H. if any IonPart equals. M+H and 2M+H can
   * identify each other as neutral mass 200 will result in mz 201 and 401 which are unique pairs.
   * Same is true for multi-charge.
   *
   * @return only true if molecules count is different or sub IonPart equals between a and b
   * (ignoring parts in both with different count sign like +-H). Special restriction for multiples,
   * so false for M+H and 2M+2H and false for M-H2O+H and 2M+2H (only diff is H2O), while M+H and
   * 2M+H is true.
   */
  public static boolean restrictPartsOverlapToMultimers(IonType a, IonType b) {
    final List<IonPart[]> overlap = a.streamPartsOverlap(b, true).toList();

    if (overlap.isEmpty()) {
      return true;
    }
    // 1M+H and 2M+2H are restricted (duplicates)
    // M+H and 2M+H are fine
    if (a.molecules() != b.molecules()) {
      // check multiples
      for (IonPart[] pair : overlap) {
        final int countA = pair[0].count();
        final int countB = pair[1].count();
        if (countB * a.molecules() == countA * b.molecules()) {
          // return if any part does match the multiplier - then this is enough to cross out parts
          return false;
        }
      }
      // nothing is multiplied
      return true;
    }

    return false;
  }


  /**
   * @return true if ion pair matches MOL != MOL or MOL==1
   */
  public static boolean checkMolCount(IonType a, IonType b) {
    return checkMolCount(a.molecules(), b.molecules());
  }

  /**
   * 2M and 4M is the same as M to 2M so there cannot be a GCD>1. 2M and 5M for example are ok.
   *
   * @return true if any MOL==1, or A != B with the greatest common divisor (GCD) == 1.
   */
  public static boolean checkMolCount(int molA, int molB) {
    return molA == 1 || molB == 1 || (molA != molB
        && MathUtils.greatestCommonDivisor(molA, molB) == 1);
  }

  /**
   *
   * @return True if a charge state was not detected or if it fits to the adduct
   */
  public static boolean checkChargeState(IonType adduct, Integer charge) {
    return (charge == null || adduct.totalCharge() == charge);
  }

  /**
   * Only one ion can have modifications
   *
   * @return true if only one ion has neutral modifications
   */
  public static boolean checkMaxMod(IonType adduct, IonType adduct2) {
    return !(hasNeutralModification(adduct) && hasNeutralModification(adduct2));
  }

  /**
   * @return true if any part is a neutral modification
   */
  public static boolean hasNeutralModification(IonType ion) {
    for (IonPart part : ion.parts()) {
      if (part.isNeutralModification()) {
        return true;
      }
    }
    return false;
  }

  /**
   * @return all distinct ion parts in all ion types
   */
  public static @NotNull List<IonPart> extractUniqueParts(@NotNull List<IonType> ions) {
    return ions.stream().<IonPart>mapMulti((ion, consumer) -> ion.parts().forEach(consumer))
        .distinct().toList();
  }
}
