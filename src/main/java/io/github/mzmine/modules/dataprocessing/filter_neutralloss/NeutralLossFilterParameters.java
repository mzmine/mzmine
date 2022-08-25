/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.dataprocessing.filter_neutralloss;

import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.Locale;
import org.openscience.cdk.interfaces.IMolecularFormula;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.colorpalette.ColorPaletteParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;
import io.github.mzmine.util.FormulaUtils;

public class NeutralLossFilterParameters extends SimpleParameterSet {

  public static final FeatureListsParameter PEAK_LISTS = new FeatureListsParameter();

  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter();

  public static final BooleanParameter checkRT =
      new BooleanParameter("Check RT", "Compare rt of peaks to parent.");

  public static final RTToleranceParameter rtTolerance = new RTToleranceParameter();

  public static final DoubleParameter minHeight = new DoubleParameter("Minimum height",
      "Minimum peak height to be considered as an isotope peak.",
      NumberFormat.getNumberInstance(Locale.ENGLISH), 0.0);

  public static final DoubleParameter neutralLoss = new DoubleParameter("Neutral loss (m/z)",
      "Enter exact mass of neutral loss.", NumberFormat.getNumberInstance(Locale.ENGLISH), 0.0);

  public static final StringParameter molecule = new StringParameter("Molecule",
      "String of a neutral loss compound (e.g. HI). If this textbox is not empty, the \"Neutral loss\" field will be ignored.",
      "");

  public static final StringParameter suffix = new StringParameter("Name suffix",
      "Suffix to be added to feature list name. If \"auto\" then this module will create a suffix.",
      "auto");

  public NeutralLossFilterParameters() {
    super(new Parameter[] {PEAK_LISTS, mzTolerance, checkRT, rtTolerance, minHeight, neutralLoss,
        molecule, suffix},
        "https://mzmine.github.io/mzmine_documentation/module_docs/filter_neutral_loss/filter_neutral_loss.html");
  }

  @Override
  public boolean checkParameterValues(Collection<String> errorMessages) {
    if (super.checkParameterValues(errorMessages) == false)
      return false;

    String molecule = this.getParameter(NeutralLossFilterParameters.molecule).getValue();

    // no formula -> use deltaMass
    if (molecule.isEmpty())
      return true;
    // formula can be parsed
    IMolecularFormula formula = FormulaUtils.createMajorIsotopeMolFormula(molecule);
    if (formula != null) {
      return true;
    }
    errorMessages.add("Formula cannot be parsed. Enter valid formula.");
    return false;
  }
}
