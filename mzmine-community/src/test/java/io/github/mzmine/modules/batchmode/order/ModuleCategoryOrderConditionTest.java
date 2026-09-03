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

import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.batchmode.BatchQueue;
import io.github.mzmine.modules.impl.MZmineProcessingStepImpl;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportModule;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ModuleCategoryOrderConditionTest {

  @Test
  void contextualDescriptionUsesTheMatchingModuleName() {
    final TestSubjectModule subject = subjectAfterAlignment();
    final BatchQueue queue = queue(subject,
        new TestOrderModule("Test alignment", MZmineModuleCategory.ALIGNMENT));

    final Map<Integer, String> messages = BatchModuleOrderValidator.validateAndFormatByStep(queue);

    Assertions.assertTrue(messages.get(0).contains("Subject MUST run after Test alignment"));
  }

  @Test
  void contextualDescriptionRequestsAMissingCategoryModule() {
    final Map<Integer, String> messages = BatchModuleOrderValidator.validateAndFormatByStep(
        queue(subjectAfterAlignment()));

    Assertions.assertTrue(
        messages.get(0).contains("Subject MUST run after a module in the Alignment category"));
  }

  @Test
  void moduleInAnotherPipelineIsNotUsedInTheDescription() {
    final BatchQueue queue = queue(new AllSpectralDataImportModule(),
        new TestOrderModule("Other-pipeline alignment", MZmineModuleCategory.ALIGNMENT),
        new AllSpectralDataImportModule(), subjectAfterAlignment());

    final Map<Integer, String> messages = BatchModuleOrderValidator.validateAndFormatByStep(queue);

    Assertions.assertTrue(
        messages.get(3).contains("Subject MUST run after a module in the Alignment category"));
    Assertions.assertFalse(messages.get(3).contains("Other-pipeline alignment"));
  }

  @Test
  void ifPresentConditionIsIgnoredWhenNoAnchorMatches() {
    final TestSubjectModule subject = new TestSubjectModule(
        new ModuleOrderRecommendation("Test rationale", ModuleOrderRule.ifPresentShouldRunAfter(
            ModuleCategoryOrderCondition.of(MZmineModuleCategory.ALIGNMENT))));

    Assertions.assertTrue(
        BatchModuleOrderValidator.validateAndFormatByStep(queue(subject)).isEmpty());
  }

  @Test
  void ifPresentConditionWarnsWhenMatchingAnchorIsInTheWrongPosition() {
    final TestSubjectModule subject = new TestSubjectModule(
        new ModuleOrderRecommendation("Test rationale", ModuleOrderRule.ifPresentShouldRunAfter(
            ModuleCategoryOrderCondition.of(MZmineModuleCategory.ALIGNMENT))));
    final BatchQueue queue = queue(subject,
        new TestOrderModule("Test alignment", MZmineModuleCategory.ALIGNMENT));

    final Map<Integer, String> messages = BatchModuleOrderValidator.validateAndFormatByStep(queue);

    Assertions.assertTrue(
        messages.get(0)
            .contains("Subject should run after Test alignment (step 2) (if present)"));
  }

  private static TestSubjectModule subjectAfterAlignment() {
    return new TestSubjectModule(new ModuleOrderRecommendation("Test rationale",
        ModuleOrderRule.mustRunAfter(
            ModuleCategoryOrderCondition.of(MZmineModuleCategory.ALIGNMENT))));
  }

  private static BatchQueue queue(final MZmineProcessingModule... modules) {
    final BatchQueue queue = new BatchQueue();
    for (final MZmineProcessingModule module : modules) {
      queue.add(new MZmineProcessingStepImpl<>(module, new SimpleParameterSet()));
    }
    return queue;
  }
}
