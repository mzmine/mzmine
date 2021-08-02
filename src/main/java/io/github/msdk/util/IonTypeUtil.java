/*
 * (C) Copyright 2015-2017 by MSDK Development Team
 *
 * This software is dual-licensed under either
 *
 * (a) the terms of the GNU Lesser General Public License version 2.1 as published by the Free
 * Software Foundation
 *
 * or (per the licensee's choosing)
 *
 * (b) the terms of the Eclipse Public License v1.0 as published by the Eclipse Foundation.
 */

package io.github.msdk.util;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import com.google.common.base.Strings;

import io.github.msdk.MSDKRuntimeException;
import io.github.msdk.datamodel.IonType;
import io.github.msdk.datamodel.PolarityType;
import io.github.msdk.datamodel.SimpleIonType;

/**
 * IonType utilities
 */
public class IonTypeUtil {

  private static final Pattern ionTypePattern =
      Pattern.compile("\\[(\\d*)M([+-]?.*)\\](\\d*)([+-])");

  private static final Pattern adductPattern = Pattern.compile("([+-])(\\d*)([\\w]+)");

  private static final Pattern elementPattern = Pattern.compile("([A-Z][a-z]*)(\\d*)");

  /**
   * Creates an IonType from a string. The expected string format is [M+2H]2+.
   *
   * @return a {@link IonType} object.
   * @param adduct a {@link String} object.
   */
  public static @Nonnull IonType createIonType(final @Nonnull String adduct) {

    Matcher m = ionTypePattern.matcher(adduct);

    if (!m.matches())
      throw new MSDKRuntimeException("Cannot parse ion type " + adduct);

    final String numOfMoleculesGroup = m.group(1);
    final String adductFormulaGroup = m.group(2);
    final String chargeGroup = m.group(3);
    final String polarityGroup = m.group(4);

    try {

      // Polarity type
      PolarityType polarity;
      switch (polarityGroup) {
        case "+":
          polarity = PolarityType.POSITIVE;
          break;
        case "-":
          polarity = PolarityType.NEGATIVE;
          break;
        default:
          polarity = PolarityType.UNKNOWN;
      }

      // Number of molecules
      Integer numberOfMolecules = 1;
      if (!Strings.isNullOrEmpty(numOfMoleculesGroup))
        numberOfMolecules = Integer.parseInt(numOfMoleculesGroup);

      // Charge
      Integer charge = 1;
      if (!Strings.isNullOrEmpty(chargeGroup))
        charge = Integer.parseInt(chargeGroup);

      // Adduct formula
      String adductFormula = parseAdductFormula(adductFormulaGroup);

      // Create ionType
      IonType ionType =
           new SimpleIonType(adduct, polarity, numberOfMolecules, adductFormula, charge);

      return ionType;

    } catch (Exception e) {
      throw new MSDKRuntimeException("Cannot parse ion type " + adduct);
    }

  }

  /**
   * Parses a "vague" formula from ionization adduct definition (e.g., -H2O+NH4 coming from adduct
   * type [2M-H2O+NH4]+) into a standard chemical formula. Note that the resulting formula may have
   * negative element counts, in case the adduct represents a loss. In the above adduct case, the
   * resulting formula would be H2NO-1
   * 
   * @param adductFormula Adduct formula in the form +XXX or -XXX or +XXX-YYY
   * @return Chemical formula formatted according to the Hill System
   */
  private static String parseAdductFormula(String adductFormula) {

    Hashtable<String, Integer> elementCounts = new Hashtable<>();

    Matcher m = adductPattern.matcher(adductFormula);
    while (m.find()) {
      String plusMinus = m.group(1);
      Integer multiplier = 1;
      if (!Strings.isNullOrEmpty(m.group(2)))
        multiplier = Integer.parseInt(m.group(2));
      String formula = m.group(3);
      Matcher f = elementPattern.matcher(formula);
      while (f.find()) {
        String element = f.group(1);
        Integer count = multiplier;
        if (!Strings.isNullOrEmpty(f.group(2)))
          count *= Integer.parseInt(f.group(2));
        if (plusMinus.equals("-"))
          count *= -1;
        if (elementCounts.containsKey(element))
          count += elementCounts.get(element);
        elementCounts.put(element, count);
      }
    }
    // Build the formula string according to Hill System rules
    StringBuilder formula = new StringBuilder();
    Integer cCount = elementCounts.get("C");
    if (cCount != null && cCount != 0) {
      formula.append("C");
      if (cCount != 1)
        formula.append(cCount);
    }
    elementCounts.remove("C");
    Integer hCount = elementCounts.get("H");
    if (hCount != null && hCount != 0) {
      formula.append("H");
      if (hCount != 1)
        formula.append(hCount);
    }
    elementCounts.remove("H");
    String elements[] = elementCounts.keySet().toArray(new String[0]);
    Arrays.sort(elements);
    for (String element : elements) {
      Integer count = elementCounts.get(element);
      if (count != 0) {
        formula.append(element);
        if (count != 1)
          formula.append(count);
      }
    }
    return formula.toString();
  }

}
