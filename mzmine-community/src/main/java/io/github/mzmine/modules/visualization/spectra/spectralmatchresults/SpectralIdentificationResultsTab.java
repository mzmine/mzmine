/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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

import io.github.mzmine.gui.framework.fx.features.SimpleFeatureListTab;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFX;
import io.github.mzmine.util.FeatureUtils;
import java.util.stream.Collectors;
import javafx.beans.binding.Bindings;
import javafx.util.Subscription;


public class SpectralIdentificationResultsTab extends SimpleFeatureListTab {

  private final SpectraIdentificationResultsPane matchPane;

  public SpectralIdentificationResultsTab(final FeatureTableFX table) {
    super("Spectral matches", false, false);

    matchPane = new SpectraIdentificationResultsPane(getParentGroup());
    setContent(matchPane);
    matchPane.setFeatureTable(table);
    final var sub = table.getSelectionModel().selectedItemProperty().subscribe(
        _ -> setSubTitle(table.getSelectedRows().stream().map(FeatureUtils::rowToString)
            .collect(Collectors.joining(", "))));
    setOnCloseRequest(_ -> sub.unsubscribe());
  }
}
