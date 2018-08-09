package net.sf.mzmine.modules.peaklistmethods.identification.lipididentification.lipididentificationtools;

public class LipidTools {

  private int numberOfCAtoms;
  private int numberOfDoubleBonds;

  public int getNumberOfCAtoms(String lipidAnnotation) {

    // int counterFirstBracket = 0;
    // int counterSecondBracket = 0;
    // int indexFirstNumber = 0;
    // int indexLastNumber = 0;
    //
    // // Loop through every char and check for "C"
    // for (int i = 0; i < lipidAnnotation.length(); i++) {
    // // get first Bracket
    // if (lipidAnnotation.charAt(i) == '(' && counterFirstBracket == 0) {
    // indexFirstNumber = counterFirstBracket++;
    // for (int j = 0; j < lipidAnnotation.length(); j++) {
    // if (lipidAnnotation.charAt(j) == 'H' && counterH == 0) {
    // counterH++;
    // if (counterH == 1) {
    // indexFirstH = j;
    // }
    //
    // }
    // }
    // }
    // // get second C
    // if (formula.charAt(i) == 'C' && i != indexFirstC) {
    // counterC++;
    // if (counterC == 2) {
    // indexSecondC = i;
    // }
    // for (int j = 0; j < formula.length(); j++) {
    // if (formula.charAt(j) == 'H' && j != indexFirstH) {
    // counterH++;
    // if (counterH == 2) {
    // indexSecondH = j;
    // }
    //
    // }
    // }
    // }
    //
    // }
    //
    // // Combine to total number of C
    // firstCNumbers = formula.substring(indexFirstC + 1, indexFirstH);
    // secondCNumbers = formula.substring(indexSecondC + 1, indexSecondH);
    // numberOfCAtoms = Integer.parseInt(firstCNumbers) + Integer.parseInt(secondCNumbers);
    return numberOfCAtoms;
  }
}
