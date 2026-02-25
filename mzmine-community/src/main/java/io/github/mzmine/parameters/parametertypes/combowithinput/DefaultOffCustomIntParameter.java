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

import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A combo parameter with {@link DefaultOffCustomOption} options and an integer input field active
 * when {@link DefaultOffCustomOption#CUSTOM} is selected.
 * <p>
 * Both the {@code defaultValue} (returned for {@link DefaultOffCustomOption#DEFAULT}) and the
 * {@code offValue} (returned for {@link DefaultOffCustomOption#OFF}) are stored in the parameter
 * itself, so {@link #resolve()} requires no arguments.
 */
public class DefaultOffCustomIntParameter extends
    DefaultOffCustomParameter<Integer, IntegerParameter, DefaultOffCustomIntValue> {

  /**
   * @param embeddedParameter the integer parameter providing name, description, and min/max/format
   * @param defaultValue      the value returned when {@link DefaultOffCustomOption#DEFAULT} is
   *                          selected
   * @param offValue          the value returned when {@link DefaultOffCustomOption#OFF} is selected
   */
  public DefaultOffCustomIntParameter(@NotNull IntegerParameter embeddedParameter, int defaultValue,
      int offValue) {
    this(embeddedParameter, defaultValue, offValue, true);
  }

  /**
   * @param embeddedParameter the integer parameter providing name, description, and min/max/format
   * @param defaultValue      the value returned when {@link DefaultOffCustomOption#DEFAULT} is
   *                          selected
   * @param offValue          the value returned when {@link DefaultOffCustomOption#OFF} is selected;
   *                          may be {@code null}
   * @param includeOff        if {@code false} the {@link DefaultOffCustomOption#OFF} entry is
   *                          omitted from the combo-box choices
   */
  public DefaultOffCustomIntParameter(@NotNull IntegerParameter embeddedParameter, int defaultValue,
      @Nullable Integer offValue, boolean includeOff) {
    super(embeddedParameter, defaultValue, offValue,
        new DefaultOffCustomIntValue(DefaultOffCustomOption.DEFAULT,
            embeddedParameter.getValue() != null ? embeddedParameter.getValue() : defaultValue),
        includeOff);
  }

  @Override
  public @NotNull DefaultOffCustomIntValue createValue(@NotNull DefaultOffCustomOption option,
      @NotNull IntegerParameter embeddedParameter) {
    return new DefaultOffCustomIntValue(option, embeddedParameter.getValue());
  }

  @Override
  public @NotNull DefaultOffCustomIntParameter cloneParameter() {
    final IntegerParameter embeddedClone = embeddedParameter.cloneParameter();
    embeddedClone.setValue(value.custom());
    final DefaultOffCustomIntParameter clone = new DefaultOffCustomIntParameter(embeddedClone,
        defaultValue, offValue, includeOff);
    clone.setValue(new DefaultOffCustomIntValue(value.option(), value.custom()));
    return clone;
  }
}
