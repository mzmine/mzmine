package net.sf.mzmine.modules.peaklistmethods.identification.lipididentification.lipididentificationtools;

public class IsotopeLipidTools {

  private int numberOfCAtoms = 0;

  public int getNumberOfCAtoms(String formula) {

    int counterC = 0;
    int counterH = 0;
    int indexFirstC = 0;
    int indexFirstH = 0;
    int indexSecondC = 0;
    int indexSecondH = 0;

    String firstCNumbers = null;
    String secondCNumbers = null;
    // Loop through every char and check for "C"
    for (int i = 0; i < formula.length(); i++) {
      // get first C
      if (formula.charAt(i) == 'C' && counterC == 0) {
        counterC++;
        if (counterC == 1) {
          indexFirstC = i;
        }
        for (int j = 0; j < formula.length(); j++) {
          if (formula.charAt(j) == 'H' && counterH == 0) {
            counterH++;
            if (counterH == 1) {
              indexFirstH = j;
            }

          }
        }
      }
      // get second C
      if (formula.charAt(i) == 'C' && i != indexFirstC) {
        counterC++;
        if (counterC == 2) {
          indexSecondC = i;
        }
        for (int j = 0; j < formula.length(); j++) {
          if (formula.charAt(j) == 'H' && j != indexFirstH) {
            counterH++;
            if (counterH == 2) {
              indexSecondH = j;
            }

          }
        }
      }

    }

    // Combine to total number of C
    firstCNumbers = formula.substring(indexFirstC + 1, indexFirstH);
    secondCNumbers = formula.substring(indexSecondC + 1, indexSecondH);
    numberOfCAtoms = Integer.parseInt(firstCNumbers) + Integer.parseInt(secondCNumbers);
    return numberOfCAtoms;
  }

  public void setNumberOfCAtoms(int numberOfCAtoms) {
    this.numberOfCAtoms = numberOfCAtoms;
  }

}
