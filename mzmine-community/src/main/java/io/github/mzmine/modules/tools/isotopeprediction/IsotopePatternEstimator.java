/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

// cdk license as it is LGPL
package io.github.mzmine.modules.tools.isotopeprediction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.Element;
import org.openscience.cdk.config.IsotopeFactory;
import org.openscience.cdk.config.Isotopes;
import org.openscience.cdk.formula.IsotopeContainer;
import org.openscience.cdk.formula.IsotopePattern;
import org.openscience.cdk.formula.IsotopePatternManipulator;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.interfaces.IIsotope;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

/**
 * Estimates the most intense signals of an isotope pattern
 *
 * @author Miguel Rojas Cherto
 * @cdk.created 2007-11-20
 * @cdk.keyword isotope pattern
 *
 */
class IsotopePatternEstimator {

  /**
   * Default intensity of a peak in a calculated isotope pattern. normalized to 0-1.
   */
  public static final double DEFAULT_MIN_INTENSITY_IN_PATTERN = 0.1;
  /**
   * Still includes N and O isotopes but not [2]H
   */
  public static final double DEFAULT_FAST_ABUNDANCE = 0.002;
  public static final float DEFAULT_RESOLUTION = 0.0005f;

  private static final Logger logger = Logger.getLogger(IsotopePatternEstimator.class.getName());

  /**
   * Minimal abundance of the isotopes to be added in the combinatorial search.
   */
  private final double minIntensityInPattern;
  private final double minIsotopeAbundance;
  private final double resolution;
  private final boolean storeFormula;

  private IChemObjectBuilder builder = null;
  private IsotopeFactory isoFactory;

  public IsotopePatternEstimator() {
    this(DEFAULT_FAST_ABUNDANCE, DEFAULT_MIN_INTENSITY_IN_PATTERN, DEFAULT_RESOLUTION, false);
  }

  /**
   *
   * @param minIsotopeAbundance   Min abundance of a specific isotope to be included in pattern
   *                              calculation. <b>!!!VALUE IN 0-100%!!!</b>
   * @param minIntensityInPattern Min intensity of a peak in a calculated isotope pattern.
   *                              normalized to 0-1.
   * @param resolution            Resolution in dalton
   */
  public IsotopePatternEstimator(double minIsotopeAbundance, double minIntensityInPattern,
      double resolution, boolean storeFormula) {
    this.minIsotopeAbundance = minIsotopeAbundance;
    this.minIntensityInPattern = minIntensityInPattern;
    this.resolution = resolution;
    this.storeFormula = storeFormula;
  }

  /**
   * Get all combinatorial chemical isotopes given a structure.
   *
   * @param molFor The IMolecularFormula to start
   * @return A IsotopePattern object containing the different combinations
   */
  public IsotopePattern getIsotopes(IMolecularFormula molFor) {

    if (builder == null) {
      try {
        isoFactory = Isotopes.getInstance();
        builder = molFor.getBuilder();
      } catch (Exception e) {
        logger.log(Level.WARNING, "Unexpected error:", e);
      }
    }
    String mf = MolecularFormulaManipulator.getString(molFor, true);

    IMolecularFormula molecularFormula = MolecularFormulaManipulator.getMajorIsotopeMolecularFormula(
        mf, builder);

    IsotopePattern abundance_Mass = null;

    for (IIsotope isos : molecularFormula.isotopes()) {
      String elementSymbol = isos.getSymbol();
      int atomCount = molecularFormula.getIsotopeCount(isos);

      // Generate possible isotope containers for the current atom's
      // these will then me 'multiplied' with the existing patten
      List<IsotopeContainer> additional = new ArrayList<>();
      for (IIsotope isotope : isoFactory.getIsotopes(elementSymbol)) {
        double mass = isotope.getExactMass();
        double abundance = isotope.getNaturalAbundance();
        if (abundance <= minIsotopeAbundance) {
          continue;
        }
        IsotopeContainer container = new IsotopeContainer(mass, abundance);
        if (storeFormula) {
          container.setFormula(asFormula(isotope));
        }
        additional.add(container);
      }

      for (int i = 0; i < atomCount; i++) {
        abundance_Mass = calculateAbundanceAndMass(abundance_Mass, additional);
      }
    }

    IsotopePattern isoP = IsotopePatternManipulator.sortAndNormalizedByIntensity(abundance_Mass);
    isoP = cleanAbundance(isoP, minIntensityInPattern);
    IsotopePattern isoPattern = IsotopePatternManipulator.sortByMass(isoP);
    return isoPattern;

  }

  private IMolecularFormula asFormula(IIsotope isotope) {
    IMolecularFormula mf = builder.newInstance(IMolecularFormula.class);
    mf.addIsotope(isotope);
    return mf;
  }

  private IMolecularFormula union(IMolecularFormula a, IMolecularFormula b) {
    IMolecularFormula mf = builder.newInstance(IMolecularFormula.class);
    mf.add(a);
    mf.add(b);
    return mf;
  }

  private static IsotopeContainer findExisting(List<IsotopeContainer> containers, double mass,
      double treshhold) {
    for (IsotopeContainer container : containers) {
      if (Math.abs(container.getMass() - mass) <= treshhold) {
        return container;
      }
    }
    return null;
  }

  private void addDistinctFormula(IsotopeContainer container, IMolecularFormula mf) {
    for (IMolecularFormula curr : container.getFormulas()) {
      if (MolecularFormulaManipulator.compare(curr, mf)) {
        return;
      }
    }
    container.addFormula(mf);
  }

  /**
   * Calculates the mass and abundance of all isotopes generated by adding one atom. Receives the
   * periodic table element and calculate the isotopes, if there exist a previous calculation, add
   * these new isotopes. In the process of adding the new isotopes, remove those that has an
   * abundance less than setup parameter minIntensity, and remove duplicated masses.
   *
   * @param additional additional isotopes to 'multiple' the current pattern by
   * @return the calculation was successful
   */
  private IsotopePattern calculateAbundanceAndMass(IsotopePattern current,
      List<IsotopeContainer> additional) {

    if (additional == null || additional.size() == 0) {
      return current;
    }

    List<IsotopeContainer> containers = new ArrayList<>();

    // Verify if there is a previous calculation. If it exists, add the new
    // isotopes
    if (current == null) {
      current = new IsotopePattern();
      for (IsotopeContainer container : additional) {
        current.addIsotope(container);
      }
    } else {
      for (IsotopeContainer container : current.getIsotopes()) {
        for (IsotopeContainer other : additional) {

          double abundance = container.getIntensity() * other.getIntensity() * 0.01;
          double mass = container.getMass() + other.getMass();

          // merge duplicates with some resolution
          IsotopeContainer existing = findExisting(containers, mass, resolution);
          if (existing != null) {
            double newIntensity = existing.getIntensity() + abundance;
            // moving weighted avg
            existing.setMass(
                (existing.getMass() * existing.getIntensity() + mass * abundance) / newIntensity);
            existing.setIntensity(newIntensity);
            if (storeFormula) {
              for (IMolecularFormula mf : container.getFormulas()) {
                addDistinctFormula(existing, union(mf, other.getFormula()));
              }
            }
            continue;
          }

          // Filter isotopes too small
          // intensities may increase during pattern calculation, so don't filter prematurely
          if (abundance > 1E-11) {
            IsotopeContainer newcontainer = new IsotopeContainer(mass, abundance);
            if (storeFormula) {
              for (IMolecularFormula mf : container.getFormulas()) {
                newcontainer.addFormula(union(mf, other.getFormula()));
              }
            }
            containers.add(newcontainer);
          }
        }
      }

      current = new IsotopePattern();
      for (IsotopeContainer container : containers) {
        current.addIsotope(container);
      }
    }
    return current;
  }

  /**
   * Normalize the intensity (relative abundance) of all isotopes in relation of the most abundant
   * isotope.
   *
   * @param isopattern   The IsotopePattern object
   * @param minIntensity The minimum abundance
   * @return The IsotopePattern cleaned
   */
  private IsotopePattern cleanAbundance(IsotopePattern isopattern, double minIntensity) {

    double intensity, biggestIntensity = 0.0f;

    for (IsotopeContainer sc : isopattern.getIsotopes()) {

      intensity = sc.getIntensity();
      if (intensity > biggestIntensity) {
        biggestIntensity = intensity;
      }

    }

    for (IsotopeContainer sc : isopattern.getIsotopes()) {

      intensity = sc.getIntensity();
      intensity /= biggestIntensity;
      if (intensity < 0) {
        intensity = 0;
      }

      sc.setIntensity(intensity);
    }

    IsotopePattern sortedIsoPattern = new IsotopePattern();
    sortedIsoPattern.setMonoIsotope(new IsotopeContainer(isopattern.getIsotopes().get(0)));
    for (int i = 1; i < isopattern.getNumberOfIsotopes(); i++) {
      if (isopattern.getIsotopes().get(i).getIntensity() >= (minIntensity)) {
        IsotopeContainer container = new IsotopeContainer(isopattern.getIsotopes().get(i));
        sortedIsoPattern.addIsotope(container);
      }
    }
    return sortedIsoPattern;
  }

  public static double estimateRequiredAbundance(@NotNull final IMolecularFormula formula) {
    double minAbundance = 0.1;
    try {
      final IsotopeFactory fac = Isotopes.getInstance();

      int numCarbon = 0;
      IIsotope[] carbonIsotopes = fac.getIsotopes(Element.C);
      for (IIsotope carbonIsotope : carbonIsotopes) {
        if (!allowIsotope(carbonIsotope)) {
          continue;
        }
        numCarbon += formula.getIsotopeCount(carbonIsotope);
      }

      if (numCarbon >= 75) {
        // 13C peak of C75H152 is 83%
        minAbundance = 0.01;
      }

      IIsotope[] sIsotopes = fac.getIsotopes(Element.S);
      int numSulfur = 0;
      for (IIsotope sIsotope : sIsotopes) {
        if (!allowIsotope(sIsotope)) {
          continue;
        }
        numSulfur += formula.getIsotopeCount(sIsotope);
      }
      if (numSulfur >= 5) {
        minAbundance = 0.0074;
      }

      int numRelevantElements = numCarbon + numSulfur;

      // only pay attention to N and O if the formula is big enough and there are enough
      // N and O to make a difference
      if (numRelevantElements > 70) {
        IIsotope[] nIsotopes = fac.getIsotopes(Element.N);
        int numNitrogen = 0;
        for (IIsotope nIsotope : nIsotopes) {
          if (!allowIsotope(nIsotope)) {
            continue;
          }
          numNitrogen += formula.getIsotopeCount(nIsotope);
        }
        if (numNitrogen >= 40) {
          minAbundance = 0.0035;
        }
        numRelevantElements += numNitrogen;
      }

      if (numRelevantElements > 110) {
        IIsotope[] oIsotopes = fac.getIsotopes(Element.N);
        int numOxygen = 0;
        for (IIsotope oIsotope : oIsotopes) {
          if (!allowIsotope(oIsotope)) {
            continue;
          }
          numOxygen += formula.getIsotopeCount(oIsotope);
        }
        if (numOxygen >= 40) {
          minAbundance = 0.002;
        }
        numRelevantElements += numOxygen;
      }

    } catch (IOException e) {
      return minAbundance;
    }

    return minAbundance;
  }

  private static boolean allowIsotope(@Nullable IIsotope isotope) {
    if (isotope == null) {
      return false;
    }
    return isotope.getNaturalAbundance() != null && isotope.getNaturalAbundance() > 0.00001;
  }
}