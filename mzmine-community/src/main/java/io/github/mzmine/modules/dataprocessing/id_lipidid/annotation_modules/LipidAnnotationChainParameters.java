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

package io.github.mzmine.modules.dataprocessing.id_lipidid.annotation_modules;

import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;

public class LipidAnnotationChainParameters extends SimpleParameterSet {

  public static final IntegerParameter minChainLength = new IntegerParameter("Minimum chain length",
      "Enter the shortest possible chain length.", 12, true, 1, 60);

  public static final IntegerParameter maxChainLength = new IntegerParameter("Maximum chain length",
      "Enter the longest possible chain length.", 26, true, 1, 60);

  public static final IntegerParameter minDBEs = new IntegerParameter("Minimum number of DBEs",
      "Enter the minimum number of double bond equivalents.", 0, true, 0, 30);

  public static final IntegerParameter maxDBEs = new IntegerParameter("Maximum number of DBEs",
      "Enter the maximum number of double bond equivalents.", 6, true, 0, 30);

  public static final BooleanParameter onlySearchForEvenChainLength = new BooleanParameter(
      "Only search for even chain length", "Only search for even chain length.");

  public LipidAnnotationChainParameters() {
    super(minChainLength, maxChainLength, minDBEs, maxDBEs, onlySearchForEvenChainLength);
  }

}
