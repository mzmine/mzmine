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

package io.github.mzmine.modules.tools.tools_autoparam.estimation.domain;

import java.util.function.DoubleFunction;
import java.util.function.ToDoubleFunction;
import org.jetbrains.annotations.NotNull;

/**
 * A continuous search coordinate that controls a composite parameter value.
 */
public record MappedSearchDomain<T>(@NotNull DoubleSearchDomain coordinate,
                                    @NotNull ToDoubleFunction<T> encoder,
                                    @NotNull DoubleFunction<T> decoder) implements SearchDomain<T> {

  @Override
  public double lower() {
    return coordinate.lower();
  }

  @Override
  public double upper() {
    return coordinate.upper();
  }

  @Override
  public double encode(@NotNull T value) {
    return encoder.applyAsDouble(value);
  }

  @Override
  public @NotNull T decode(double value) {
    return decoder.apply(value);
  }

  @Override
  public @NotNull SearchScale searchScale() {
    return coordinate.searchScale();
  }

  @Override
  public @NotNull String format(@NotNull T value) {
    return Double.toString(encode(value));
  }
}
