/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.id_onlinecompounddb;

import io.github.mzmine.modules.tools.isotopepatternscore.IsotopePatternScoreParameters;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.NeutralMassParameter;
import io.github.mzmine.parameters.parametertypes.submodules.ModuleOptionsEnumComboParameter;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;

/**
 * @deprecated because of old API usage. Hard to maintain. This was removed from the interfaces and
 * is only here as reference point
 */
@Deprecated
public class SingleRowIdentificationParameters extends SimpleParameterSet {

  public static final ModuleOptionsEnumComboParameter<OnlineDatabases> DATABASE = new ModuleOptionsEnumComboParameter<>(
      "Database", "Database to search", OnlineDatabases.PubChem);

  public static final NeutralMassParameter NEUTRAL_MASS = new NeutralMassParameter("Neutral mass",
      "Value to use in the search query");

  // Max count of 10,000 is enforced by ChemSpider API
  public static final IntegerParameter MAX_RESULTS = new IntegerParameter("Number of results",
      "Maximum number of results to display", 20, 1, 10000);

  public static final MZToleranceParameter MZ_TOLERANCE = new MZToleranceParameter();

  public static final OptionalModuleParameter<IsotopePatternScoreParameters> ISOTOPE_FILTER = new OptionalModuleParameter<>(
      "Isotope pattern filter", "Search only for compounds with a isotope pattern similar",
      new IsotopePatternScoreParameters());

  public SingleRowIdentificationParameters() {
    super(DATABASE, NEUTRAL_MASS, MAX_RESULTS, MZ_TOLERANCE, ISOTOPE_FILTER);
  }
}
