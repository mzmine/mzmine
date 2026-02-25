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

/**
 * Value for {@link DefaultOffCustomIntParameter}, combining a {@link DefaultOffCustomOption} with a
 * custom integer input.
 */
public record DefaultOffCustomIntValue(DefaultOffCustomOption option, int custom)
    implements ComboWithInputValue<DefaultOffCustomOption, Integer> {

  @Override
  public DefaultOffCustomOption getSelectedOption() {
    return option;
  }

  @Override
  public Integer getEmbeddedValue() {
    return custom;
  }

  /**
   * Resolves the effective integer value.
   *
   * @param defaultValue the value used when {@link DefaultOffCustomOption#DEFAULT} is selected
   * @param offValue     the value used when {@link DefaultOffCustomOption#OFF} is selected
   * @return the resolved value
   */
  public int resolve(int defaultValue, int offValue) {
    return switch (option) {
      case DEFAULT -> defaultValue;
      case OFF -> offValue;
      case CUSTOM -> custom;
    };
  }
}
