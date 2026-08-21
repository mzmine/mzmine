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

package io.github.mzmine.modules.order;

import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.dataprocessing.align_gc.GCAlignerModule;
import io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.ModularADAPChromatogramBuilderModule;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.FeatureResolverModule;
import io.github.mzmine.modules.dataprocessing.featdet_masscalibration.MassCalibrationModule;
import io.github.mzmine.modules.dataprocessing.featdet_mobilityscanmerger.MobilityScanMergerModule;
import io.github.mzmine.modules.dataprocessing.featdet_shoulderpeaksfilter.ShoulderPeaksFilterModule;
import io.github.mzmine.modules.dataprocessing.featdet_spectraldeconvolutiongc.SpectralDeconvolutionGCModule;
import io.github.mzmine.modules.dataprocessing.filter_blanksubtraction.FeatureListBlankSubtractionModule;
import io.github.mzmine.modules.dataprocessing.filter_blanksubtraction_chromatograms.ChromatogramBlankSubtractionModule;
import io.github.mzmine.modules.dataprocessing.group_compoundgrouper.CompoundGrouperModule;
import io.github.mzmine.modules.dataprocessing.group_compoundgrouper.intensityrepresentation.ConfigCompoundRepresentationModule;
import io.github.mzmine.modules.dataprocessing.group_metacorrelate.corrgrouping.CorrelateGroupingModule;
import io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.addionannotations.AddIonNetworkingModule;
import io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.formula.createavgformulas.CreateAvgNetworkFormulasModule;
import io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.formula.prediction.FormulaPredictionIonNetworkModule;
import io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.ionidnetworking.IonNetworkingModule;
import io.github.mzmine.modules.io.export_msn_tree.MSnTreeExportModule;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class InferredModuleOrderRecommendationsTest {

  @Test
  void taskPreconditionsAreDeclaredByTheirModules() {
    assertMustRunAfter(new GCAlignerModule(), SpectralDeconvolutionGCModule.class);

    assertMustSatisfy(new ModularADAPChromatogramBuilderModule(), MassDetectionCondition.INSTANCE);
    assertMustSatisfy(new MobilityScanMergerModule(), MassDetectionCondition.INSTANCE);
    assertMustSatisfy(new ShoulderPeaksFilterModule(), MassDetectionCondition.INSTANCE);
    assertMustSatisfy(new MassCalibrationModule(), MassDetectionCondition.INSTANCE);
    assertMustSatisfy(new MSnTreeExportModule(), MassDetectionCondition.INSTANCE);

    assertMustRunAfter(new ConfigCompoundRepresentationModule(), CompoundGrouperModule.class);
    assertMustRunAfter(new IonNetworkingModule(), CorrelateGroupingModule.class);
    assertMustRunAfter(new AddIonNetworkingModule(), CorrelateGroupingModule.class);
    assertMustRunAfter(new FormulaPredictionIonNetworkModule(), IonNetworkingModule.class);
    assertMustRunAfter(new CreateAvgNetworkFormulasModule(), IonNetworkingModule.class);

    assertMustSatisfy(new FeatureListBlankSubtractionModule(),
        ModuleCategoryOrderCondition.after(MZmineModuleCategory.ALIGNMENT));
    assertMustRunBefore(new ChromatogramBlankSubtractionModule(), FeatureResolverModule.class);
  }

  private static void assertMustRunAfter(@NotNull final MZmineProcessingModule module,
      @NotNull final Class<? extends MZmineProcessingModule> anchorModule) {
    final ModuleOrderRule expectedRule = ModuleOrderRule.mustRunAfter(anchorModule);
    Assertions.assertTrue(
        module.getModuleOrderRecommendations().stream().map(ModuleOrderRecommendation::rule)
            .anyMatch(expectedRule::equals),
        () -> "%s must declare that it runs after %s".formatted(module.getName(),
            anchorModule.getSimpleName()));
  }

  private static void assertMustRunBefore(@NotNull final MZmineProcessingModule module,
      @NotNull final Class<? extends MZmineProcessingModule> anchorModule) {
    final ModuleOrderRule expectedRule = ModuleOrderRule.mustRunBefore(anchorModule);
    Assertions.assertTrue(
        module.getModuleOrderRecommendations().stream().map(ModuleOrderRecommendation::rule)
            .anyMatch(expectedRule::equals),
        () -> "%s must declare that it runs before %s".formatted(module.getName(),
            anchorModule.getSimpleName()));
  }

  private static void assertMustSatisfy(@NotNull final MZmineProcessingModule module,
      @NotNull final ModuleOrderCondition condition) {
    final ModuleOrderRule expectedRule = ModuleOrderRule.mustSatisfy(condition);
    Assertions.assertTrue(
        module.getModuleOrderRecommendations().stream().map(ModuleOrderRecommendation::rule)
            .anyMatch(expectedRule::equals),
        () -> "%s must declare custom condition %s".formatted(module.getName(),
            condition.getClass().getSimpleName()));
  }
}
