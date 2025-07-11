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

package io.github.mzmine.parameters.parametertypes.ionidentity;

import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.gui.framework.fx.components.PolarityFilterComboBox;
import io.github.mzmine.parameters.parametertypes.ComboComponent;
import io.github.mzmine.parameters.parametertypes.ComboParameter;

/**
 * Polarity combobox with options to set No filter or positive / negative
 */
public class PolarityFilterParameter extends ComboParameter<PolarityType> {

  public PolarityFilterParameter(final String name, final String description,
      final PolarityType[] choices, PolarityType value) {
    super(name, description, choices, value);
  }

  public PolarityFilterParameter(final PolarityType[] choices, PolarityType value) {
    super("Polarity", "Filters the polarity (ion mode)", choices, value);
  }

  public PolarityFilterParameter(final boolean includeAnyPolarity) {
    this("Polarity", "Filters the polarity (ion mode)", includeAnyPolarity);
  }

  public PolarityFilterParameter(final String name, final String description,
      final boolean includeAnyPolarity) {
    this(name, description,
        includeAnyPolarity ? new PolarityType[]{PolarityType.ANY, PolarityType.POSITIVE,
            PolarityType.NEGATIVE}
            : new PolarityType[]{PolarityType.POSITIVE, PolarityType.NEGATIVE},
        includeAnyPolarity ? PolarityType.ANY : PolarityType.POSITIVE);
  }

  @Override
  public ComboComponent<PolarityType> createEditingComponent() {
    return new PolarityFilterComboBox(getChoices(), getValue());
  }

  @Override
  public PolarityFilterParameter cloneParameter() {
    return new PolarityFilterParameter(getName(), getDescription(),
        getChoices().toArray(PolarityType[]::new), getValue());
  }
}
