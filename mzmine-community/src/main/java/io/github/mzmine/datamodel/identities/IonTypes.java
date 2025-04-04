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

import java.util.List;
import java.util.stream.Stream;

public enum IonTypes {
  // positive,
  M_PLUS(IonParts.M_PLUS), //
  M_2PLUS(IonParts.M_PLUS, IonParts.M_PLUS), //
  M_PLUS_H2O(IonParts.M_PLUS, IonParts.H2O), //
  H(IonParts.H), //
  H2(IonParts.H2_PLUS), //
  H3(IonParts.H3_PLUS), //
  NA(IonParts.NA), //
  NH4(IonParts.NH4), //
  NA2_MINUS_H(IonParts.H_MINUS, IonParts.NA2_PLUS), //
  K(IonParts.K), //
  CA(IonParts.CA), //
  CA_H_MINUS(IonParts.CA, IonParts.H_MINUS), //
  MG(IonParts.MG), //
  MG_H_MINUS(IonParts.MG, IonParts.H_MINUS), //
  FEII(IonParts.FEII), //
  FEII_MINUS_H(IonParts.FEII, IonParts.H_MINUS), //
  FEIII(IonParts.FEIII), //
  FEIII_H_MINUS(IonParts.FEIII, IonParts.H_MINUS), //
  FEIII_2H_MINUS(IonParts.FEIII, IonParts.H_MINUS, IonParts.H_MINUS), //
  // negative
  M_MINUS(IonParts.M_MINUS), //
  M_2MINUS(IonParts.M_MINUS, IonParts.M_MINUS), //
  H_MINUS(IonParts.H_MINUS), //
  F(IonParts.F), //
  CL(IonParts.CL), //
  BR(IonParts.BR), //
  I(IonParts.I), //
  FORMATE_FA(IonParts.FORMATE_FA), //
  ACETATE_AC(IonParts.ACETATE_AC), //
  // in source fragments
  H_H2O(IonParts.H2O, IonParts.H), //
  H_2H2O(IonParts.H2O_2, IonParts.H), //
  H_3H2O(IonParts.H2O_3, IonParts.H), //
  H_4H2O(IonParts.H2O_4, IonParts.H), //
  // multi charge
  H2_PLUS(IonParts.H2_PLUS), //
  H3_PLUS(IonParts.H3_PLUS), //
  NA_H(IonParts.NA, IonParts.H), //
  NH4_H(IonParts.NH4, IonParts.H), //
  K_H(IonParts.K, IonParts.H), //

  // 2M only the major adducts
  M2_H(H.ion.withMolecules(2)), //
  M2_H_H2O(H_H2O.ion.withMolecules(2)), //
  M2_NA(NA.ion.withMolecules(2)), //
  M2_NH4(NH4.ion.withMolecules(2)), //
  M2_H_MINUS(H_MINUS.ion.withMolecules(2)), //
  M2_CL(CL.ion.withMolecules(2)), //

  M2_2H_PLUS(H2_PLUS.ion.withMolecules(2)), //
  M2_NA2_MINUS_H(NA2_MINUS_H.ion.withMolecules(2)), //

  // 3M
  M3_H(H.ion.withMolecules(3)), //
  M3_H_H2O(H_H2O.ion.withMolecules(3)), //
  M3_NA(NA.ion.withMolecules(3)), //
  M3_NH4(NH4.ion.withMolecules(3)), //
  M3_H_MINUS(H_MINUS.ion.withMolecules(3)), //
  M3_CL(CL.ion.withMolecules(3)), //
  ;

  private final IonType ion;

  IonTypes(final IonPart... parts) {
    this(IonType.create(parts));
  }

  IonTypes(final int molecules, final IonPart... parts) {
    this(IonType.create(List.of(parts), molecules));
  }

  IonTypes(IonType ion) {
    this.ion = ion;
  }

  public IonType asIonType() {
    return ion;
  }

  // default values
  public static final List<IonType> DEFAULT_VALUES_POSITIVE = Stream.of(M_PLUS, M_PLUS_H2O, H,
          H_H2O, H_2H2O, H_3H2O, H_4H2O, NA, K, NH4, M_2PLUS, H2_PLUS, CA, CA_H_MINUS, MG, MG_H_MINUS,
          FEII, FEIII, FEII_MINUS_H, FEIII_H_MINUS, FEIII_2H_MINUS, NA_H, NH4_H, K_H, NA2_MINUS_H)
      .map(IonTypes::asIonType).sorted(IonType.DEFAULT_ION_ADDUCT_SORTER).toList();

  public static final List<IonType> DEFAULT_VALUES_NEGATIVE = Stream.of(M_MINUS, H_MINUS,
          NA2_MINUS_H, CL, BR, FORMATE_FA, ACETATE_AC, NA).map(IonTypes::asIonType)
      .sorted(IonType.DEFAULT_ION_ADDUCT_SORTER).toList();

}
