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

package io.github.mzmine.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolverModule;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolverParameters;
import io.github.mzmine.modules.dataprocessing.featdet_msn_tree.MsnTreeFeatureDetectionModule;
import io.github.mzmine.modules.dataprocessing.featdet_msn_tree.MsnTreeFeatureDetectionParameters;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MZmineCoreTest {

  @Test
  void testUniqueModuleForParameterSet() {
    // modules are only initialized if asked for
    final MZmineModule moduleInstance = MZmineCore.getModuleInstance(
        MsnTreeFeatureDetectionModule.class);
    assertNotNull(moduleInstance, "Module instance not found for MsnTreeFeatureDetectionModule");

    final var params = new MsnTreeFeatureDetectionParameters();

    final List<MZmineModule> modules = MZmineCore.getModulesForParameterSet(params);
    assertEquals(1, modules.size());

    final Optional<MZmineModule> module = MZmineCore.getModuleForParameterSetIfUnique(params);
    assertTrue(module.isPresent(), "No unique module found for msn tree builder");
  }

  @Test
  void testFailingUniqueModuleForParameterSet() {
    final MZmineModule moduleInstance = MZmineCore.getModuleInstance(
        MinimumSearchFeatureResolverModule.class);
    assertNotNull(moduleInstance,
        "Module instance not found for MinimumSearchFeatureResolverModule");

    var params = new MinimumSearchFeatureResolverParameters();
    final Optional<MZmineModule> module = MZmineCore.getModuleForParameterSetIfUnique(params);
    assertTrue(module.isPresent(), "No unique module found for local min parameters");

    final List<MZmineModule> modules = MZmineCore.getModulesForParameterSet(params);
    assertEquals(1, modules.size());
  }

}