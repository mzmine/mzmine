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

package io.github.mzmine.modules.visualization.vankrevelendiagram;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.util.ExitCode;

/*
 * Van Krevelen diagram class
 *
 * @author Ansgar Korf (ansgar.korf@uni-muenster)
 */
public class VanKrevelenDiagramParameters extends SimpleParameterSet {

  public static final FeatureListsParameter featureList = new FeatureListsParameter(1, 1);

  public static final ComboParameter<VanKrevelenDiagramDataTypes> colorScaleValues = new ComboParameter<>(
      "Color scale", "Select a parameter to be plotted as color scale",
      VanKrevelenDiagramDataTypes.values(), VanKrevelenDiagramDataTypes.RETENTION_TIME);

  public static final ComboParameter<VanKrevelenDiagramDataTypes> bubbleSizeValues = new ComboParameter<>(
      "Bubble size", "Select a parameter to be plotted as bubble size",
      VanKrevelenDiagramDataTypes.values(), VanKrevelenDiagramDataTypes.INTENSITY);

  public VanKrevelenDiagramParameters() {
    super(new Parameter[]{featureList, colorScaleValues, bubbleSizeValues},
        "");
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    return super.showSetupDialog(valueCheckRequired);
  }

  @Override
  public int getVersion() {
    return 2;
  }

}
