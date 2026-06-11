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

import io.github.mzmine.parameters.parametertypes.combowithinput.ComboWithInputValue;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Value record for {@link DuplicateAnnotationScopeParameter}. Carries the selected scope and, when
 * {@link DuplicateAnnotationScope#WITHIN_RT_TOLERANCE} is active, the RT tolerance used to
 * partition duplicate candidates into independent RT clusters before winner selection.
 */
public record DuplicateAnnotationScopeFilter(@NotNull DuplicateAnnotationScope scope,
                                             @Nullable RTTolerance rtTolerance) implements
    ComboWithInputValue<DuplicateAnnotationScope, RTTolerance> {

  @Override
  public @NotNull DuplicateAnnotationScope getSelectedOption() {
    return scope;
  }

  @Override
  public @Nullable RTTolerance getEmbeddedValue() {
    return rtTolerance;
  }

  // RTTolerance does not override equals/hashCode, so we implement them here to compare by value.
  // The rtTolerance component is only meaningful when scope == WITHIN_RT_TOLERANCE.
  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof DuplicateAnnotationScopeFilter other)) {
      return false;
    }
    if (scope != other.scope) {
      return false;
    }
    if (scope != DuplicateAnnotationScope.WITHIN_RT_TOLERANCE) {
      return true;
    }
    if (rtTolerance == null && other.rtTolerance == null) {
      return true;
    }
    if (rtTolerance == null || other.rtTolerance == null) {
      return false;
    }
    return Float.compare(rtTolerance.getTolerance(), other.rtTolerance.getTolerance()) == 0
        && rtTolerance.getUnit() == other.rtTolerance.getUnit();
  }

  @Override
  public int hashCode() {
    if (scope != DuplicateAnnotationScope.WITHIN_RT_TOLERANCE || rtTolerance == null) {
      return scope.hashCode();
    }
    return Objects.hash(scope, rtTolerance.getTolerance(), rtTolerance.getUnit());
  }
}
