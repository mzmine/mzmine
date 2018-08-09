package net.sf.mzmine.modules.peaklistmethods.identification.lipididentification.lipididentificationtools;

/**
 * This enum contains specific fragments of a lipid class apart from fatty acids If no specific
 * fragment is listed in Lipid Maps, the enum is null sn1 represents fatty acid chains
 */

public enum MSMSLibrary {

  PC(new String[] {"sn1"}, null, null), //
  PI(new String[] {"[M-H]-sn1", "[M-H]-sn1-H2O", "[M-H]-sn1-C6H12O6"},
      new String[] {null, "H2O", "C6H12O6"}, new int[] {999, 200, 400}), //
  PE(new String[] {"[M-H]-sn1", "[M-H]-sn1-H2O", null}, new String[] {null, "H2O", null},
      new int[] {200, 50, 999}), //
  PS(new String[] {"[M-H]-C3H5NO2", "[M-H]-C3H5NO2-sn1+H2O", "[M-H]-C3H5NO2-sn1", "sn1"},
      new String[] {"C3H5NO2", "C3H3NO", "C3H5NO2", null}, new int[] {999, 200, 200, 100}), //
  PG(new String[] {"[M-H]-sn1", "[M-H]-sn1-H2O", "[M-H]-sn1-C3H8O3", "sn1"},
      new String[] {null, "H2O", "-C3H8O3", null}, new int[] {200, 200, 200, 999}), //
  BMP(new String[] {"sn1"}, null, null), //
  CL(new String[] {"[M-H]-sn1", "sn1+sn1+C6H11P2O8", "sn1+sn1+C6H10O5P", "sn1+sn1+C3H6PO4",
      "sn1+sn1+C6H11P2O8", "sn1+C3H6PO4+H2O", "sn1+C3H6PO4", "sn1"},
      new String[] {null, "C6H11P2O8", "C6H10O5P", "C3H6PO4", "C6H11P2O8", "C3H8PO5", "C3H6PO4",
          null},
      new int[] {50, 300, 100, 999, 300, 100, 200, 100}), //
  DAG(new String[] {"sn1"}, null, null), //
  TAG(new String[] {"sn1"}, null, null), //
  MGDG(new String[] {"sn1"}, null, null), //
  DGDG(new String[] {"sn1"}, null, null), //
  SQDG(new String[] {"[M-H]-sn1", "sn1", "fragment C6H9O7S"}, new String[] {null, null, "C6H9O7S"},
      new int[] {300, 100, 999}), //
  DGTS(new String[] {"fragment C10H22O5N", "fragment C7H14O2N"},
      new String[] {"C10H22O5N", "C7H14O2N"}, new int[] {100, 100}), //
  MELA(new String[] {"-139 -(Erythritol + NH3) ", "[M-139 - sn1]", "[M-139 - H2O]"},
      new String[] {"-139", "-139-sn1", "-139-H2O"}, new int[] {900, 900, 900}), //
  MELBC(new String[] {"-139 -(Erythritol + NH3) ", "[M-139 - sn1]", "[M-139 - H2O]"},
      new String[] {"-139", "-139-sn1", "-139-H2O"}, new int[] {900, 900, 900}), //
  MELD(new String[] {"-139 -(Erythritol + NH3) ", "[M-139 - sn1]", "[M-139 - H2O]"},
      new String[] {"-139", "-139-sn1", "-139-H2O"}, new int[] {900, 900, 900}), //
  diRL(
      new String[] {"[M-H]-sn1-2H", "sn1-2H", "[M-H]-C12H22O9(di-Rhamnose+H)",
          "[M-H]-C6H12O5(Rhamnose+H)", "C12H21O9 (di-Rhamnose)", "C6H11O5 (Rhamnose)"},
      new String[] {null, null, "C12H22O9", "C6H12O5", "C12H21O9", "C6H11O5"},
      new int[] {900, 900, 900, 900, 900, 900}), //
  mRL(new String[] {"[M-H]-sn1-2H", "sn1-2H", "[M-H]-C6H12O5(Rhamnose+H)", "C6H11O5 (Rhamnose)"},
      new String[] {null, null, "C6H12O5", "C6H11O5"}, new int[] {900, 900, 900, 900}), //
  HAA(new String[] {"sn1+O"}, null, null); //

  private final String[] name, formulaOfStaticFormula;
  private final int[] relativeIntensity;

  MSMSLibrary(String[] name, String[] formulaOfStaticFormula, int[] relativeIntensity) {

    this.name = name;
    this.formulaOfStaticFormula = formulaOfStaticFormula;
    this.relativeIntensity = relativeIntensity;
  }

  public String[] getFormulaOfStaticFormula() {
    return formulaOfStaticFormula;
  }

  public int[] getRelativeIntensity() {
    return relativeIntensity;
  }

  public String[] getName() {
    return this.name;
  }

}
