package net.sf.mzmine.modules.peaklistmethods.identification.lipididentification.lipidutils;

public class LipidChainBuilder {

  LipidChainBuilder() {

  }

  public String calculateChainFormula(final int chainLength, final int chainDoubleBonds,
      final int numberOfAcylChains, final int numberOfAlkylChains) {
    String chainFormula = null;
    if (chainLength > 0) { // +1 H for CH3 last CH3 group
      final int numberOfHydrogens = (1 * numberOfAcylChains + 1 * numberOfAlkylChains)// +1H for las
                                                                                      // CH3 group
          + (chainLength * 2 - chainDoubleBonds * 2) // double bond correction
          - 2 * numberOfAcylChains; // remove 2 H for C in acyl group
      final int numberOfCarbons = chainLength - numberOfAcylChains;
      // correctNumberOfCarbons(chainLength, numberOfAcylChains, numberOfAlkylChains);
      chainFormula = "C" + numberOfCarbons + 'H' + numberOfHydrogens;
    }
    return chainFormula;
  }

}
