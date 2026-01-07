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

package io.github.mzmine.modules.dataprocessing.group_metacorrelate.corrgrouping;


import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;

public class AdvancedCorrelateGroupingParameters extends SimpleParameterSet {

  public static final BooleanParameter keepExtendedStats = new BooleanParameter(
      "Keep extended stats", """
      Default conserves memory by removing additional information that is usually saved with every correlation result.
      This information is only needed in some visualization modules that depend on correlation grouping.""",
      false);

  public static final IntegerParameter simplifyLargeDatasets = new IntegerParameter(
      "Simplify for ≥ samples", """
      Simplify some steps for large datasets with ≥samples.
      This will skip the feature overlap function "Min intensity % overlap".""", 250);


  // Constructor
  public AdvancedCorrelateGroupingParameters() {
    super(keepExtendedStats, simplifyLargeDatasets);
  }

}
