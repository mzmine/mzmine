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

package io.github.mzmine.modules;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.batchmode.BatchQueue;
import io.github.mzmine.modules.batchmode.BatchQueueParameter;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetectionModule;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetectionParameters;
import io.github.mzmine.modules.impl.MZmineProcessingStepImpl;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.ParameterUtils;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import testutils.MZmineTestUtil;

class MZmineProcessingStepEqualityTest {

  @BeforeAll
  static void initMzmine() {
    MZmineTestUtil.startMzmineCore();
  }

  @Test
  void duplicateConfigurationsRetainObjectIdentity() {
    final MZmineProcessingModule module = massDetectionModule();
    final MZmineProcessingStep<MZmineProcessingModule> first = new MZmineProcessingStepImpl<>(
        module, massDetectionParameters(1));
    final MZmineProcessingStep<MZmineProcessingModule> second = new MZmineProcessingStepImpl<>(
        module, massDetectionParameters(1));

    Assertions.assertNotEquals(first, second);
    Assertions.assertTrue(ParameterUtils.equalValues(first, second, false, false));

    final BatchQueue queue = new BatchQueue();
    queue.addAll(first, second);
    queue.removeAll(List.of(first));

    Assertions.assertEquals(1, queue.size());
    Assertions.assertSame(second, queue.getFirst());
  }

  @Test
  void batchQueueParametersCompareStepValues() {
    final MZmineProcessingModule module = massDetectionModule();
    final BatchQueue firstQueue = new BatchQueue();
    firstQueue.add(new MZmineProcessingStepImpl<>(module, massDetectionParameters(1)));
    final BatchQueue secondQueue = new BatchQueue();
    secondQueue.add(new MZmineProcessingStepImpl<>(module, massDetectionParameters(1)));

    final BatchQueueParameter first = new BatchQueueParameter();
    first.setValue(firstQueue);
    final BatchQueueParameter second = new BatchQueueParameter();
    second.setValue(secondQueue);

    Assertions.assertTrue(first.valueEquals(second));
  }

  @Test
  void differentParameterValuesDoNotCompareEqual() {
    final MZmineProcessingModule module = massDetectionModule();
    final MZmineProcessingStep<MZmineProcessingModule> ms1 = new MZmineProcessingStepImpl<>(module,
        massDetectionParameters(1));
    final MZmineProcessingStep<MZmineProcessingModule> ms2 = new MZmineProcessingStepImpl<>(module,
        massDetectionParameters(2));

    Assertions.assertFalse(ParameterUtils.equalValues(ms1, ms2, false, false));
  }

  @Test
  void differentModulesDoNotCompareEqual() {
    final ParameterSet parameters = massDetectionParameters(1);
    final MZmineProcessingStep<MZmineProcessingModule> massDetection = new MZmineProcessingStepImpl<>(
        massDetectionModule(), parameters);
    final MZmineProcessingStep<MZmineProcessingModule> importStep = new MZmineProcessingStepImpl<>(
        massDetectionModule(), massDetectionParameters(2));

    Assertions.assertFalse(ParameterUtils.equalValues(massDetection, importStep, false, false));
  }

  @Test
  void batchQueueParametersWithDifferentStepValuesDoNotCompareEqual() {
    final MZmineProcessingModule module = massDetectionModule();
    final BatchQueue firstQueue = new BatchQueue();
    firstQueue.add(new MZmineProcessingStepImpl<>(module, massDetectionParameters(1)));
    final BatchQueue secondQueue = new BatchQueue();
    secondQueue.add(new MZmineProcessingStepImpl<>(module, massDetectionParameters(2)));

    final BatchQueueParameter first = new BatchQueueParameter();
    first.setValue(firstQueue);
    final BatchQueueParameter second = new BatchQueueParameter();
    second.setValue(secondQueue);

    Assertions.assertFalse(first.valueEquals(second));
  }

  private static ParameterSet massDetectionParameters(final int msLevel) {
    final ParameterSet parameters = new MassDetectionParameters().cloneParameterSet();
    parameters.setParameter(MassDetectionParameters.scanSelection, new ScanSelection(msLevel));
    return parameters;
  }

  private static MZmineProcessingModule massDetectionModule() {
    return MZmineCore.getModuleInstance(MassDetectionModule.class);
  }

}
