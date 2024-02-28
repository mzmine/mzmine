/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.visualization.spectra.spectralmatchresults;

import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFX;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.spectraldb.entry.SpectralDBAnnotation;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SpectraIdentificationResultsModule implements MZmineModule {

  public static final String MODULE_NAME = "Local spectral libraries search results";

  public static void showNewTab(List<ModularFeatureListRow> rows) {
    showNewTab(rows, null);
  }
  public static void showNewTab(List<ModularFeatureListRow> rows, FeatureTableFX table) {
    List<SpectralDBAnnotation> spectralID =
        rows.stream().flatMap(row -> row.getSpectralLibraryMatches().stream()).toList();
    if (!spectralID.isEmpty()) {
      SpectraIdentificationResultsWindowFX tab = new SpectraIdentificationResultsWindowFX(table);
      tab.setTitle(rows);
      tab.setText("Spectral matches "+spectralID.size());
      tab.addMatches(spectralID);
      MZmineCore.getDesktop().addTab(tab);
    }
  }

  @Override
  public @NotNull
  String getName() {
    return MODULE_NAME;
  }

  @Override
  public @Nullable
  Class<? extends ParameterSet> getParameterSetClass() {
    return SpectraIdentificationResultsParameters.class;
  }
}
