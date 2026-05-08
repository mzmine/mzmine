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

package io.github.mzmine.modules.dataprocessing.id_spectral_library_analog_search;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.impl.AbstractProcessingModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;

/**
 * Searches feature list rows against a spectral library using the four spectral-networking
 * similarity algorithms (modified cosine, no-precursor cosine, MS2Deepscore, DREAMS) but with a
 * wide precursor window so chemical analogs are returned. Results are stored on the row in the
 * {@link io.github.mzmine.datamodel.features.types.annotations.AnalogSpectralLibraryMatchesType}
 * data type as a list of {@link io.github.mzmine.util.spectraldb.entry.SpectralDBAnnotation}s.
 */
public class AnalogSpectralLibrarySearchModule extends AbstractProcessingModule {

  public static final String MODULE_NAME = "Analog spectral library search";

  public AnalogSpectralLibrarySearchModule() {
    super(MODULE_NAME, AnalogSpectralLibrarySearchParameters.class, MZmineModuleCategory.ANNOTATION,
        "Search feature list rows against a spectral library using analog (modification-aware) similarity.");
  }

  @Override
  public @NotNull ExitCode runModule(@NotNull final MZmineProject project,
      @NotNull final ParameterSet parameters, @NotNull final Collection<Task> tasks,
      @NotNull final Instant moduleCallDate) {
    final ModularFeatureList[] featureLists = parameters.getValue(
        AnalogSpectralLibrarySearchParameters.featureLists).getMatchingFeatureLists();
    // one task per feature list — keeps progress reporting per list and avoids cross-list lock contention
    Arrays.stream(featureLists).forEach(flist -> tasks.add(
        new AnalogSpectralLibrarySearchTask(project, parameters, flist, moduleCallDate,
            this.getClass())));
    return ExitCode.OK;
  }
}
