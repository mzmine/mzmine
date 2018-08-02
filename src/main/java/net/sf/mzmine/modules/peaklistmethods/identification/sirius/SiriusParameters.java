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

package net.sf.mzmine.modules.peaklistmethods.identification.sirius;

import java.awt.Window;
import net.sf.mzmine.datamodel.IonizationType;
import net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.elements.ElementsParameter;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.dialogs.ParameterSetupDialog;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;

import java.io.IOException;
import java.text.DecimalFormat;

import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import net.sf.mzmine.util.ExitCode;
import org.openscience.cdk.config.IsotopeFactory;
import org.openscience.cdk.config.Isotopes;
import org.openscience.cdk.formula.MolecularFormulaRange;

public abstract class SiriusParameters extends SimpleParameterSet {
  private static final int ISOTOPE_MAX = 100;
  private static final int ISOTOPE_MIN = 0;

 public static final MZToleranceParameter MZ_TOLERANCE = new MZToleranceParameter();

  public static final ComboParameter<IonizationType> ionizationType = new ComboParameter<IonizationType>(
      "Ionization type", "Ionization type", IonizationType.values());

  public static final ElementsParameter ELEMENTS = new ElementsParameter(
      "Elements", "Elements and ranges");

  public static final IntegerParameter SIRIUS_TIMEOUT =
      new IntegerParameter(
          "Timer for Sirius task", "Specify the amount of seconds, during which Sirius should finish processing of the row",
          10
      );

  public ExitCode showSetupDialog(Window parent, boolean valueCheckRequired) {
    String message = "<html>SIRIUS Module Disclaimer:" +
        "<br>    - If you use the SIRIUS export module, cite <a href=\"https://bmcbioinformatics.biomedcentral.com/articles/10.1186/1471-2105-11-395\">MZmine2 paper</a> and the following articles: " +
        "<br>     <a href=\"http://www.pnas.org/content/112/41/12580.abstract\">Dührkop et al., Proc Natl Acad Sci USA 112(41):12580-12585</a> "
        + "and <a href=\"https://jcheminf.springeropen.com/articles/10.1186/s13321-016-0116-8\">Boecker et al., Journal of Cheminformatics (2016) 8:5</a>" +
        "<br>    - Sirius can be downloaded at the following address: <a href=\"https://bio.informatik.uni-jena.de/software/sirius/\">https://bio.informatik.uni-jena.de/software/sirius/</a>" +
        "<br>    - Sirius results can be mapped into <a href=\"http://gnps.ucsd.edu/\">GNPS</a> molecular networks. <a href=\"https://bix-lab.ucsd.edu/display/Public/Mass+spectrometry+data+pre-processing+for+GNPS\">See the documentation</a>.";
    ParameterSetupDialog dialog = new ParameterSetupDialog(parent, valueCheckRequired, this, message);
    dialog.setVisible(true);
    return dialog.getExitCode();
  }

  static {
    ELEMENTS.setValue(createDefaultElements());
  }

  public SiriusParameters(Parameter[] params) {
    super(params);
  }

  /**
   * Create default table of elements
   * The table is used later to calculate FormulaConstraints
   * @return
   */
  private static MolecularFormulaRange createDefaultElements() {
    MolecularFormulaRange range = new MolecularFormulaRange();
    try {
      IsotopeFactory iFac = Isotopes.getInstance();
      range.addIsotope(iFac.getMajorIsotope("C"), ISOTOPE_MIN, ISOTOPE_MAX);
      range.addIsotope(iFac.getMajorIsotope("H"), ISOTOPE_MIN, ISOTOPE_MAX);
      range.addIsotope(iFac.getMajorIsotope("N"), ISOTOPE_MIN, ISOTOPE_MAX);
      range.addIsotope(iFac.getMajorIsotope("O"), ISOTOPE_MIN, ISOTOPE_MAX);
      range.addIsotope(iFac.getMajorIsotope("P"), ISOTOPE_MIN, ISOTOPE_MAX);
      range.addIsotope(iFac.getMajorIsotope("S"), ISOTOPE_MIN, ISOTOPE_MAX);
      range.addIsotope(iFac.getMajorIsotope("F"), ISOTOPE_MIN, ISOTOPE_MIN);
      range.addIsotope(iFac.getMajorIsotope("B"), ISOTOPE_MIN, ISOTOPE_MIN);
      range.addIsotope(iFac.getMajorIsotope("I"), ISOTOPE_MIN, ISOTOPE_MIN);
      range.addIsotope(iFac.getMajorIsotope("Br"), ISOTOPE_MIN, ISOTOPE_MIN);
      range.addIsotope(iFac.getMajorIsotope("Se"), ISOTOPE_MIN, ISOTOPE_MIN);
      range.addIsotope(iFac.getMajorIsotope("Cl"), ISOTOPE_MIN, ISOTOPE_MIN);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return range;
  }
}
