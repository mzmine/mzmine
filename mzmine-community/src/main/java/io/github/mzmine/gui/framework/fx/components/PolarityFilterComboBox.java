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

package io.github.mzmine.gui.framework.fx.components;

import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.parameters.parametertypes.ComboComponent;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.StringConverter;

public class PolarityFilterComboBox extends ComboComponent<PolarityType> {

  public PolarityFilterComboBox(boolean includeAny) {
    this(FXCollections.observableList(
            includeAny ? List.of(PolarityType.ANY, PolarityType.POSITIVE, PolarityType.NEGATIVE)
                : List.of(PolarityType.POSITIVE, PolarityType.NEGATIVE)),
        includeAny ? PolarityType.ANY : PolarityType.POSITIVE);

  }

  public PolarityFilterComboBox(final ObservableList<PolarityType> choices,
      final PolarityType value) {
    super(choices);
    setValue(value);

    setConverter(new StringConverter<>() {
      @Override
      public String toString(final PolarityType o) {
        return o.toLabel();
      }

      @Override
      public PolarityType fromString(final String polarity) {
        return PolarityType.parseFromString(polarity);
      }
    });
  }
}
