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

package io.github.mzmine.modules.batchmode.order;

import static io.github.mzmine.modules.batchmode.order.ModuleCategoryOrderCondition.of;
import static io.github.mzmine.modules.batchmode.order.ModuleOrderRecommendation.anyOf;
import static io.github.mzmine.modules.batchmode.order.ModuleOrderRecommendation.of;
import static io.github.mzmine.modules.batchmode.order.ModuleOrderRule.ifPresentMustRunBefore;
import static io.github.mzmine.modules.batchmode.order.ModuleOrderRule.ifPresentShouldRunAfter;

import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.io.spectraldbsubmit.batch.LibraryBatchGenerationModule;

public class ModuleOrderRecommendations {

  public static final ModuleOrderRecommendation BEFORE_ALIGNMENT_OR_LIB_EXPORT = anyOf(
      of("Annotations are not preserved during alignment",
          ifPresentShouldRunAfter(of(MZmineModuleCategory.ALIGNMENT))),
      of("The library generation requires prior annotation.",
          ifPresentMustRunBefore(LibraryBatchGenerationModule.class)));
}
