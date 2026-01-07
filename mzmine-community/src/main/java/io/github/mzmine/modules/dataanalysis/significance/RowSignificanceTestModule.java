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

package io.github.mzmine.modules.dataanalysis.significance;

import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ValuePropertyComponent;
import javafx.scene.layout.Region;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @param <T> The value type required to construct an instance of this test using
 *            {@link this#getInstance(ValuePropertyComponent)}
 */
public interface RowSignificanceTestModule<T> extends MZmineModule {

  @NotNull <C extends Region & ValuePropertyComponent<T>> C createConfigurationComponent();

  /**
   * @param parameterComponent A component to construct the test from.
   * @return A test or null if the configuration was invalid.
   */
  @Nullable RowSignificanceTest getInstance(@NotNull ValuePropertyComponent<T> parameterComponent);
}
