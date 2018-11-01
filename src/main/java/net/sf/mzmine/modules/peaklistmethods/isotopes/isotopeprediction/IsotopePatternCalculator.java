/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package net.sf.mzmine.modules.peaklistmethods.isotopes.isotopeprediction;

import java.awt.Window;
import java.util.ArrayList;
import javax.annotation.Nonnull;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.IsotopePattern;
import net.sf.mzmine.datamodel.IsotopePattern.IsotopePatternStatus;
import net.sf.mzmine.datamodel.PolarityType;
import net.sf.mzmine.datamodel.impl.ExtendedIsotopePattern;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;
import net.sf.mzmine.datamodel.impl.SimpleIsotopePattern;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.util.ExitCode;
import org.openscience.cdk.formula.IsotopeContainer;
import org.openscience.cdk.formula.IsotopePatternGenerator;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

/**
 * The reason why we introduce this as a module, rather than simple utility class, is to remember
 * the parameter values.
 */
public class IsotopePatternCalculator implements MZmineModule {

  private static final double ELECTRON_MASS = 5.4857990943E-4;

  private static final String MODULE_NAME = "Isotope pattern prediction.";

  @Override
  public @Nonnull String getName() {
    return MODULE_NAME;
  }

  public static IsotopePattern calculateIsotopePattern(String molecularFormula, double minAbundance,
      int charge, PolarityType polarity) {
    return calculateIsotopePattern(molecularFormula, minAbundance, charge, polarity, false);
  }

  public static IsotopePattern calculateIsotopePattern(String molecularFormula, double minAbundance,
      int charge, PolarityType polarity, boolean storeFormula) {

    return calculateIsotopePattern(molecularFormula, minAbundance, 0.00005f, charge, polarity,
        storeFormula);
  }

  public static IsotopePattern calculateIsotopePattern(String molecularFormula, double minAbundance,
      double mergeWidth, int charge, PolarityType polarity, boolean storeFormula) {

    IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();

    IMolecularFormula cdkFormula =
        MolecularFormulaManipulator.getMolecularFormula(molecularFormula, builder);

    return calculateIsotopePattern(cdkFormula, minAbundance, mergeWidth, charge, polarity,
        storeFormula);
  }

  public static IsotopePattern calculateIsotopePattern(IMolecularFormula cdkFormula,
      double minAbundance, int charge, PolarityType polarity) {
    return calculateIsotopePattern(cdkFormula, minAbundance, charge, polarity, false);
  }

  public static IsotopePattern calculateIsotopePattern(IMolecularFormula cdkFormula,
      double minAbundance, int charge, PolarityType polarity, boolean storeFormula) {
    return calculateIsotopePattern(cdkFormula, minAbundance, 0.00005f, charge, polarity, false);
  }

  public static IsotopePattern calculateIsotopePattern(IMolecularFormula cdkFormula,
      double minAbundance, double mergeWidth, int charge, PolarityType polarity,
      boolean storeFormula) {

    // TODO: check if the formula is not too big (>100 of a single atom?).
    // if so, just cancel the prediction

    // Set the minimum abundance of isotope
    // TODO: in the CDK minAbundance is now called minIntensity and refers to the relative intensity
    // in the isotope pattern, should change it here, too
    IsotopePatternGenerator generator = new IsotopePatternGenerator(minAbundance);
    generator.setMinResolution(mergeWidth);
    generator.setStoreFormulas(storeFormula);

    org.openscience.cdk.formula.IsotopePattern pattern = generator.getIsotopes(cdkFormula);

    int numOfIsotopes = pattern.getNumberOfIsotopes();

    DataPoint dataPoints[] = new DataPoint[numOfIsotopes];
    String isotopeComposition[] = new String[numOfIsotopes];

    for (int i = 0; i < numOfIsotopes; i++) {
      IsotopeContainer isotope = pattern.getIsotope(i);

      // For each unit of charge, we have to add or remove a mass of a
      // single electron. If the charge is positive, we remove electron
      // mass. If the charge is negative, we add it.
      double mass = isotope.getMass() + (polarity.getSign() * -1 * charge * ELECTRON_MASS);

      if (charge != 0)
        mass /= charge;

      double intensity = isotope.getIntensity();

      dataPoints[i] = new SimpleDataPoint(mass, intensity);

      if (storeFormula)
        isotopeComposition[i] = formatCDKString(isotope.toString());
    }

    String formulaString = MolecularFormulaManipulator.getString(cdkFormula);


    if (storeFormula)
      return new ExtendedIsotopePattern(dataPoints, IsotopePatternStatus.PREDICTED,
          formulaString, isotopeComposition);
    else
      return new SimpleIsotopePattern(dataPoints, IsotopePatternStatus.PREDICTED, formulaString);
  }

  /**
   * Returns same isotope pattern (same ratios between isotope intensities) with maximum intensity
   * normalized to 1
   */
  public static IsotopePattern normalizeIsotopePattern(IsotopePattern pattern) {
    return normalizeIsotopePattern(pattern, 1);
  }

  /**
   * Returns same isotope pattern (same ratios between isotope intensities) with maximum intensity
   * normalized to given intensity
   */
  public static IsotopePattern normalizeIsotopePattern(IsotopePattern pattern,
      double normalizedValue) {

    DataPoint highestIsotope = pattern.getHighestDataPoint();
    DataPoint dataPoints[] = pattern.getDataPoints();

    double maxIntensity = highestIsotope.getIntensity();

    DataPoint newDataPoints[] = new DataPoint[dataPoints.length];

    for (int i = 0; i < dataPoints.length; i++) {

      double mz = dataPoints[i].getMZ();
      double intensity = dataPoints[i].getIntensity() / maxIntensity * normalizedValue;

      newDataPoints[i] = new SimpleDataPoint(mz, intensity);
    }

    SimpleIsotopePattern newPattern;

    if (pattern instanceof ExtendedIsotopePattern
        && ((ExtendedIsotopePattern) pattern).getIsotopeCompositions() != null)
      return new ExtendedIsotopePattern(newDataPoints, pattern.getStatus(),
          pattern.getDescription(), ((ExtendedIsotopePattern) pattern).getIsotopeCompositions());
    else
      return new SimpleIsotopePattern(newDataPoints, pattern.getStatus(), pattern.getDescription());


  }

  /**
   * Merges the isotopes falling within the given m/z tolerance. If the m/z difference between the
   * isotopes is smaller than mzTolerance, their intensity is added together and new m/z value is
   * calculated as a weighted average.
   */
  public static IsotopePattern mergeIsotopes(IsotopePattern pattern, double mzTolerance) {

    DataPoint dataPoints[] = pattern.getDataPoints().clone();

    String newIsotopeComposition[] = new String[pattern.getNumberOfDataPoints()];
    if (pattern instanceof ExtendedIsotopePattern
        && ((ExtendedIsotopePattern) pattern).getIsotopeCompositions() != null)
      newIsotopeComposition = ((ExtendedIsotopePattern) pattern).getIsotopeCompositions();

    for (int i = 0; i < dataPoints.length - 1; i++) {

      if (Math.abs(dataPoints[i].getMZ() - dataPoints[i + 1].getMZ()) < mzTolerance) {
        double newIntensity = dataPoints[i].getIntensity() + dataPoints[i + 1].getIntensity();
        double newMZ = (dataPoints[i].getMZ() * dataPoints[i].getIntensity()
            + dataPoints[i + 1].getMZ() * dataPoints[i + 1].getIntensity()) / newIntensity;
        dataPoints[i + 1] = new SimpleDataPoint(newMZ, newIntensity);
        dataPoints[i] = null;

        if (pattern instanceof ExtendedIsotopePattern
            && ((ExtendedIsotopePattern) pattern).getIsotopeCompositions() != null) {
          newIsotopeComposition[i + 1] = ((ExtendedIsotopePattern) pattern).getIsotopeComposition(i)
              + ", " + ((ExtendedIsotopePattern) pattern).getIsotopeComposition(i + 1);
          newIsotopeComposition[i] = null;

          // System.out.println("merged isotopes: " + i + " + " + (i+1));
        }
      }
    }

    ArrayList<DataPoint> newDataPoints = new ArrayList<DataPoint>();
    for (DataPoint dp : dataPoints) {
      if (dp != null)
        newDataPoints.add(dp);
    }

    if (pattern instanceof ExtendedIsotopePattern
        && ((ExtendedIsotopePattern) pattern).getIsotopeCompositions() != null) {
      ArrayList<String> newComp = new ArrayList<String>();
      for (String comp : newIsotopeComposition) {
        if (comp != null)
          newComp.add(comp);
      }
      return new ExtendedIsotopePattern(newDataPoints.toArray(new DataPoint[0]), pattern.getStatus(),
          pattern.getDescription(), newComp.toArray(new String[0]));
    }

    return new SimpleIsotopePattern(
        newDataPoints.toArray(new DataPoint[0]), pattern.getStatus(), pattern.getDescription());

  }

  public static IsotopePattern showIsotopePredictionDialog(Window parent,
      boolean valueCheckRequired) {

    ParameterSet parameters =
        MZmineCore.getConfiguration().getModuleParameters(IsotopePatternCalculator.class);
    ExitCode exitCode = parameters.showSetupDialog(parent, valueCheckRequired);
    if (exitCode != ExitCode.OK)
      return null;

    String formula = parameters.getParameter(IsotopePatternCalculatorParameters.formula).getValue();
    int charge = parameters.getParameter(IsotopePatternCalculatorParameters.charge).getValue();
    PolarityType polarity =
        parameters.getParameter(IsotopePatternCalculatorParameters.polarity).getValue();
    double minAbundance =
        parameters.getParameter(IsotopePatternCalculatorParameters.minAbundance).getValue();

    try {
      IsotopePattern predictedPattern =
          calculateIsotopePattern(formula, minAbundance, charge, polarity);
      return predictedPattern;
    } catch (Exception e) {
      MZmineCore.getDesktop().displayException(MZmineCore.getDesktop().getMainWindow(), e);
    }

    return null;

  }

  @Override
  public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
    return IsotopePatternCalculatorParameters.class;
  }

  static String formatCDKString(String cdkString) {
    int startIndex = cdkString.lastIndexOf("MF=");
    int endIndex = cdkString.length() - 1;

    return cdkString.substring(startIndex + 3, endIndex);
  }
}
