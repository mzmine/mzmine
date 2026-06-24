/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.parameters.parametertypes.statistics;

import io.github.mzmine.modules.dataanalysis.utils.imputation.ImputationFunctions;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import java.util.List;

public class MissingValueImputationParameter extends ComboParameter<ImputationFunctions> {

  public MissingValueImputationParameter(String name, String description,
      ImputationFunctions[] choices) {
    this(name, description, choices, ImputationFunctions.GLOBAL_LIMIT_OF_DETECTION);
  }

  public MissingValueImputationParameter() {
    this(ImputationFunctions.GLOBAL_LIMIT_OF_DETECTION);
  }

  public MissingValueImputationParameter(ImputationFunctions defaultValue) {
    this("Missing value imputation",
        "Missing values will be changed for other small values like an estimated LOD (portion of the smalles value across the dataset) or feature-wise lowest values.",
        ImputationFunctions.valuesExcludeNone, defaultValue);
  }

  public MissingValueImputationParameter(String name, String description,
      ImputationFunctions[] choices, ImputationFunctions defaultValue) {
    super(name, description, choices, defaultValue);
  }

  public MissingValueImputationParameter(String name, String description,
      List<ImputationFunctions> choices, ImputationFunctions defaultValue) {
    super(name, description, choices, defaultValue);
  }

  @Override
  public MissingValueImputationParameter cloneParameter() {
    return new MissingValueImputationParameter(getName(), getDescription(), getChoices(), value);
  }
}
