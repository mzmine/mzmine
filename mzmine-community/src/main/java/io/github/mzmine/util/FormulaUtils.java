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

package io.github.mzmine.util;

import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonTypeType;
import io.github.mzmine.datamodel.features.types.numbers.NeutralMassType;
import io.github.mzmine.datamodel.identities.MolecularFormulaIdentity;
import io.github.mzmine.datamodel.identities.iontype.IonModification;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.config.IsotopeFactory;
import org.openscience.cdk.config.Isotopes;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.formula.MolecularFormula;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.interfaces.IIsotope;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public class FormulaUtils {

  /**
   * https://physics.nist.gov/cgi-bin/cuu/Value?meu|search_for=electron+mass 2022/05/04
   */
  public static final double electronMass = 0.000548579909065;
  private static final Logger logger = Logger.getLogger(FormulaUtils.class.getName());

  /**
   * Sort all molecular formulas by score of ppm distance, isotope sccore and msms score (with
   * weighting). Best will be at first position
   */
  public static void sortFormulaList(List<? extends MolecularFormulaIdentity> list, float ppmMax,
      float weightIsotopeScore, float weightMSMSscore) {
    if (list == null) {
      return;
    }

    list.sort((a, b) -> {
      float scoreA = a.getScore(ppmMax, weightIsotopeScore, weightMSMSscore);
      float scoreB = b.getScore(ppmMax, weightIsotopeScore, weightMSMSscore);
      // best to position 0 (therefore change A B)
      return Double.compare(scoreB, scoreA);
    });
  }

  public static void sortFormulaList(List<? extends MolecularFormulaIdentity> list,
      double neutralMassOverride, float ppmMax, float weightIsotopeScore, float weightMSMSscore) {
    if (list == null) {
      return;
    }

    list.sort((a, b) -> {
      double scoreA = a.getScore(neutralMassOverride, ppmMax, weightIsotopeScore, weightMSMSscore);
      double scoreB = b.getScore(neutralMassOverride, ppmMax, weightIsotopeScore, weightMSMSscore);
      // best to position 0 (therefore change A B)
      return Double.compare(scoreB, scoreA);
    });
  }

  public static void getAllSubFormulas(IMolecularFormula formula) {
    String f = "C6H6O2N";
  }

  /**
   * Returns the exact mass of an element. Mass is obtained from the CDK library.
   */
  public static double getElementMass(String element) {
    try {
      Isotopes isotopeFactory = Isotopes.getInstance();
      IIsotope majorIsotope = isotopeFactory.getMajorIsotope(element);
      // If the isotope symbol does not exist, return 0
      if (majorIsotope == null) {
        return 0;
      }
      return majorIsotope.getExactMass();
    } catch (IOException e) {
      e.printStackTrace();
      return 0;
    }
  }

  public static boolean containsElement(IMolecularFormula f, String element) {
    for (IIsotope iso : f.isotopes()) {
      if (iso.getSymbol().equals(element)) {
        return true;
      }
    }
    return false;
  }

  public static int countElement(IMolecularFormula f, String element) {
    int count = 0;
    for (IIsotope iso : f.isotopes()) {
      if (iso.getSymbol().equals(element)) {
        count += f.getIsotopeCount(iso);
      }
    }
    return count;
  }

  @NotNull
  public static Map<String, Integer> parseFormula(String formula) {

    Map<String, Integer> parsedFormula = new Hashtable<>();

    Pattern pattern = Pattern.compile("([A-Z][a-z]?)(-?[0-9]*)");
    Matcher matcher = pattern.matcher(formula);

    while (matcher.find()) {
      String element = matcher.group(1);
      String countString = matcher.group(2);
      int addCount = 1;
      if ((countString.length() > 0) && (!countString.equals("-"))) {
        addCount = Integer.parseInt(countString);
      }
      int currentCount = 0;
      if (parsedFormula.containsKey(element)) {
        currentCount = parsedFormula.get(element);
      }
      int newCount = currentCount + addCount;
      parsedFormula.put(element, newCount);
    }
    return parsedFormula;
  }

  @NotNull
  public static String formatFormula(@NotNull Map<String, Integer> parsedFormula) {

    StringBuilder formattedFormula = new StringBuilder();

    // Use TreeSet to sort the elements by alphabet
    TreeSet<String> elements = new TreeSet<>(parsedFormula.keySet());

    if (elements.contains("C")) {
      int countC = parsedFormula.get("C");
      formattedFormula.append("C");
      if (countC > 1) {
        formattedFormula.append(countC);
      }
      elements.remove("C");
      if (elements.contains("H")) {
        int countH = parsedFormula.get("H");
        formattedFormula.append("H");
        if (countH > 1) {
          formattedFormula.append(countH);
        }
        elements.remove("H");
      }
    }
    for (String element : elements) {
      formattedFormula.append(element);
      int count = parsedFormula.get(element);
      if (count > 1) {
        formattedFormula.append(count);
      }
    }
    return formattedFormula.toString();
  }

  /**
   * Calculate m/z ratio for given ionic formula string
   * <p>
   * This method utilizes existing cdk codebase to parse the ionic formula string and then obtain
   * its exact mass and charge Seems like the cdk builder used does not support isotopic notation
   * such as (15N)5 but recursive subformulas should be supported If no charge is given then +1 is
   * used For charge of more than -1 or +1 use square brackets Uses monoisotopic masses of each atom
   * present in the compound
   * <p>
   * Example outputs C23H39N7O17P3S+    gives 810.1330500980904 (charge is +1) C23H39N7O17P3S- gives
   * 810.1341472579095 (charge is -1) C10H16N5O10P2+     gives 428.03669139209063 (charge is +1)
   * C5H6(CH2)5N5O10P2+ gives 428.03669139209063 (charge is +1) [C21H30N7O17P3]2+  gives
   * 372.54500261509054 (charge is +2)
   *
   * @param ionicFormula ionic formula string
   * @return ion m/z ratio
   */
  public static double calculateMzRatio(String ionicFormula) {
    IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();
    IMolecularFormula mf = MolecularFormulaManipulator.getMolecularFormula(ionicFormula, builder);

    int charge = 1;
    char lastChar = ionicFormula.charAt(ionicFormula.length() - 1);
    if (lastChar == '-') {
      charge = -1;
    }
    if (mf.getCharge() != null) {
      charge = mf.getCharge();
    }

    double mass = MolecularFormulaManipulator.getMass(mf, MolecularFormulaManipulator.MonoIsotopic);
    mass -= charge * electronMass;

    return Math.abs(mass / charge);
  }

  /**
   * Calculates the m/z of the given formula. Formula must have a charge, otherwise the neutral mass
   * is returned
   *
   * @param formula The formula.
   * @return the calculated m/z ratio. if the formula's charge is null or 0, the neutral mass is
   * returned assuming charge 1 without knowledge of polarity for subtracting or adding an electron
   */
  public static double calculateMzRatio(@NotNull final IMolecularFormula formula) {
    double neutralmass = MolecularFormulaManipulator.getMass(formula,
        MolecularFormulaManipulator.MonoIsotopic);
    final Integer charge = formula.getCharge();
    if (charge == null || charge == 0) {
      return neutralmass;
    }

    return (neutralmass - charge * electronMass) / Math.abs(charge);
  }

  public static double calculateExactMass(String formula) {
    return calculateExactMass(formula, 0);
  }

  /**
   * Calculates exact monoisotopic mass of a given formula. Note that the returned mass may be
   * negative, in case the formula contains negative such as C3H10P-3. This is important for
   * calculating the mass of some ionization adducts, such as deprotonation (H-1).
   */
  public static double calculateExactMass(String formula, int charge) {

    if (formula.trim().length() == 0) {
      return 0;
    }

    Map<String, Integer> parsedFormula = parseFormula(formula);

    double totalMass = 0;
    for (String element : parsedFormula.keySet()) {
      int count = parsedFormula.get(element);
      double elementMass = getElementMass(element);
      totalMass += count * elementMass;
    }

    totalMass -= charge * electronMass;

    return totalMass;
  }

  public static String ionizeFormula(String formula, IonizationType ionType) {
    final IMolecularFormula form = ionType.ionizeFormula(formula);
    return MolecularFormulaManipulator.getString(form);
  }

  /**
   * Checks if a formula string only contains valid isotopes/elements.
   *
   * @param formula String of the molecular formula.
   * @return true / false
   */
  public static boolean checkMolecularFormula(String formula) {
    if (formula.matches(".*[äöüÄÖÜß°§$%&/()=?ß²³´`+*~'#;:<>|]")) { // check
      // for
      // this
      // first
      logger.info("Formula contains illegal characters.");
      return false;
    }
    IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();
    IMolecularFormula molFormula;

    try {
      molFormula = MolecularFormulaManipulator.getMajorIsotopeMolecularFormula(formula, builder);
      if (molFormula == null) {
        return false;
      }
    } catch (RuntimeException e) {
      logger.info("Cannot parse formula " + formula);
      return false;
    }

    boolean valid = true;

    for (IIsotope iso : molFormula.isotopes()) {
      if ((iso.getAtomicNumber() == null) || (iso.getAtomicNumber() == 0)) {
        // iso.getAtomicNumber() != null has to be checked, e.g. for
        // some reason an element with
        // Symbol "R" and number 0 exists in the CDK
        valid = false;
      }
    }

    if (!valid) {
      logger.warning("Formula invalid! Formula contains element symbols that do not exist.");
      return false;
    }
    return true;
  }

  /**
   * @param formula formula maybe with defined isotopes
   * @return mono isotopic mass
   */
  public static double getMonoisotopicMass(IMolecularFormula formula) {
    return formula == null ? 0d
        : MolecularFormulaManipulator.getMass(formula, MolecularFormulaManipulator.MonoIsotopic);
  }

  /**
   * Creates a formula with the major isotopes (important to use this method for exact mass
   * calculation over the CDK version, which generates formulas without an exact mass)
   *
   * @return the formula or null
   */
  @Nullable
  public static IMolecularFormula createMajorIsotopeMolFormula(String formula) {
    try {
      // new formula consists of isotopes without exact mass
      IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();
      IMolecularFormula f = MolecularFormulaManipulator.getMajorIsotopeMolecularFormula(
          formula.replace(" ", ""), builder);

      if (f == null) {
        return null;
      }
      // replace isotopes
      // needed, as MolecularFormulaManipulator method returns isotopes
      // without exact mass info
      try {
        return replaceAllIsotopesWithoutExactMass(f);
      } catch (Exception e) {
        logger.log(Level.SEVERE, "Cannot create formula for: " + formula, e);
        return null;
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Cannot create formula for: " + formula, e);
      return null;
    }
  }

  /**
   * Searches for all isotopes exactmass=null and replaces them with the major isotope
   *
   * @throws IOException
   */
  public static IMolecularFormula replaceAllIsotopesWithoutExactMass(IMolecularFormula f)
      throws IOException {
    if (f == null) {
      return null;
    }
    for (IIsotope iso : f.isotopes()) {
      // find isotope without exact mass
      if (iso.getExactMass() == null || iso.getExactMass() == 0) {
        int isotopeCount = f.getIsotopeCount(iso);
        f.removeIsotope(iso);

        // replace
        IsotopeFactory iFac = Isotopes.getInstance();
        IIsotope major = iFac.getMajorIsotope(iso.getAtomicNumber());
        if (major != null) {
          f.addIsotope(major, isotopeCount);
        }
        return replaceAllIsotopesWithoutExactMass(f);
      }
    }
    // no isotope found
    return f;
  }

  /**
   * Changes the absolute charge of the input formula
   *
   * @param formula   is changed
   * @param absCharge
   * @return the input formula but with changed charge
   */
  public static IMolecularFormula resetAbsCharge(IMolecularFormula formula, int absCharge) {
    int charge = Objects.requireNonNullElse(formula.getCharge(), 0);
    formula.setCharge(charge < 0 ? -absCharge : absCharge);
    return formula;
  }

  /**
   * Get all sub formulas + the original formula
   *
   * @param f original formula
   * @return list of original formula followed by sub formulas, sorted by ascending mz
   */
  public static FormulaWithExactMz[] getAllFormulas(IMolecularFormula f) {
    return getAllFormulas(f, null, 0);
  }

  /**
   * Get all sub formulas + the original formula. See
   * {@link #getAllFormulas(IMolecularFormula, Integer, double)} Resetting charge to single charge
   * is useful for fragmentation spectra where charges are often lost to become single charged. Or
   * to 0 for neutral losses.
   *
   * @param inputFormula original formula
   * @param minMzValue   the minimum mz value to consider
   * @return list of original formula followed by sub formulas, sorted by ascending mz
   */
  public static FormulaWithExactMz[] getAllFormulas(IMolecularFormula inputFormula,
      double minMzValue) {
    return getAllFormulas(inputFormula, null, minMzValue);
  }

  /**
   * Get all sub formulas + the original formula. Resetting charge to single charge is useful for
   * fragmentation spectra where charges are often lost to become single charged. Or to 0 for
   * neutral losses.
   *
   * @param inputFormula   original formula
   * @param resetAbsCharge the new absolute charge. use current charge if null or set the charge to
   *                       +-resetAbsCharge.
   * @param minMzValue     the minimum mz value to consider
   * @return list of original formula followed by sub formulas, sorted by ascending mz
   */
  @Nullable
  public static FormulaWithExactMz[] getAllFormulas(IMolecularFormula inputFormula,
      @Nullable Integer resetAbsCharge, double minMzValue) {
    if (inputFormula == null) {
      return null;
    }

    if (resetAbsCharge != null) {
      inputFormula = resetAbsCharge(inputFormula, resetAbsCharge);
    }

    List<IMolecularFormula> result = new ArrayList<>();
    result.add(inputFormula);
    getAllFormulas(result, inputFormula);
    //
    FormulaWithExactMz[] formulas = new FormulaWithExactMz[result.size()];
    for (int i = 0; i < result.size(); i++) {
      IMolecularFormula formula = result.get(i);
      formulas[i] = new FormulaWithExactMz(formula, calculateMzRatio(formula));
    }
    Arrays.sort(formulas, Comparator.comparingDouble(FormulaWithExactMz::mz));
    if (minMzValue > 0) {
      int index = getClosestIndexOfFormula(minMzValue, formulas);
      if (formulas[index].mz() < minMzValue) {
        index++;
      }
      if (index > 0) {
        if (index > formulas.length) {
          return new FormulaWithExactMz[0];
        }

        formulas = Arrays.copyOfRange(formulas, index, formulas.length);
      }
    }
    return formulas;
  }

  /**
   * Remove one isotope at a time. Keep charge state
   *
   * @param result       list that is expanded by all sub formulas
   * @param inputFormula input formula
   */
  private static void getAllFormulas(List<IMolecularFormula> result,
      IMolecularFormula inputFormula) {
    for (IIsotope iso : inputFormula.isotopes()) {
      // do not use enhanced loop - adding new elements to the end of the list
      int size = result.size();
      for (int i = 0; i < size; i++) {
        IMolecularFormula f = result.get(i);
        int count = f.getIsotopeCount(iso);
        // remove at least one - all
        for (int newCount = count - 1; newCount >= 0; newCount--) {
          IMolecularFormula newFormula = cloneWithIsotopeCount(f, iso, newCount);
          if (newFormula.getIsotopeCount() > 0) {
            result.add(newFormula);
          }
        }
      }
    }
  }


  /**
   * @param mz               search for mz
   * @param formulasMzSorted formulas sorted by mz {@link #getAllFormulas(IMolecularFormula)}
   * @return the formula with the closest mz
   */
  @Nullable
  public static FormulaWithExactMz getClosestFormula(double mz,
      FormulaWithExactMz[] formulasMzSorted) {
    int index = getClosestIndexOfFormula(mz, formulasMzSorted);
    return index >= 0 ? formulasMzSorted[index] : null;
  }

  /**
   * @param mz               search for mz
   * @param formulasMzSorted formulas sorted by mz {@link #getAllFormulas(IMolecularFormula)}
   * @return the index of the formula with the closest mz or -1 if the list is empty
   */
  public static int getClosestIndexOfFormula(double mz, FormulaWithExactMz[] formulasMzSorted) {
    if (formulasMzSorted == null || formulasMzSorted.length == 0) {
      return -1;
    }
    if (mz < formulasMzSorted[0].mz()) {
      return 0;
    }
    if (mz > formulasMzSorted[formulasMzSorted.length - 1].mz()) {
      return formulasMzSorted.length - 1;
    }

    int lo = 0;
    int hi = formulasMzSorted.length - 1;

    while (lo <= hi) {
      int mid = (hi + lo) / 2;

      if (mz < formulasMzSorted[mid].mz()) {
        hi = mid - 1;
      } else if (mz > formulasMzSorted[mid].mz()) {
        lo = mid + 1;
      } else {
        return mid;
      }
    }
    // lo == hi + 1
    return (formulasMzSorted[lo].mz() - mz) < (mz - formulasMzSorted[hi].mz()) ? lo : hi;
  }


  public static IMolecularFormula cloneWithIsotopeCount(IMolecularFormula f, IIsotope iso,
      int newCount) {
    MolecularFormula newf = new MolecularFormula();
    newf.setCharge(f.getCharge());
    for (IIsotope isotope : f.isotopes()) {
      if (isotope.equals(iso)) {
        if (newCount > 0) {
          newf.addIsotope(isotope, newCount);
        }
      } else {
        newf.addIsotope(isotope, f.getIsotopeCount(isotope));
      }
    }
    return newf;
  }

  /**
   * @param result is going to be changed. is also the returned value
   */
  public static IMolecularFormula subtractFormula(IMolecularFormula result, IMolecularFormula sub) {
    for (IIsotope isotope : sub.isotopes()) {
      int count = sub.getIsotopeCount(isotope);
      boolean found = false;
      do {
        found = false;
        for (IIsotope realIsotope : result.isotopes()) {
          // there can be different implementations of IIsotope
          if (equalIsotopes(isotope, realIsotope)) {
            found = true;
            int realCount = result.getIsotopeCount(realIsotope);
            int remaining = realCount - count;
            result.removeIsotope(realIsotope);
            if (remaining > 0) {
              result.addIsotope(realIsotope, remaining);
            }
            count -= realCount;
            break;
          }
        }
      } while (count > 0 && found);
    }
    final Integer resultCharge = Objects.requireNonNullElse(result.getCharge(), 0);
    final Integer subtractCharge = Objects.requireNonNullElse(sub.getCharge(), 0);
    result.setCharge(resultCharge - subtractCharge);
    return result;
  }

  /**
   * @param result is going to be changed. is also the returned value
   */
  public static IMolecularFormula addFormula(IMolecularFormula result, IMolecularFormula add) {
    result.add(add);
    final Integer resultCharge = Objects.requireNonNullElse(result.getCharge(), 0);
    final Integer subtractCharge = Objects.requireNonNullElse(add.getCharge(), 0);
    result.setCharge(resultCharge + subtractCharge);
    return result;
  }

  /**
   * Compare to IIsotope. The method doesn't compare instance but if they have the same symbol,
   * natural abundance and exact mass. TODO
   *
   * @param isotopeOne The first Isotope to compare
   * @param isotopeTwo The second Isotope to compare
   * @return True, if both isotope are the same
   */
  private static boolean equalIsotopes(IIsotope isotopeOne, IIsotope isotopeTwo) {
    return isotopeOne.getSymbol().equals(isotopeTwo.getSymbol());
    // exactMass and naturalAbundance is null when using
    // createMajorIsotopeMolFormula
    // // XXX: floating point comparision!
    // if (!Objects.equals(isotopeOne.getNaturalAbundance(),
    // isotopeTwo.getNaturalAbundance()))
    // return false;
    // if (!Objects.equals(isotopeOne.getExactMass(),
    // isotopeTwo.getExactMass()))
    // return false;
  }

  public static long getFormulaSize(String formula) {
    long size = 1;

    IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();
    IMolecularFormula molFormula;

    molFormula = MolecularFormulaManipulator.getMajorIsotopeMolecularFormula(formula, builder);
    Isotopes isotopeFactory;
    try {
      isotopeFactory = Isotopes.getInstance();
      for (IIsotope iso : molFormula.isotopes()) {

        int naturalIsotopes = 0;
        for (IIsotope i : isotopeFactory.getIsotopes(iso.getSymbol())) {
          if (i.getNaturalAbundance() > 0.0) {
            naturalIsotopes++;
          }

        }

        try {
          size = Math.multiplyExact(size, (molFormula.getIsotopeCount(iso) * naturalIsotopes));
        } catch (ArithmeticException e) {
          e.printStackTrace();
          logger.info("Formula size of " + formula + " is too big.");
          return -1;
        }
      }
    } catch (IOException e) {
      logger.warning("Unable to initialise Isotopes.");
      e.printStackTrace();
    }

    return size;
  }

  /**
   * @param smiles The smiles string.
   * @return A molecular formula representing the smiles or null, if the smiles cannot be parsed.
   */
  @Nullable
  public static IMolecularFormula getFormulaFromSmiles(@Nullable String smiles) {
    if (smiles == null) {
      return null;
    }
    try {
      final IAtomContainer iAtomContainer = new SmilesParser(
          SilentChemObjectBuilder.getInstance()).parseSmiles(smiles);
      return MolecularFormulaManipulator.getMolecularFormula(iAtomContainer);
    } catch (InvalidSmilesException e) {
      logger.log(Level.SEVERE, e.getMessage(), e);
    }
    return null;
  }

  /**
   * Creates a neutral formula from the given formula string. In case the formula is not neutral,
   * the formula is neutralized using protons.
   *
   * @param formula The formula.
   * @return The neutral formula.
   */
  @Nullable
  public static IMolecularFormula neutralizeFormulaWithHydrogen(@Nullable final String formula) {
    if (formula == null) {
      return null;
    }
    return neutralizeFormulaWithHydrogen(createMajorIsotopeMolFormula(formula));
  }

  /**
   * Creates a neutral formula from the given formula string. In case the formula is not neutral,
   * the formula is neutralized using protons.
   *
   * @param formula The formula. Remains unaltered, cloned during this method.
   * @return The neutral or null if the formula cannot be cloned or neutralized by protonation.
   */
  @Nullable
  public static IMolecularFormula neutralizeFormulaWithHydrogen(
      @Nullable final IMolecularFormula formula) {
    if (formula == null) {
      return null;
    }

    try {
      final IMolecularFormula molecularFormula = (IMolecularFormula) formula.clone();
      final Integer charge = molecularFormula.getCharge();
      if (charge != null && charge != 0) {
        final String string = MolecularFormulaManipulator.getString(molecularFormula);

        logger.finest(
            () -> "Compound " + string + " is not neutral as determined by molFormula. charge = "
                + charge + ". Adjusting protonation.");

        final boolean adjusted = MolecularFormulaManipulator.adjustProtonation(molecularFormula,
            -charge);
        if (!adjusted || molecularFormula.getCharge() != 0) {
          logger.info(() -> "Cannot determine neutral formula by adjusting protons. " + string);
          return null;
        }
      }
      return molecularFormula;
    } catch (CloneNotSupportedException e) {
      logger.log(Level.SEVERE, e.getMessage(), e);
      return null;
    }
  }

  @Nullable
  public static IMolecularFormula cloneFormula(@Nullable final IMolecularFormula formula) {
    if (formula == null) {
      return null;
    }
    try {
      return (IMolecularFormula) formula.clone();
    } catch (CloneNotSupportedException e) {
      throw new IllegalArgumentException("Cannot clone given formula. " + formula);
    }
  }

  /**
   * Creates the ionized formula combining the adduct from the feature annotation
   */
  public static @Nullable IMolecularFormula getIonizedFormula(final FeatureAnnotation annotation) {
    if (annotation.getFormula() == null || annotation.getFormula().isBlank()) {
      return null;
    }

    IMolecularFormula molecularFormula = FormulaUtils.neutralizeFormulaWithHydrogen(
        annotation.getFormula());
    assert molecularFormula != null;

    if (annotation.getAdductType() == null && annotation instanceof CompoundDBAnnotation c
        && annotation.getPrecursorMZ() != null && c.get(
        NeutralMassType.class) instanceof Double neutralMass) {
      final IonModification mod = IonModification.getBestIonModification(neutralMass,
          annotation.getPrecursorMZ(), MZTolerance.FIFTEEN_PPM_OR_FIVE_MDA, null);
      c.put(IonTypeType.class, new IonType(mod));
    }

    if (annotation.getAdductType() == null) {
      return null;
    }
    try {
      // ionize formula
      // considering both 2M etc
      return annotation.getAdductType().addToFormula(molecularFormula);
    } catch (CloneNotSupportedException e) {
      logger.log(Level.WARNING, "Cannot ionize formula");
      throw new RuntimeException(e);
    }
  }

  public static boolean isSubFormula(FormulaWithExactMz a, FormulaWithExactMz b) {
    if (a.mz() <= b.mz()) {
      return isSubFormula(a.formula(), b.formula());
    } else {
      return isSubFormula(b.formula(), a.formula());
    }
  }

  private static boolean isSubFormula(IMolecularFormula smaller, IMolecularFormula larger) {
    for (IIsotope isotope : smaller.isotopes()) {
      if (smaller.getIsotopeCount(isotope) > larger.getIsotopeCount(isotope)) {
        return false;
      }
    }
    return true;
  }

}
