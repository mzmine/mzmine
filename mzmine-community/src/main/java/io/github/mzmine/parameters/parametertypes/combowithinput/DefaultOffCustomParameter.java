/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

import io.github.mzmine.parameters.UserParameter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Abstract base for combo parameters that offer {@link DefaultOffCustomOption} choices and wrap an
 * arbitrary embedded parameter. Concrete subclasses supply the value record type and the embedded
 * parameter type; this class owns the stored {@code defaultValue} and {@code offValue}.
 *
 * @param <V>  the resolved value type (e.g. {@link Double}, {@link Integer})
 */
public class DefaultOffCustomParameter<V> extends
    ComboWithInputParameter<DefaultOffCustomOption, DefaultOffCustomValue<V>, UserParameter<V, ?>> {

  protected final @Nullable V defaultValue;
  protected final @Nullable V offValue;
  protected final boolean includeOff;

  /**
   * @param embeddedParameter the parameter providing the input field (name, description,
   *                          validation, …)
   * @param defaultValue      returned by {@link #resolveValue()} when
   *                          {@link DefaultOffCustomOption#DEFAULT} is selected
   * @param offValue          returned by {@link #resolveValue()} when
   *                          {@link DefaultOffCustomOption#OFF} is selected; may be {@code null}
   */
  public DefaultOffCustomParameter(@NotNull UserParameter<V, ?> embeddedParameter,
      @Nullable V defaultValue, @Nullable V offValue) {
    this(embeddedParameter, defaultValue, offValue, true);
  }

  /**
   * @param embeddedParameter the parameter providing the input field (name, description,
   *                          validation, …)
   * @param defaultValue      returned by {@link #resolveValue()} when
   *                          {@link DefaultOffCustomOption#DEFAULT} is selected; may be
   *                          {@code null}
   * @param offValue          returned by {@link #resolveValue()} when
   *                          {@link DefaultOffCustomOption#OFF} is selected; may be {@code null}
   * @param includeOff        if {@code false} the {@link DefaultOffCustomOption#OFF} entry is
   *                          omitted from the combo-box choices
   */
  public DefaultOffCustomParameter(@NotNull UserParameter<V, ?> embeddedParameter,
      @Nullable V defaultValue, @Nullable V offValue, boolean includeOff) {
    super(embeddedParameter, optionsFor(includeOff), DefaultOffCustomOption.CUSTOM,
        new DefaultOffCustomValue<>(DefaultOffCustomOption.DEFAULT, defaultValue));
    this.defaultValue = defaultValue;
    this.offValue = offValue;
    this.includeOff = includeOff;
  }

  private static DefaultOffCustomOption[] optionsFor(boolean includeOff) {
    return includeOff ? DefaultOffCustomOption.values()
        : new DefaultOffCustomOption[]{DefaultOffCustomOption.DEFAULT,
            DefaultOffCustomOption.CUSTOM};
  }

  /**
   * Resolves the effective value using the option stored in {@link #value} together with the
   * {@link #defaultValue} and {@link #offValue} fields.
   *
   * @return the resolved value; may be {@code null} when {@link DefaultOffCustomOption#OFF} or
   * {@link DefaultOffCustomOption#DEFAULT} is selected and the corresponding value is {@code null}
   */
  @Nullable
  public V resolveValue() {
    return switch (value.getSelectedOption()) {
      case DEFAULT -> defaultValue;
      case OFF -> offValue;
      case CUSTOM -> value.getEmbeddedValue();
    };
  }

  public @Nullable V getDefaultValue() {
    return defaultValue;
  }

  public @Nullable V getOffValue() {
    return offValue;
  }

  @Override
  public DefaultOffCustomValue<V> createValue(@NotNull DefaultOffCustomOption option,
      @NotNull UserParameter<V, ?> embeddedParameter) {
    return new DefaultOffCustomValue<>(option, embeddedParameter.getValue());
  }

  @Override
  public @NotNull DefaultOffCustomParameter<V> cloneParameter() {
    final UserParameter<V, ?> embeddedClone = embeddedParameter.cloneParameter();
    embeddedClone.setValue(value.custom());

    final DefaultOffCustomParameter<V> clone = new DefaultOffCustomParameter<>(embeddedClone,
        defaultValue, offValue, includeOff);
    final DefaultOffCustomValue<V> currentValue = getValue();
    clone.setValue(
        new DefaultOffCustomValue<>(currentValue.getSelectedOption(), currentValue.custom()));

    return clone;
  }
}
