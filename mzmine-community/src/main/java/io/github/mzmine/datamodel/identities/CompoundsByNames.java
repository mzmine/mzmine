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
      case "e" -> IonParts.M_MINUS;
      case "hfa", "formic acid", "formicacid", "CH2O2", "HCOOH", "CHOOH" -> IonParts.FORMIC_ACID;
      case "fa", "formate", "formiate", "CHO2", "HCOO", "CHOO", "CHO2-", "HCOO-", "CHOO-" ->
          IonParts.FORMATE_FA;
      case "acetate", "ac", "C2H3O2", "CH3COO", "CH3COO-" -> IonParts.ACETATE_AC;
      case "hac", "acetic acid", "aceticacid", "C2H4O2", "CH3COOH" -> IonParts.ACETIC_ACID;
      case "water", "h2o" -> IonParts.H2O;
      case "acn", "acetonitril", "acetonitrile", "CH3CN", "C2H3N" -> IonParts.ACN;
      case "ethanol", "etoh", "CH3CH2OH", "C2H6O" -> IonParts.ETHANOL;
      case "methanol", "meoh", "CH3OH", "CH4O" -> IonParts.METHANOL;
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
