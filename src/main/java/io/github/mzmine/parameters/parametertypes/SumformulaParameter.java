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

package io.github.mzmine.parameters.parametertypes;


import io.github.msdk.MSDKRuntimeException;
import java.util.Collection;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

/**
 * Automatically validates formula
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class SumformulaParameter extends StringParameter {

  public static final double ELECTRON_MASS = 5.4857990943E-4;
  private final IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();

  public SumformulaParameter(String name, String description) {
    this(name, description, null);
  }

  public SumformulaParameter(String name, String description, int inputsize) {
    super(name, description, inputsize);
  }

  public SumformulaParameter(String name, String description, String defaultValue) {
    super(name, description, defaultValue);
  }

  public SumformulaParameter(String name, String description, String defaultValue,
      boolean valueRequired) {
    super(name, description, defaultValue, valueRequired);
  }

  @Override
  public String getValue() {
    return getNameWithoutCharge();
  }

  public String getNameWithoutCharge() {
    if (value == null || value.isEmpty()) {
      return "";
    }

    int l = Math.max(value.lastIndexOf('+'), value.lastIndexOf('-'));
    if (l == -1) {
      return value;
    } else {
      return value.substring(0, l);
    }
  }

  /**
   * Full name with charge
   *
   */
  public String getFullName() {
    return name;
  }


  public int getCharge() {
    if (value == null || value.isEmpty()) {
      return 0;
    }
    // cutoff first -
    String value = this.value.substring(1);
    int l = Math.max(value.lastIndexOf('+'), value.lastIndexOf('-'));
    if (l == -1) {
      return 0;
    } else {
      try {
        int multiplier = value.charAt(l) == '+' ? 1 : -1;
        String s = value.substring(l + 1);
        // only sign? then charge 1
        if (s.isEmpty()) {
          s += "1";
        }

        return multiplier * Integer.parseInt(s);
      } catch (Exception e) {
        throw new IllegalArgumentException("Could not set up formula. Invalid input.");
      }
    }
  }

  /**
   * Monoisotopic mass of sum formula (not mass to charge!). Mass of electrons is subtracted/added
   *
   */
  public double getMonoisotopicMass() {
    if (value != null && !value.isEmpty()) {
        double mz = MolecularFormulaManipulator.getMass(getFormula(), MolecularFormulaManipulator.MonoIsotopic);
        mz -= getCharge() * ELECTRON_MASS;
        if (value.startsWith("-")) {
          mz = -mz;
        }
        return mz;
    } else if (valueRequired) {
      throw new IllegalArgumentException("Could not set up formula. Invalid input.");
    }
    return 0;
  }

  public IMolecularFormula getFormula() {
    if (value != null && !value.isEmpty()) {
      try {
        String formString = this.value;
        // cutoff first - (negative mz)
        if (formString.startsWith("-")) {
          formString = formString.substring(1);
        }

        //
        int l = Math.max(formString.lastIndexOf('+'), formString.lastIndexOf('-'));
        if (l == -1) {
          return MolecularFormulaManipulator.getMajorIsotopeMolecularFormula(formString, builder);
        } else {
          String f = formString.substring(0, l);
          String charge = formString.substring(l);
          return MolecularFormulaManipulator.getMajorIsotopeMolecularFormula("[" + f + "]" + charge,
              builder);
        }
      } catch (Exception e) {
        throw new MSDKRuntimeException("Could not set up formula. Invalid input.");
      }
    } else if (valueRequired) {
      throw new MSDKRuntimeException("Could not set up formula. Invalid input.");
    }
    return null;
  }

  public boolean checkValue() {
    return checkValue(null);
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    if (!valueRequired) {
      return true;
    }
    if ((value == null) || (value.trim().isEmpty())) {
      if (errorMessages != null) {
        errorMessages.add(name + " is not set properly");
      }
      return false;
    }
    try {
      getMonoisotopicMass();
      return true;
    } catch (Exception e) {
      return false;
    }
  }

}
