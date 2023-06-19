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

package io.github.mzmine.parameters.parametertypes.ionidentity;


import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;

public class IonLibraryParameterSet extends SimpleParameterSet {

  public static final ComboParameter<String> POSITIVE_MODE = new ComboParameter<>("MS mode",
      "Positive or negative mode?", new String[] {"POSITIVE", "NEGATIVE"}, "POSITIVE");

  public static final IntegerParameter MAX_CHARGE = new IntegerParameter("Maximum charge",
      "Maximum charge to be used for adduct search.", 2, 1, 100);
  public static final IntegerParameter MAX_MOLECULES = new IntegerParameter(
      "Maximum molecules/cluster", "Maximum molecules per cluster (f.e. [2M+Na]+).", 3, 1, 10);

  public static final IonModificationParameter ADDUCTS = new IonModificationParameter("Adducts",
      "List of adducts, each one refers a specific distance in m/z axis between related peaks");

  public IonLibraryParameterSet() {
    super(new Parameter[] {POSITIVE_MODE, MAX_CHARGE, MAX_MOLECULES, ADDUCTS});
  }
}
