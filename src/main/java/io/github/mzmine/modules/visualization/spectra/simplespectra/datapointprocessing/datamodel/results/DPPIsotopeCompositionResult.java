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

package io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.results;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Used to keep track of the isotope composition of a peak. This result type contains an ArrayList
 * of Strings. A ProcessedDataPoint should only contain one result of this type. If there is more
 * than one isotope involved, they should all be added to this list.
 * 
 * @author SteffenHeu steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 *
 */
public class DPPIsotopeCompositionResult extends DPPResult<List<String>> {

  public DPPIsotopeCompositionResult(List<String> value) {
    super(value);
  }

  public DPPIsotopeCompositionResult(String value) {
    super(new ArrayList<String>());

    if (value != null && !value.equals("") && !value.equals(" "))
      getValue().add(value);
  }

  public void add(String value) {
    if (value == null || value.equals("") || value.equals(" "))
      return;
    this.getValue().add(value);
  }

  public void addAll(Collection<String> values) {
    for (String value : values)
      add(value);
  }

  @Override
  public String toString() {
    String label = "";
    for (String s : value)
      label += s + ", ";
    label = label.substring(0, label.length() - 2);
    return label;
  }

  @Override
  public ResultType getResultType() {
    return ResultType.ISOTOPECOMPOSITION;
  }

}
