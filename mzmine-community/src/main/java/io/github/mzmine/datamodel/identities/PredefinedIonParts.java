/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

import io.github.mzmine.datamodel.identities.iontype.CombinedIonModification;
import io.github.mzmine.datamodel.identities.iontype.IonModification;
import io.github.mzmine.datamodel.identities.iontype.IonModificationType;

public enum PredefinedIonParts {
  "e", IonUtils.ELECTRON_MASS, -1);
  public static final IonPart M_MINUS = new IonPart("e", IonUtils.ELECTRON_MASS, -1);
  public static final IonPart M_PLUS = new IonPart("e", -IonUtils.ELECTRON_MASS, +1);
  public static final IonPart M_PLUS = new IonPart("e", -IonUtils.ELECTRON_MASS, +1);

  // use combinations of X adducts (2H++; -H+Na2+) and modifications
//  public static final IonModification M_MINUS = new IonModification(IonModificationType.ADDUCT, "e",
//      +0.00054858, -1);
  // NR4+ is already charged, mass might also be charged already
  public static final io.github.mzmine.datamodel.identities.iontype.IonModification M_MINUS_ALREADY_CHARGED = new IonModification(
      IonModificationType.ADDUCT, "e", 0, -1);
  public static final IonModification H_NEG = new IonModification(IonModificationType.ADDUCT, "H",
      "H", -1.007276, -1);
  public static final IonModification M_PLUS = new IonModification(IonModificationType.ADDUCT, "e",
      -0.00054858, 1);
  // NR4+ is already charged, mass might also be charged already
  public static final IonModification M_PLUS_ALREADY_CHARGED = new IonModification(
      IonModificationType.ADDUCT, "e", 0, 1);
  public static final IonModification H = new IonModification(IonModificationType.ADDUCT, "H", "H",
      1.007276, 1);
  // water loss
  public static final IonModification H2O = new IonModification(IonModificationType.NEUTRAL_LOSS,
      "H2O", "H2O", -18.010565, 0);
  //
  public static final IonModification NA = new IonModification(IonModificationType.ADDUCT, "Na",
      "Na", 22.989218, 1);
  public static final IonModification NH4 = new IonModification(IonModificationType.ADDUCT, "NH4",
      "NH4", 18.033823, 1);
  public static final IonModification K = new IonModification(IonModificationType.ADDUCT, "K", "K",
      38.963158, 1);
  public static final IonModification FE = new IonModification(IonModificationType.ADDUCT, "Fe",
      "Fe", 55.933840, 2);
  public static final IonModification CA = new IonModification(IonModificationType.ADDUCT, "Ca",
      "Ca", 39.961493820, 2);
  public static final IonModification MG = new IonModification(IonModificationType.ADDUCT, "Mg",
      "Mg", 47.96953482, 2);
  // combined
  public static final IonModification M_PLUS_H2O = CombinedIonModification.create(M_PLUS, H2O);
  public static final IonModification H_H2O_1 = CombinedIonModification.create(H, H2O);
  public static final IonModification H_H2O_2 = CombinedIonModification.create(H, H2O, H2O);
  public static final IonModification H_H2O_3 = CombinedIonModification.create(H, H2O, H2O, H2O);
  public static final IonModification H2plus = CombinedIonModification.create(H, H);
  public static final IonModification M2plus = CombinedIonModification.create(M_PLUS, M_PLUS);
  public static final IonModification NA_H = CombinedIonModification.create(NA, H);
  public static final IonModification K_H = CombinedIonModification.create(K, H);
  public static final IonModification NH4_H = CombinedIonModification.create(NH4, H);
  public static final IonModification Hneg_NA2 = CombinedIonModification.create(NA, NA, H_NEG);
  public static final IonModification Hneg_CA = CombinedIonModification.create(CA, H_NEG);
  public static final IonModification Hneg_FE = CombinedIonModification.create(FE, H_NEG);
  public static final IonModification Hneg_MG = CombinedIonModification.create(MG, H_NEG);

  // NEGATIVE
  public static final IonModification CL = new IonModification(IonModificationType.ADDUCT, "Cl",
      "Cl", 34.969401, -1);
  public static final IonModification BR = new IonModification(IonModificationType.ADDUCT, "Br",
      "Br", 78.918886, -1);
  public static final IonModification FA = new IonModification(IonModificationType.ADDUCT, "FA",
      "HCO2", 44.99820285, -1);
  public static final IonModification ACETATE = new IonModification(IonModificationType.ADDUCT,
      "Acetate", "C2H3O2", 59.013304, -1);
  // combined
  // +Na -2H+]-
  public static final IonModification NA_2H = CombinedIonModification.create(NA, H_NEG, H_NEG);

  // modifications
  public static final IonModification H2 = new IonModification(IonModificationType.NEUTRAL_LOSS,
      "H2", "H2", -2.015650, 0);
  public static final IonModification C2H4 = new IonModification(IonModificationType.NEUTRAL_LOSS,
      "C2H4", "C2H4", -28.031301, 0);
  public static final IonModification H2O_2 = CombinedIonModification.create(H2O, H2O);
  public static final IonModification H2O_3 = CombinedIonModification.create(H2O, H2O, H2O);
  public static final IonModification H2O_4 = CombinedIonModification.create(H2O, H2O, H2O, H2O);
  public static final IonModification H2O_5 = CombinedIonModification.create(H2O, H2O, H2O, H2O,
      H2O);

  public static final IonModification NH3 = new IonModification(IonModificationType.NEUTRAL_LOSS,
      "NH3", "NH3", -17.026549, 0);
  public static final IonModification CO = new IonModification(IonModificationType.NEUTRAL_LOSS,
      "CO", "CO", -27.994915, 0);
  public static final IonModification CO2 = new IonModification(IonModificationType.NEUTRAL_LOSS,
      "CO2", "CO2", -43.989829, 0);
  // cluster
  public static final IonModification MEOH = new IonModification(IonModificationType.CLUSTER,
      "MeOH", "CH3OH", 32.026215, 0);
  public static final IonModification HFA = new IonModification(IonModificationType.CLUSTER, "HFA",
      "CHOOH", 46.005479, 0);
  public static final IonModification HAc = new IonModification(IonModificationType.CLUSTER, "HAc",
      "CH3COOH", 60.021129, 0);
  public static final IonModification ACN = new IonModification(IonModificationType.CLUSTER, "ACN",
      "CH3CN", 41.026549, 0);
  public static final IonModification O = new IonModification(IonModificationType.CLUSTER, "O", "O",
      15.99491462, 0);
  public static final IonModification ISOPROP = new IonModification(IonModificationType.CLUSTER,
      "IsoProp", "C3H8O", 60.058064, 0);
  // isotopes
  public static final IonModification C13 = new IonModification(IonModificationType.ISOTOPE,
      "(13C)", 1.003354838, 0);

  // default values
  public static final IonModification[] DEFAULT_VALUES_POSITIVE = {M_PLUS, M_PLUS_H2O, H, H_H2O_1,
      H_H2O_2, H_H2O_3, NA, K, NH4, M2plus, H2plus, CA, FE, MG, NA_H, NH4_H, K_H, Hneg_NA2, Hneg_CA,
      Hneg_FE, Hneg_MG, M_PLUS_ALREADY_CHARGED, H2O, H_NEG};
  public static final IonModification[] DEFAULT_VALUES_NEGATIVE = {M_MINUS, H_NEG, NA_2H, CL, BR,
      FA, ACETATE, M_MINUS_ALREADY_CHARGED, H2O, NA};
  // default modifications
  public static final IonModification[] DEFAULT_VALUES_MODIFICATIONS = {H2O, H2O_2, H2O_3, H2O_4,
      H2O_5, NH3, O, CO, CO2, C2H4, HFA, HAc, MEOH, ACN, ISOPROP};
  // isotopes
  public static final IonModification[] DEFAULT_VALUES_ISOTOPES = {C13};
}
