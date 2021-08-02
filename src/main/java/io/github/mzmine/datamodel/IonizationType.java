/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.datamodel;

public enum IonizationType {

  NO_IONIZATION("No ionization", "", 0, PolarityType.NEUTRAL,-6,0,0), //

  POSITIVE("[M]+", "+", -0.00054857990946, PolarityType.POSITIVE,-6,1,1), //

  POSITIVE_HYDROGEN("[M+H]+", "H",1.007276,PolarityType.POSITIVE,-0.268998651917666,1,1), //

  SODIUM("[M+Na]+", "Na",22.989218,PolarityType.POSITIVE,-0.963288182616102,1,1), //

  POTASSIUM("[M+K]+", "K", 38.963158,PolarityType.POSITIVE,-2.41599440912713,1,1), //

  LITHIUM("[M+Li]+", "Li", 7.01546, PolarityType.POSITIVE,-6,1,1), //

  AMMONIUM("[M+NH4]+", "NH4",18.033823,PolarityType.POSITIVE,-2.49171512306525,1,1), //

  NAME1("[M+2H-NH3]2+","H2",-15.0120166,PolarityType.POSITIVE,-3.51290442213519,1,2),//

  NAME2("[M]3+","",-0.001645737,PolarityType.POSITIVE,-3.51290442213519,1,3),//

  NAME3("[M]2+","",-0.0010404,PolarityType.POSITIVE,-3.51290442213519,1,2),//

  NAME4("[M+H]2+","H",1.006178842,PolarityType.POSITIVE,-3.33681316307951,1,2),//

  NAME5("[M+2H]2+","H2",2.014552,PolarityType.POSITIVE,-1.81393441779917,1,2),//

  NAME6("[M+H+Na]2+","NaH",23.996494,PolarityType.POSITIVE,-2.69999106549233,1,2),//

  NAME7("[M+2H+Na]3+","NaH2",25.00377,PolarityType.POSITIVE,-3.81393441779917,1,3),//

  NAME8("[M+H+K]2+","KH",39.970434,PolarityType.POSITIVE,-2.23415082118236,1,2),//

  NAME9("[M+2Na]2+","Na2",45.978436,PolarityType.POSITIVE,-2.66780638212093,1,2),//

  NAME10("[M+H+2Na]3+","Na2H",46.98573,PolarityType.POSITIVE,-3.51290442213519,1,3),//

  NAME11("[M+3Na]3+","Na3",68.967654,PolarityType.POSITIVE,-3.51290442213519,1,3),//

  NAME13("[M+H-H2O]+","H",-17.0032778,PolarityType.POSITIVE,-0.747608492437131,1,1),//

  NAME15("[M+H-NH3]+","H",-16.01927432,PolarityType.POSITIVE,-1.58862513607331,1,1),//

  NAME16("[M-H+2Na]+","Na2",44.97116444,PolarityType.POSITIVE,-1.85969190835984,1,1),//

  NAME18("[M-2H+3Na]+","Na3",66.9530814,PolarityType.POSITIVE,-1.91084443080722,1,1),//

  NAME19("[M+H+H2O]+","H2OH",19.01786821,PolarityType.POSITIVE,-2.3225727239649,1,1),//

  NAME22("[M-H+2K]+","K2",76.91904,PolarityType.POSITIVE,-3.11496441346315,1,1),//

  NAME23("[M+H2O]+","H2O",18.010011,PolarityType.POSITIVE,-3.21187442647121,1,1),//

  NAME24("[M+H-OH]+","H",-15.99548193,PolarityType.POSITIVE,-3.21187442647121,1,1),//

  NAME25("[M-H2O]+","",-18.0110879,PolarityType.POSITIVE,-3.51290442213519,1,1),//

  NAME26("[M-H]+","",-1.0083404,PolarityType.POSITIVE,-3.51290442213519,1,1),//

  NAME27("[M+Na-H2O]+","Na",4.978142219,PolarityType.POSITIVE,-3.51290442213519,1,1),//

  NAME28("[M-2H+3K]+","K3",114.8748814,PolarityType.POSITIVE,-3.51290442213519,1,1),//

  NAME29("[M+K-H2O]+","K",20.95204222,PolarityType.POSITIVE,-3.81393441779917,1,1),//

  NAME30("[M-CO2H+H]+","H",-43.98986378,PolarityType.POSITIVE,-4.81393441779917,1,1),//

  NAME32("[2M+H]+","H",1.007276,PolarityType.POSITIVE,-1.22398481647346,2,1),//

  NAME37("[2M+Na]+","Na",22.989218,PolarityType.POSITIVE,-2.96883637778491,2,1),//

  NAME34("[2M+Na-H2O]+","Na",4.978142219,PolarityType.POSITIVE,-3.81393441779917,2,1),//

  NAME35("[2M+K-H2O]+","K",20.95204222,PolarityType.POSITIVE,-3.81393441779917,2,1),//

  NAME38("[2M+K]+","K",38.96314222,PolarityType.POSITIVE,-3.81393441779917,2,1),//

  NAME33("[3M+H]+","H",1.007276,PolarityType.POSITIVE,-2.26986637344889,3,1),//

  NAME39("[3M+K]+","K",38.96314222,PolarityType.POSITIVE,-3.81393441779917,3,1),//

  NAME36("[3M+K-H2O]+","K",20.95204222,PolarityType.POSITIVE,-4.81393441779917,3,1),//

  NAME31("[3M+H-H2O]+","H",-17.0032778,PolarityType.POSITIVE,-4.81393441779917,3,1),//

  NEGATIVE("[M]-", "-", 0.00054857990946, PolarityType.NEGATIVE,-6,1,-1), //

  NEGATIVE_HYDROGEN("[M-H]-", "H",-1.007276,PolarityType.NEGATIVE,-0.172110574929576,1,-1), //

  NAME_2("[M+Na-3H]2-","Na",19.96739,PolarityType.NEGATIVE,-4.24802233641235,1,-2),//

  CARBONATE("[M+CO3]-", "CO3", 59.98529, PolarityType.NEGATIVE,-6,1,-1), //

  FORMATE("[M+HCOO]-", "HCOO", 44.998201, PolarityType.NEGATIVE,-1,1,-1), //

  PHOSPHATE("[M+H2PO4]-", "H2PO4", 96.96962, PolarityType.NEGATIVE,-6,1,-1), //

  ACETATE("[M+CH3COO]-", "CH3COO", 59.013851, PolarityType.NEGATIVE,-6,1,-1), //

  TRIFLUORACETATE("[M+CF3COO]-", "CF3COO", 112.985586, PolarityType.NEGATIVE,-6,1,-1), //

  CHLORIDE("[M+Cl]-", "Cl", 34.969402, PolarityType.NEGATIVE,-6,1,-1), //

  BROMIDE("[M+Br]-", "Br", 78.918885, PolarityType.NEGATIVE,-6,1,-1), //

  NAME_1("[M-2H]2-","2H",-2.014552,PolarityType.NEGATIVE,-1.4029242963981,1,-2),//

  NAME_4("[M-H-H2O]-","",-19.01839,PolarityType.NEGATIVE,-0.819887542383565,1,-1),//

  NAME_5("[M-H-NH3]-","",-18.03381516,PolarityType.NEGATIVE,-1.94699234074837,1,-1),//

  NAME_6("[M-H+H2O]-","H2O",17.003275,PolarityType.NEGATIVE,-2.13407898410552,1,-1),//

  NAME_7("[M-2H]-","",-2.01506,PolarityType.NEGATIVE,-2.77090108169269,1,-1),//

  NAME_8("[M+K-2H]-","K",36.9486306,PolarityType.NEGATIVE,-3.24802233641235,1,-1),//

  NAME_9("[M+Na-2H]-","Na",20.974666,PolarityType.NEGATIVE,-4.24802233641235,1,-1),//

  NAME_10("[2M-H]-","",-1.007276,PolarityType.NEGATIVE,-0.978509392194437,2,-1),//

  NAME_11("[3M-H]-","",-1.007276,PolarityType.NEGATIVE,-1.99274983130905,3,-1);


  // log10freq records log base 10 observed frequency of adducts and fragments from available LC-MS1
  // spectra for pure compounds available in the NIST database introduced in CliqueMS algorithm. The
  // compounds whose frequency is not yet observed is given a minimum log frequency value of -6.0 .

  private final String name, adductFormula;
  private final PolarityType polarity;
  private final double addedMass, log10freq;
  private final int numMol, charge;

  IonizationType(String name, String adductFormula, double addedMass, PolarityType polarity, double log10freq, int numMol, int charge) {

    this.name = name;
    this.adductFormula = adductFormula;
    this.addedMass = addedMass;
    this.log10freq = log10freq;
    this.polarity = polarity;
    this.numMol = numMol;
    this.charge = charge;
  }

  public String getAdductName() {
    return name;
  }

  public String getAdductFormula() {
    return adductFormula;
  }

  public double getAddedMass() {
    return addedMass;
  }

  public PolarityType getPolarity() {
    return polarity;
  }

  public double getLog10freq(){
    return log10freq;
  }

  public int getNumMol(){
    return numMol;
  }

  public int getCharge(){
    return charge;
  }

  @Override
  public String toString() {
    return name;
  }

}
