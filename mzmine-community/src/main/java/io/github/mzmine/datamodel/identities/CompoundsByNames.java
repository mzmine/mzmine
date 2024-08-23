/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.openscience.cdk.interfaces.IMolecularFormula;

public class CompoundsByNames {

  /**
   * Get ion part for common names
   *
   * @param name compound name
   * @return optional of ion part for compound if known
   */
  public static Optional<IonPart> getIonPartByName(@NotNull String name) {
    String simplifiedName = name.toLowerCase().replaceAll("[_-]", " ").trim();
    return Optional.ofNullable(switch (simplifiedName) {
      case "hfa", "formic acid", "formicacid" -> IonParts.FORMIC_ACID;
      case "fa", "formate", "formiate" -> IonParts.FORMATE_FA;
      case "acetate", "ac" -> IonParts.ACETATE_AC;
      case "hac", "acetic acid", "aceticacid" -> IonParts.ACETIC_ACID;
      case "water", "h2o" -> IonParts.H2O;
      case "acn", "acetonitril", "acetonitrile" -> IonParts.ACN;
      case "ethanol", "etoh" -> IonParts.ETHANOL;
      case "methanol", "meoh" -> IonParts.METHANOL;
      case "isoprop", "iso prop", "isopropanol", "iso propanol", "iproh", "ipr" ->
          IonParts.ISO_PROPANOL;
      case "hydrocholide", "hcl" -> IonParts.HCL;
      default -> null;
    });
  }


  /**
   * Get formulas for common names
   *
   * @param name compound name
   * @return optional of compound formula if known
   */
  public static Optional<IMolecularFormula> getFormulaByName(@NotNull String name) {
    return getIonPartByName(name).map(IonPart::singleFormula);
  }
}
