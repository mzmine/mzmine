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

package io.github.mzmine.modules.dataanalysis.significance.ttest;

import io.github.mzmine.modules.dataanalysis.significance.RowSignificanceTest;
import io.github.mzmine.modules.dataanalysis.significance.RowSignificanceTestModule;
import io.github.mzmine.parameters.ValuePropertyComponent;
import io.github.mzmine.parameters.parametertypes.statistics.StorableTTestConfiguration;
import io.github.mzmine.parameters.parametertypes.statistics.TTestConfigurationComponent;
import io.mzio.mzmine.datamodel.parameters.ParameterSet;
import javafx.scene.layout.Region;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TTestModule implements RowSignificanceTestModule<StorableTTestConfiguration> {

  @Override
  public @NotNull String getName() {
    return "Student's t-Test";
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return TTestParameters.class;
  }

  @Override
  public @NotNull <C extends Region & ValuePropertyComponent<StorableTTestConfiguration>> C createConfigurationComponent() {
    return (C) new TTestConfigurationComponent();
  }

  @Override
  public RowSignificanceTest getInstance(
      @NotNull ValuePropertyComponent<StorableTTestConfiguration> parameterComponent) {
    final StorableTTestConfiguration value = parameterComponent.valueProperty().getValue();
    return value.toValidConfig();
  }
}
