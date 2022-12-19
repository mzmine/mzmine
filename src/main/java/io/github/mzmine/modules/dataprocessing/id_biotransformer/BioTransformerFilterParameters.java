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

package io.github.mzmine.modules.dataprocessing.id_biotransformer;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;

public class BioTransformerFilterParameters extends SimpleParameterSet {

  public static final BooleanParameter eductMustHaveMsMs = new BooleanParameter("Educt must have MS/MS",
      "Transformation products will only be predicted for educts with MS/MS.", false);

  public static final OptionalParameter<DoubleParameter> minEductHeight = new OptionalParameter<>(
      new DoubleParameter("Minimum Educt intensity",
          "Products will only be predicted for educts above this intensity.",
          MZmineCore.getConfiguration().getIntensityFormat(), 1E4), false);

  public static final BooleanParameter productMustHaveMsMs = new BooleanParameter(
      "Product must have MS/MS",
      "Transformation products will only be assigned to products with an MS/MS spectrum.", false);

  public static final OptionalParameter<DoubleParameter> minProductHeight = new OptionalParameter<>(
      new DoubleParameter("Minimum Product intensity",
          "Products will only be assigned to products above this intensity.",
          MZmineCore.getConfiguration().getIntensityFormat(), 1E4), false);

  public BioTransformerFilterParameters() {
    super(new Parameter[] {eductMustHaveMsMs, minEductHeight, productMustHaveMsMs, minProductHeight});
  }
}
