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

import io.github.mzmine.parameters.UserParameter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Abstract base for combo parameters that offer {@link DefaultOffCustomOption} choices and wrap an
 * arbitrary embedded parameter. Concrete subclasses supply the value record type and the embedded
 * parameter type; this class owns the stored {@code defaultValue} and {@code offValue} so they do
 * not have to be repeated at every call site.
 *
 * @param <V>   the resolved value type (e.g. {@link Double}, {@link Integer})
 * @param <EP>  the type of the embedded {@link UserParameter} (e.g. {@code DoubleParameter})
 * @param <Val> the {@link ComboWithInputValue} record that pairs an option with the custom input
 */
public abstract class DefaultOffCustomParameter<V, EP extends UserParameter<V, ?>, Val extends ComboWithInputValue<DefaultOffCustomOption, V>> extends
    ComboWithInputParameter<DefaultOffCustomOption, Val, EP> {

  protected final @NotNull V defaultValue;
  protected final @Nullable V offValue;
  protected final boolean includeOff;

  /**
   * @param embeddedParameter the parameter providing the input field (name, description,
   *                          validation, …)
   * @param defaultValue      returned by {@link #resolve()} when
   *                          {@link DefaultOffCustomOption#DEFAULT} is selected
   * @param offValue          returned by {@link #resolve()} when {@link DefaultOffCustomOption#OFF}
   *                          is selected; may be {@code null}
   * @param initialValue      the initial combo+input value shown in the UI
   */
  protected DefaultOffCustomParameter(@NotNull EP embeddedParameter, @NotNull V defaultValue,
      @Nullable V offValue, @NotNull Val initialValue) {
    this(embeddedParameter, defaultValue, offValue, initialValue, true);
  }

  /**
   * @param embeddedParameter the parameter providing the input field (name, description,
   *                          validation, …)
   * @param defaultValue      returned by {@link #resolve()} when
   *                          {@link DefaultOffCustomOption#DEFAULT} is selected
   * @param offValue          returned by {@link #resolve()} when {@link DefaultOffCustomOption#OFF}
   *                          is selected; may be {@code null}
   * @param initialValue      the initial combo+input value shown in the UI
   * @param includeOff        if {@code false} the {@link DefaultOffCustomOption#OFF} entry is
   *                          omitted from the combo-box choices
   */
  protected DefaultOffCustomParameter(@NotNull EP embeddedParameter, @NotNull V defaultValue,
      @Nullable V offValue, @NotNull Val initialValue, boolean includeOff) {
    super(embeddedParameter, optionsFor(includeOff), DefaultOffCustomOption.CUSTOM, initialValue);
    this.defaultValue = defaultValue;
    this.offValue = offValue;
    this.includeOff = includeOff;
  }

  private static DefaultOffCustomOption[] optionsFor(boolean includeOff) {
    return includeOff ? DefaultOffCustomOption.values()
        : new DefaultOffCustomOption[]{DefaultOffCustomOption.DEFAULT, DefaultOffCustomOption.CUSTOM};
  }

  /**
   * Resolves the effective value using the option stored in {@link #value} together with the
   * {@link #defaultValue} and {@link #offValue} fields. No external arguments are required.
   *
   * @return the resolved value; may be {@code null} when {@link DefaultOffCustomOption#OFF} is
   * selected and {@link #offValue} is {@code null}
   */
  @Nullable
  public V resolve() {
    return switch (value.getSelectedOption()) {
      case DEFAULT -> defaultValue;
      case OFF -> offValue;
      case CUSTOM -> value.getEmbeddedValue();
    };
  }

  public @NotNull V getDefaultValue() {
    return defaultValue;
  }

  public @Nullable V getOffValue() {
    return offValue;
  }
}
