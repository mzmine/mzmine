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

package io.github.mzmine.modules.tools.id_fraggraph.graphstream;

import io.github.mzmine.util.FormulaUtils;
import java.text.NumberFormat;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public class SubFormulaEdge {

  private final SignalFormulaeModel a;
  private final SignalFormulaeModel b;
  private final String id;

  public SubFormulaEdge(SignalFormulaeModel a, SignalFormulaeModel b, NumberFormat nodeNameFormatter) {
    if (a.getPeakWithFormulae().peak().getMZ() < b.getPeakWithFormulae().peak().getMZ()) {
      this.a = a;
      this.b = b;
    } else {
      this.a = b;
      this.b = a;
    }

    id = STR."\{nodeNameFormatter.format(
        a.getPeakWithFormulae().peak().getMZ())}-\{nodeNameFormatter.format(
        b.getPeakWithFormulae().peak().getMZ())}";
  }

  public SignalFormulaeModel smaller() {
    return a;
  }

  public SignalFormulaeModel larger() {
    return b;
  }

  public double getDeltaMz() {
    return b.getPeakWithFormulae().peak().getMZ() - a.getPeakWithFormulae().peak().getMZ();
  }

  public IMolecularFormula getLossFormula() {
    final IMolecularFormula formula = b.getSelectedFormulaWithMz().formula();
    final IMolecularFormula loss = FormulaUtils.subtractFormula(FormulaUtils.cloneFormula(formula),
        a.getSelectedFormulaWithMz().formula());
    return loss;
  }

  public String getLossFormulaAsString() {
    return MolecularFormulaManipulator.getString(getLossFormula());
  }

  public String getId() {
    return id;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SubFormulaEdge that)) {
      return false;
    }

    if (!a.equals(that.a)) {
      return false;
    }
    return b.equals(that.b);
  }

  @Override
  public int hashCode() {
    int result = a.hashCode();
    result = 31 * result + b.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return STR."SubFormulaEdge{id='\{id}\{'\''}\{'}'}";
  }
}
