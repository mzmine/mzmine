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

package io.github.mzmine.modules.dataanalysis.significance;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataanalysis.significance.anova.AnovaModule;
import io.github.mzmine.modules.dataanalysis.significance.anova.AnovaTest;
import io.github.mzmine.modules.dataanalysis.significance.ttest.TTestModule;
import java.util.List;

public enum RowSignificanceTestModules {
  TTEST(MZmineCore.getModuleInstance(TTestModule.class),
      UnivariateRowSignificanceTest.class), ANOVA(MZmineCore.getModuleInstance(AnovaModule.class),
      AnovaTest.class);

  public static final List<RowSignificanceTestModules> TWO_GROUP_TESTS = List.of(TTEST);
  public static final List<RowSignificanceTestModules> TWO_OR_MORE_GROUP_TESTS = List.of(TTEST,
      ANOVA);

  private final RowSignificanceTestModule<?> module;

  private final Class<? extends RowSignificanceTest> testClass;

  RowSignificanceTestModules(RowSignificanceTestModule<?> module,
      Class<? extends RowSignificanceTest> testClass) {
    this.module = module;
    this.testClass = testClass;
  }

  public RowSignificanceTestModule<?> getModule() {
    return module;
  }

  public Class<? extends RowSignificanceTest> getTestClass() {
    return testClass;
  }
}
