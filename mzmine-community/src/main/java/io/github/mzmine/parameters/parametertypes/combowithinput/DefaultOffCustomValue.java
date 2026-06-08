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

import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DefaultOffCustomValue<T> implements ComboWithInputValue<DefaultOffCustomOption, T> {

  private final @NotNull DefaultOffCustomOption option;
  private final @Nullable T custom;

  public DefaultOffCustomValue(@NotNull DefaultOffCustomOption option, @Nullable T custom) {
    this.option = option;
    this.custom = custom;
  }

  @Override
  public @NotNull DefaultOffCustomOption getSelectedOption() {
    return option;
  }

  @Override
  public @Nullable T getEmbeddedValue() {
    return custom;
  }

  public @NotNull DefaultOffCustomOption option() {
    return option;
  }

  public @Nullable T custom() {
    return custom;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || obj.getClass() != this.getClass()) {
      return false;
    }
    var that = (DefaultOffCustomValue) obj;
    return Objects.equals(this.option, that.option) && Objects.equals(this.custom, that.custom);
  }

  @Override
  public int hashCode() {
    return Objects.hash(option, custom);
  }

  @Override
  public String toString() {
    return "DefaultOffCustomValue[" + "option=" + option + ", " + "custom=" + custom + ']';
  }

}
