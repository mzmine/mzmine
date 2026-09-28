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

package io.github.mzmine.modules.dataprocessing.id_nist_mspepsearch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolverModule;
import io.github.mzmine.modules.dataprocessing.featdet_spectraldeconvolutiongc.SpectralDeconvolutionGCModule;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * The automatic search type: which of the two workflows a feature list is taken to belong to.
 */
class NistSearchModeAutoTest {

  /**
   * @param modules the modules of the applied methods, in order.
   * @return a feature list that reports exactly those applied methods.
   */
  private static FeatureList featureListWith(final MZmineModule... modules) {

    final ObservableList<FeatureListAppliedMethod> methods = FXCollections.observableArrayList();

    for (final MZmineModule module : modules) {
      final FeatureListAppliedMethod method = Mockito.mock(FeatureListAppliedMethod.class);
      Mockito.when(method.getModule()).thenReturn(module);
      methods.add(method);
    }

    final FeatureList featureList = Mockito.mock(FeatureList.class);
    Mockito.when(featureList.getAppliedMethods()).thenReturn(methods);

    return featureList;
  }

  @Test
  @DisplayName("A deconvoluted feature list is searched as GC-EI")
  void spectralDeconvolutionSelectsGcEi() {

    final FeatureList featureList = featureListWith(new MinimumSearchFeatureResolverModule(),
        new SpectralDeconvolutionGCModule());

    assertTrue(NistSearchMode.isDeconvolutedGcEi(featureList));
    assertEquals(NistSearchMode.GC_EI_IDENTITY, NistSearchMode.AUTO.resolve(featureList));
  }

  @Test
  @DisplayName("Any other feature list is searched as MS/MS")
  void everythingElseSelectsMsMs() {

    final FeatureList featureList = featureListWith(new MinimumSearchFeatureResolverModule());

    assertFalse(NistSearchMode.isDeconvolutedGcEi(featureList));
    assertEquals(NistSearchMode.MSMS_HIRES, NistSearchMode.AUTO.resolve(featureList));
    assertEquals(NistSearchMode.MSMS_HIRES, NistSearchMode.AUTO.resolve(featureListWith()));
    assertEquals(NistSearchMode.MSMS_HIRES, NistSearchMode.AUTO.resolve(null));
  }

  @Test
  @DisplayName("Every module of the spectral deconvolution category counts, not just the GC one")
  void theWholeCategoryCounts() {

    // a future deconvolution module must not have to be added here to be recognised
    assertEquals(MZmineModuleCategory.SPECTRALDECONVOLUTION,
        new SpectralDeconvolutionGCModule().getModuleCategory());
  }

  @Test
  @DisplayName("An explicitly chosen search type is never overridden")
  void explicitSearchTypesAreKept() {

    final FeatureList deconvoluted = featureListWith(new SpectralDeconvolutionGCModule());

    for (final NistSearchMode mode : NistSearchMode.searchTypes()) {
      assertEquals(mode, mode.resolve(deconvoluted));
      assertEquals(mode, mode.resolve(null));
      assertFalse(mode.isAutomatic());
    }
  }

  @Test
  @DisplayName("The automatic type has no search type of its own and says so")
  void unresolvedAutoFailsLoudly() {

    assertTrue(NistSearchMode.AUTO.isAutomatic());
    assertThrows(IllegalStateException.class, NistSearchMode.AUTO::requiredLibraryContent);
    assertThrows(IllegalStateException.class, NistSearchMode.AUTO::isHighResolution);
    assertThrows(IllegalStateException.class, NistSearchMode.AUTO::getSearchTypeLetter);
  }
}
