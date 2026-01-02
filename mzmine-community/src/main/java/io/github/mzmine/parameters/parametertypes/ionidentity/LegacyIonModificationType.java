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

package io.github.mzmine.parameters.parametertypes.ionidentity;

import java.util.List;

/**
 * Type of ion modification.
 */
@Deprecated
enum LegacyIonModificationType {
  ADDUCT, UNDEFINED_ADDUCT, NEUTRAL_LOSS, CLUSTER, ISOTOPE, UNKNOWN, MIXED;

  /**
   * The common type or MIXED
   *
   * @param adducts list of ion modifications
   * @return the common type of all modifications - or MIXED
   */
  public static LegacyIonModificationType getType(LegacyIonModification[] adducts) {
    if (adducts == null || adducts.length == 0) {
      return UNKNOWN;
    }
    LegacyIonModificationType t = adducts[0].getType();
    for (int i = 1; i < adducts.length; i++) {
      if (!t.equals(adducts[i].getType())) {
        return LegacyIonModificationType.MIXED;
      }
    }
    return t;
  }

  /**
   * The common type or MIXED
   *
   * @param adducts list of ion modifications
   * @return the common type of all modifications - or MIXED
   */
  public static LegacyIonModificationType getType(List<LegacyIonModification> adducts) {
    if (adducts == null || adducts.isEmpty()) {
      return UNKNOWN;
    }
    LegacyIonModificationType t = adducts.get(0).getType();
    for (int i = 1; i < adducts.size(); i++) {
      if (!t.equals(adducts.get(i).getType())) {
        return LegacyIonModificationType.MIXED;
      }
    }
    return t;
  }

  @Override
  public String toString() {
    return super.toString().replaceAll("_", " ");
  }
}
