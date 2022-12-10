/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.identification.sumformulaprediction;

import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.restrictions.elements.ElementalHeuristicParameters;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.restrictions.rdbe.RDBERestrictionParameters;
import io.github.mzmine.modules.tools.isotopepatternscore.IsotopePatternScoreParameters;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ColorParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.elements.ElementsCompositionRangeParameter;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import javafx.scene.paint.Color;

/**
 */
public class DPPSumFormulaPredictionParameters extends SimpleParameterSet {

  public static final IntegerParameter charge = new IntegerParameter("Charge", "Charge");

  public static final DoubleParameter noiseLevel = new DoubleParameter("Noise level",
      "Minimum intensity of a data point to predict a sum formula for.");

  public static final ComboParameter<IonizationType> ionization =
      new ComboParameter<IonizationType>("Ionization type", "Ionization type",
          IonizationType.values());

  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter();

  public static final ElementsCompositionRangeParameter elements =
      new ElementsCompositionRangeParameter("Elements", "Elements and ranges");

  public static final OptionalModuleParameter<ElementalHeuristicParameters> elementalRatios =
      new OptionalModuleParameter<ElementalHeuristicParameters>("Element count heuristics",
          "Restrict formulas by heuristic restrictions of elemental counts and ratios",
          new ElementalHeuristicParameters());

  public static final OptionalModuleParameter<RDBERestrictionParameters> rdbeRestrictions =
      new OptionalModuleParameter<RDBERestrictionParameters>("RDBE restrictions",
          "Search only for formulas which correspond to the given RDBE restrictions",
          new RDBERestrictionParameters());

  public static final OptionalModuleParameter<IsotopePatternScoreParameters> isotopeFilter =
      new OptionalModuleParameter("Isotope pattern filter",
          "Search only for formulas with a isotope pattern similar",
          new IsotopePatternScoreParameters());

  public static final OptionalParameter<IntegerParameter> displayResults =
      new OptionalParameter<IntegerParameter>(new IntegerParameter("Display results (#)",
          "Check if you want to display the sum formula prediction results in the plot. "
              + "Displaying too much datasets might decrease clarity.\nPlease enter the number "
              + "of predicted sum formulas, you would like to display.",
          1));

  public static final ColorParameter datasetColor = new ColorParameter("Dataset color",
      "Set the color you want the detected isotope patterns to be displayed with.", Color.BLACK);

  public DPPSumFormulaPredictionParameters() {
    super(new Parameter[] {charge, noiseLevel, ionization, mzTolerance, elements, elementalRatios,
        rdbeRestrictions, isotopeFilter, displayResults, datasetColor});
  }
}
