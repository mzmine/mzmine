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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.parameters.parametertypes.combowithinput;

import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.combowithinput.IntOrAutoValue.IntOrAuto;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class ComboWithIntOrAutoParameter extends
    ComboWithInputParameter<IntOrAuto, IntOrAutoValue, IntegerParameter> {

  public ComboWithIntOrAutoParameter(IntegerParameter embeddedParameter) {
    super(embeddedParameter, IntOrAuto.values(), IntOrAuto.MANUAL,
        new IntOrAutoValue(IntOrAuto.AUTO, 5));
  }

  @Override
  public IntOrAutoValue createValue(IntOrAuto option, IntegerParameter embeddedParameter) {
    return new IntOrAutoValue(option, embeddedParameter.getValue());
  }

  @Override
  public ComboWithIntOrAutoParameter cloneParameter() {
    final IntegerParameter embeddedClone = embeddedParameter.cloneParameter();
    embeddedClone.setValue(value.manual());
    final ComboWithIntOrAutoParameter clone = new ComboWithIntOrAutoParameter(
        embeddedClone);
    clone.setValue(new IntOrAutoValue(value.value(), value.manual()));
    return clone;
  }
}
