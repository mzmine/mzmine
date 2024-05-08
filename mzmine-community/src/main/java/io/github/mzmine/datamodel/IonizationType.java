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

package io.github.mzmine.datamodel;

import io.github.mzmine.util.FormulaUtils;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public enum IonizationType {

  NO_IONIZATION("No ionization", "", "", PolarityType.NEUTRAL, -6, 0, 0), //

  POSITIVE("[M]+", "", "", PolarityType.POSITIVE, -6, 1, 1), //

  POSITIVE_HYDROGEN("[M+H]+", "H", "", PolarityType.POSITIVE, -0.268998651917666, 1, 1), //

  SODIUM("[M+Na]+", "Na", "", PolarityType.POSITIVE, -0.963288182616102, 1, 1), //

  POTASSIUM("[M+K]+", "K", "", PolarityType.POSITIVE, -2.41599440912713, 1, 1), //

  LITHIUM("[M+Li]+", "Li", "", PolarityType.POSITIVE, -6, 1, 1), //

  AMMONIUM("[M+NH4]+", "NH4", "", PolarityType.POSITIVE, -2.49171512306525, 1, 1), //

//  NAME1("[M+2H-NH3]2+", "H2", "NH3", -15.0120166, PolarityType.POSITIVE, -3.51290442213519, 1, 2),//

  NAME2("[M]3+", "", "", PolarityType.POSITIVE, -3.51290442213519, 1, 3),//

  NAME3("[M]2+", "", "", PolarityType.POSITIVE, -3.51290442213519, 1, 2),//

  NAME4("[M+H]2+", "H", "", PolarityType.POSITIVE, -3.33681316307951, 1, 2),//

  NAME5("[M+2H]2+", "H2", "", PolarityType.POSITIVE, -1.81393441779917, 1, 2),//

  NAME6("[M+H+Na]2+", "NaH", "", PolarityType.POSITIVE, -2.69999106549233, 1, 2),//

  NAME7("[M+2H+Na]3+", "NaH2", "", PolarityType.POSITIVE, -3.81393441779917, 1, 3),//

  NAME8("[M+H+K]2+", "KH", "", PolarityType.POSITIVE, -2.23415082118236, 1, 2),//

  NAME9("[M+2Na]2+", "Na2", "", PolarityType.POSITIVE, -2.66780638212093, 1, 2),//

  NAME10("[M+H+2Na]3+", "Na2H", "", PolarityType.POSITIVE, -3.51290442213519, 1, 3),//

  NAME11("[M+3Na]3+", "Na3", "", PolarityType.POSITIVE, -3.51290442213519, 1, 3),//

  NAME13("[M+H-H2O]+", "H", "H2O", PolarityType.POSITIVE, -0.747608492437131, 1, 1),//

  NAME15("[M+H-NH3]+", "H", "NH3", PolarityType.POSITIVE, -1.58862513607331, 1, 1),//

  NAME16("[M-H+2Na]+", "Na2", "H", PolarityType.POSITIVE, -1.85969190835984, 1, 1),//

  NAME18("[M-2H+3Na]+", "Na3", "H2", PolarityType.POSITIVE, -1.91084443080722, 1, 1),//

  NAME19("[M+H+H2O]+", "H2OH", "", PolarityType.POSITIVE, -2.3225727239649, 1, 1),//

  NAME22("[M-H+2K]+", "K2", "H", PolarityType.POSITIVE, -3.11496441346315, 1, 1),//

  NAME23("[M+H2O]+", "H2O", "", PolarityType.POSITIVE, -3.21187442647121, 1, 1),//

  NAME24("[M+H-OH]+", "H", "OH", PolarityType.POSITIVE, -3.21187442647121, 1, 1),//

  NAME25("[M-H2O]+", "", "H2O", PolarityType.POSITIVE, -3.51290442213519, 1, 1),//

  NAME26("[M-H]+", "", "H", PolarityType.POSITIVE, -3.51290442213519, 1, 1),//

  NAME27("[M+Na-H2O]+", "Na", "H2O", PolarityType.POSITIVE, -3.51290442213519, 1, 1),//

  NAME28("[M-2H+3K]+", "K3", "H2", PolarityType.POSITIVE, -3.51290442213519, 1, 1),//

  NAME29("[M+K-H2O]+", "K", "H2O", PolarityType.POSITIVE, -3.81393441779917, 1, 1),//

  NAME30("[M-CO2H+H]+", "H", "CO2H", PolarityType.POSITIVE, -4.81393441779917, 1, 1),//

  NAME32("[2M+H]+", "H", "", PolarityType.POSITIVE, -1.22398481647346, 2, 1),//

  NAME37("[2M+Na]+", "Na", "", PolarityType.POSITIVE, -2.96883637778491, 2, 1),//

  NAME34("[2M+Na-H2O]+", "Na", "H2O", PolarityType.POSITIVE, -3.81393441779917, 2, 1),//

  NAME35("[2M+K-H2O]+", "K", "H2O", PolarityType.POSITIVE, -3.81393441779917, 2, 1),//

  NAME38("[2M+K]+", "K", "", PolarityType.POSITIVE, -3.81393441779917, 2, 1),//

  NAME33("[3M+H]+", "H", "", PolarityType.POSITIVE, -2.26986637344889, 3, 1),//

  NAME39("[3M+K]+", "K", "", PolarityType.POSITIVE, -3.81393441779917, 3, 1),//

  NAME36("[3M+K-H2O]+", "K", "H2O", PolarityType.POSITIVE, -4.81393441779917, 3, 1),//

  NAME31("[3M+H-H2O]+", "H", "H2O", PolarityType.POSITIVE, -4.81393441779917, 3, 1),//

  NEGATIVE("[M]-", "", "", PolarityType.NEGATIVE, -6, 1, -1), //

  NEGATIVE_HYDROGEN("[M-H]-", "", "H", PolarityType.NEGATIVE, -0.172110574929576, 1, -1), //

  NAME_2("[M+Na-3H]2-", "Na", "H3", PolarityType.NEGATIVE, -4.24802233641235, 1, -2),//

  CARBONATE("[M+CO3]-", "CO3", "", PolarityType.NEGATIVE, -6, 1, -1), //

  FORMATE("[M+HCOO]-", "HCOO", "", PolarityType.NEGATIVE, -1, 1, -1), //

  PHOSPHATE("[M+H2PO4]-", "H2PO4", "", PolarityType.NEGATIVE, -6, 1, -1), //

  ACETATE("[M+CH3COO]-", "CH3COO", "", PolarityType.NEGATIVE, -6, 1, -1), //

  TRIFLUORACETATE("[M+CF3COO]-", "CF3COO", "", PolarityType.NEGATIVE, -6, 1, -1), //

  CHLORIDE("[M+Cl]-", "Cl", "", PolarityType.NEGATIVE, -6, 1, -1), //

  BROMIDE("[M+Br]-", "Br", "", PolarityType.NEGATIVE, -6, 1, -1), //

  NAME_1("[M-2H]2-", "", "H2", PolarityType.NEGATIVE, -1.4029242963981, 1, -2),//

  NAME_4("[M-H-H2O]-", "", "H3O", PolarityType.NEGATIVE, -0.819887542383565, 1, -1),//

  NAME_5("[M-H-NH3]-", "", "NH4", PolarityType.NEGATIVE, -1.94699234074837, 1, -1),//

  NAME_6("[M-H+H2O]-", "H2O", "H", PolarityType.NEGATIVE, -2.13407898410552, 1, -1),//

  NAME_7("[M-2H]-", "", "H2", PolarityType.NEGATIVE, -2.77090108169269, 1, -1),//

  NAME_8("[M+K-2H]-", "K", "H2", PolarityType.NEGATIVE, -3.24802233641235, 1, -1),//

  NAME_9("[M+Na-2H]-", "Na", "H2", PolarityType.NEGATIVE, -4.24802233641235, 1, -1),//

  NAME_10("[2M-H]-", "", "H", PolarityType.NEGATIVE, -0.978509392194437, 2, -1),//

  NAME_11("[3M-H]-", "", "H", PolarityType.NEGATIVE, -1.99274983130905, 3, -1);

  // log10freq records log base 10 observed frequency of adducts and fragments from available LC-MS1
  // spectra for pure compounds available in the NIST database introduced in CliqueMS algorithm. The
  // compounds whose frequency is not yet observed is given a minimum log frequency value of -6.0 .

  private final String name;
  private final IMolecularFormula addedFormula;
  private final IMolecularFormula removedFormula;
  private final PolarityType polarity;
  private final double addedMass;
  private final double log10freq;
  private final int numMol;
  private final int charge;

  IonizationType(String name, @NotNull String addedFormula, @NotNull String removedFormula,
      PolarityType polarity, double log10freq, int numMol, int charge) {
    this.name = name;
    this.log10freq = log10freq;
    this.polarity = polarity;
    this.numMol = numMol;
    this.charge = charge;
    this.addedFormula =
        !addedFormula.isBlank() ? MolecularFormulaManipulator.getMolecularFormula(addedFormula,
            SilentChemObjectBuilder.getInstance()) : null;
    this.removedFormula =
        !removedFormula.isBlank() ? MolecularFormulaManipulator.getMolecularFormula(removedFormula,
            SilentChemObjectBuilder.getInstance()) : null;

    var added = this.addedFormula != null ? MolecularFormulaManipulator.getMass(this.addedFormula,
        MolecularFormulaManipulator.MonoIsotopic) : 0d;
    var removed =
        this.removedFormula != null ? MolecularFormulaManipulator.getMass(this.removedFormula,
            MolecularFormulaManipulator.MonoIsotopic) : 0d;

    this.addedMass = (added - removed - charge * FormulaUtils.electronMass);
  }

  public String getAdductName() {
    return name;
  }

  public double getAddedMass() {
    return addedMass;
  }

  public PolarityType getPolarity() {
    return polarity;
  }

  public double getLog10freq() {
    return log10freq;
  }

  public int getNumMol() {
    return numMol;
  }

  public int getCharge() {
    return charge;
  }

  @Override
  public String toString() {
    return name;
  }

  /**
   * Ionizes the given formula. If a charged formula is given, the charges will be added together.
   * See {@link FormulaUtils#neutralizeFormulaWithHydrogen(IMolecularFormula)} for formula
   * neutralisation.
   *
   * @param formula The input formula.
   * @return The ionized formula.
   */
  public IMolecularFormula ionizeFormula(@NotNull String formula) {
    final IMolecularFormula form = MolecularFormulaManipulator.getMolecularFormula(formula,
        SilentChemObjectBuilder.getInstance());
    ionizeFormula(form);
    return form;
  }

  /**
   * Ionizes the given formula. If a charged formula is given, the charges will be added together.
   * See {@link FormulaUtils#neutralizeFormulaWithHydrogen(IMolecularFormula)} for formula
   * neutralisation.
   *
   * @param form The input formula.
   */
  public void ionizeFormula(IMolecularFormula form) {
    if (addedFormula != null) {
      form.add(addedFormula);
    }
    if (removedFormula != null) {
      FormulaUtils.subtractFormula(form, removedFormula);
    }
    final int c = Objects.requireNonNullElse(form.getCharge(), 0);
    form.setCharge(c + this.charge);
  }

  public IMolecularFormula getAddedFormula() {
    return addedFormula;
  }
}
