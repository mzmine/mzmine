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

import java.util.List;
import org.jetbrains.annotations.NotNull;

public class IonParts {

  public static final IonPart M_MINUS = new IonPart("e", IonUtils.ELECTRON_MASS, -1);
  public static final IonPart M_PLUS = M_MINUS.flipCount();

  public static final IonPart H = new IonPart("H", 1);
  public static final IonPart H2_PLUS = H.withCount(2);
  public static final IonPart H3_PLUS = H.withCount(3);
  public static final IonPart NA = new IonPart("Na", 1);
  public static final IonPart NA2_PLUS = NA.withCount(2);
  public static final IonPart NH4 = new IonPart("NH4", 1);
  public static final IonPart K = new IonPart("K", 1);
  public static final IonPart CA = new IonPart("Ca", 2);
  public static final IonPart MG = new IonPart("Mg", 2);
  public static final IonPart FEII = new IonPart("Fe", 2);
  public static final IonPart FEIII = new IonPart("Fe", 3);
  // negative
  public static final IonPart H_MINUS = H.flipCount();
  public static final IonPart F = new IonPart("F", -1);
  public static final IonPart CL = new IonPart("Cl", -1);
  public static final IonPart BR = new IonPart("Br", -1);
  public static final IonPart I = new IonPart("I", -1);
  public static final IonPart FORMATE_FA = new IonPart("CHO2", -1);
  public static final IonPart ACETATE_AC = new IonPart("C2H3O2", -1);
  // complex
  public static final IonPart METHANOL = new IonPart("CH2OH", 0);
  public static final IonPart ETHANOL = new IonPart("C2H6O", 0);
  public static final IonPart ACN = new IonPart("CH3CN", 0);
  public static final IonPart FORMIC_ACID = new IonPart("CH2O2", 0);
  public static final IonPart ACETIC_ACID = new IonPart("CH3COOH", 0);
  public static final IonPart ISO_PROPANOL = new IonPart("C3H8O", 0);
  // loss;
  public static final IonPart CO = new IonPart("CO", 0, -1);
  public static final IonPart CO2 = new IonPart("CO2", 0, -1);
  public static final IonPart NH3 = new IonPart("NH3", 0, -1);
  public static final IonPart H2 = new IonPart("H2", 0, -1);
  public static final IonPart C2H4 = new IonPart("C2H4", 0, -1);
  public static final IonPart HCL = new IonPart("HCl", 0, -1);
  public static final IonPart H2O = new IonPart("H2O", 0, -1);
  public static final IonPart H2O_2 = H2O.withCount(-2);
  public static final IonPart H2O_3 = H2O.withCount(-3);
  public static final IonPart H2O_4 = H2O.withCount(-4);
  public static final IonPart H2O_5 = H2O.withCount(-5);


  /**
   * @param nameOrFormula structure or common name
   * @return an IonPart by structure of if this fails by common name. Otherwise,
   * {@link IonPart#unknown(String, int)}
   */
  @NotNull
  public static IonPart partByNameOrFormula(String nameOrFormula) {
    // map names or structure
    return CompoundsByNames.getIonPartByName(nameOrFormula).orElse();
  }

  // default modifications
  public static final List<IonPart> DEFAULT_VALUES_MODIFICATIONS = List.of(H2O, H2O_2, H2O_3, H2O_4,
      H2O_5, HCL, NH3, CO, CO2, H2, C2H4, FORMIC_ACID, ACETIC_ACID, ACN, METHANOL, ETHANOL,
      ISO_PROPANOL);
}
