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

package io.github.mzmine.modules.dataprocessing.filter_lipidannotationcleanup;

import io.github.mzmine.parameters.parametertypes.combowithinput.ComboWithInputParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;

/**
 * Parameter that controls the scope of duplicate-annotation detection in the lipid annotation
 * cleanup. The user can choose between global deduplication (all RTs) and RT-scoped deduplication
 * (only rows whose RTs are within the configured tolerance compete as duplicates).
 */
public class DuplicateAnnotationScopeParameter extends
    ComboWithInputParameter<DuplicateAnnotationScope, DuplicateAnnotationScopeFilter, RTToleranceParameter> {

  static final DuplicateAnnotationScopeFilter DEFAULT = new DuplicateAnnotationScopeFilter(
      DuplicateAnnotationScope.ALL_RTS, new RTTolerance(0.03f, RTTolerance.Unit.MINUTES));

  public DuplicateAnnotationScopeParameter() {
    this(new RTToleranceParameter("Annotation duplicate scope",
        "Rows with the same annotation within this RT window are treated as duplicates.",
        DEFAULT.rtTolerance()), DuplicateAnnotationScope.values(), DEFAULT);
  }

  private DuplicateAnnotationScopeParameter(final RTToleranceParameter embedded,
      final DuplicateAnnotationScope[] options, final DuplicateAnnotationScopeFilter defaultValue) {
    super(embedded, options, DuplicateAnnotationScope.WITHIN_RT_TOLERANCE, defaultValue);
  }

  @Override
  public DuplicateAnnotationScopeFilter createValue(final DuplicateAnnotationScope option,
      final RTToleranceParameter embeddedParameter) {
    return new DuplicateAnnotationScopeFilter(option, embeddedParameter.getValue());
  }

  @Override
  public DuplicateAnnotationScopeParameter cloneParameter() {
    return new DuplicateAnnotationScopeParameter(embeddedParameter.cloneParameter(),
        choices.toArray(DuplicateAnnotationScope[]::new), value);
  }
}
