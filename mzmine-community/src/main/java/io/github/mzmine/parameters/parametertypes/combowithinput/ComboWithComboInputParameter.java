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

package io.github.mzmine.parameters.parametertypes.combowithinput;

import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import java.util.Collection;
import javafx.collections.ObservableList;

/**
 * A {@link ComboWithInputParameter} whose embedded input is itself a {@link ComboParameter}: a main
 * combo selects an {@link EnumType} option, and a second combo (of {@link EmbeddedType}) is shown
 * only while the selected option is one of the input triggers. The displayed parameter name is the
 * embedded combo's name (see {@link io.github.mzmine.parameters.parametertypes.EmbeddedParameter}).
 */
public class ComboWithComboInputParameter<EnumType extends UniqueIdSupplier, EmbeddedType> extends
    ComboWithInputParameter<EnumType, ComboWithComboInputValue<EnumType, EmbeddedType>, ComboParameter<EmbeddedType>> {

  public ComboWithComboInputParameter(ComboParameter<EmbeddedType> embeddedParameter,
      EnumType[] values, Collection<EnumType> inputTriggers,
      ComboWithComboInputValue<EnumType, EmbeddedType> defaultValue) {
    super(embeddedParameter, values, inputTriggers, defaultValue);
  }

  public ComboWithComboInputParameter(ComboParameter<EmbeddedType> embeddedParameter,
      ObservableList<EnumType> values, Collection<EnumType> inputTriggers,
      ComboWithComboInputValue<EnumType, EmbeddedType> defaultValue) {
    super(embeddedParameter, values, inputTriggers, defaultValue);
  }

  @Override
  public ComboWithComboInputValue<EnumType, EmbeddedType> createValue(EnumType option,
      ComboParameter<EmbeddedType> embeddedParameter) {
    return new ComboWithComboInputValue<>(option, embeddedParameter.getValue());
  }

  @Override
  public ComboWithComboInputParameter<EnumType, EmbeddedType> cloneParameter() {
    final ComboParameter<EmbeddedType> embeddedClone = embeddedParameter.cloneParameter();
    final ComboWithComboInputValue<EnumType, EmbeddedType> clonedValue = new ComboWithComboInputValue<>(
        value.getSelectedOption(), value.getEmbeddedValue());
    final ComboWithComboInputParameter<EnumType, EmbeddedType> clone = new ComboWithComboInputParameter<>(
        embeddedClone, choices, inputTriggers, clonedValue);
    clone.setValue(clonedValue);
    return clone;
  }
}
