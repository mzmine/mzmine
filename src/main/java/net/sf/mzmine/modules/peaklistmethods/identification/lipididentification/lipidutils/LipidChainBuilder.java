package net.sf.mzmine.modules.peaklistmethods.identification.lipididentification.lipidutils;

public class LipidChainBuilder {

  LipidChainBuilder() {

  }

  public String calculateTotalChainFormula(final int chainLength, final int chainDoubleBonds,
      final int numberOfAcylChains, final int numberOfAlkylChains) {
    // Calculate sum formulas for chains
    // String acylChainFormula =
    // calculateAcylChainFormula(chainLength, chainDoubleBonds, numberOfAcylChains);
    // String alkylChainFormula =
    // calculateAlkylChainFormula(chainLength, chainDoubleBonds, numberOfAlkylChains);
    //
    // System.out.println(acylChainFormula);
    // System.out.println(alkylChainFormula);
    //
    // // Merge Strings
    // String totalChainFormulaUnsorted = acylChainFormula + alkylChainFormula;
    // System.out.println("merged formula: " + totalChainFormulaUnsorted);
    // // Order sum formulas
    // Map<String, Integer> totalChainFormulaUnsortedMap =
    // FormulaUtils.parseFormula(totalChainFormulaUnsorted);
    // String totalChainFormula = FormulaUtils.formatFormula(totalChainFormulaUnsortedMap);
    // System.out.println("sorted formula: " + totalChainFormula);
    String totalChainFormula = null;
    totalChainFormula = calculateChainFormula(chainLength, chainDoubleBonds, numberOfAcylChains,
        numberOfAlkylChains);
    return totalChainFormula;
  }

  private String calculateChainFormula(final int chainLength, final int chainDoubleBonds,
      final int numberOfAcylChains, final int numberOfAlkylChains) {
    String chainFormula = null;
    if (chainLength > 0) {
      final int numberOfHydrogens =
          chainLength * 2 - chainDoubleBonds * 2 - numberOfAcylChains - numberOfAlkylChains;
      final int numberOfCarbons = chainLength - numberOfAcylChains;
      chainFormula = "C" + numberOfCarbons + 'H' + numberOfHydrogens;
    }
    System.out.println(numberOfAcylChains + " " + numberOfAlkylChains);
    return chainFormula;
  }

  private int correctNumberOfCarbons(int numberOfCarbonsNotCorrected, int numberOfAcylChains,
      int numberOfAlkylChains) {
    int numberOfCarbonsCorrected = 0;
    numberOfCarbonsCorrected = numberOfCarbonsNotCorrected - numberOfAcylChains;
    return numberOfCarbonsCorrected;
  }

  private String calculateAcylChainFormula(final int chainLength, final int chainDoubleBonds,
      final int numberOfAcylChains) {
    String acylChainFormula = null;
    for (int i = 0; i < numberOfAcylChains; i++) {
      if (chainLength > 0) {
        final int numberOfHydrogens = chainLength * 2 - chainDoubleBonds * 2 - numberOfAcylChains;
        final int numberOfCarbons = chainLength - numberOfAcylChains;
        acylChainFormula = "C" + numberOfCarbons + 'H' + numberOfHydrogens;
      }
    }
    return acylChainFormula;
  }

  private String calculateAlkylChainFormula(final int chainLength, final int chainDoubleBonds,
      final int numberOfAlkylChains) {
    String alkylChainFormula = null;
    for (int i = 0; i < numberOfAlkylChains; i++) {
      if (chainLength > 0) {
        final int numberOfHydrogens = chainLength * 2 - chainDoubleBonds * 2 - numberOfAlkylChains;
        alkylChainFormula = "C" + chainLength + 'H' + numberOfHydrogens;
      }
    }
    return alkylChainFormula;
  }

}
