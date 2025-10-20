/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.util.dependencylicenses;

import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.components.factories.FxTextFlows;
import io.github.mzmine.javafx.components.factories.FxTexts;
import io.github.mzmine.main.MZmineCore;
import java.util.List;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.ListCell;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.TextFlow;

public class DependencyListCell extends ListCell<Dependency> {

  public DependencyListCell() {
    setGraphic(buildLayout());
  }

  private Region buildLayout() {
    Hyperlink dependencyName = FxLabels.newHyperlink(() -> {
      final Dependency dep = itemProperty().get();
      if (dep == null) {
        return;
      }
      if (dep.moduleUrls() == null || dep.moduleUrls().isEmpty()) {
        return;
      }
      final String url = dep.moduleUrls().getFirst();
      if (url != null && !url.isBlank()) {
        MZmineCore.getDesktop().openWebPage(url);
      }
    }, "dependency");

    final TextFlow licenseFlow = FxTextFlows.newTextFlow();
    updateLicenseFlow(licenseFlow);

    itemProperty().subscribe(d -> {
      if (d != null) {
        dependencyName.setText(
            d.moduleName().substring(Math.max(d.moduleName().lastIndexOf(":") + 1, 0)) + " v"
                + d.moduleVersion());
      } else {
        dependencyName.setText("");
      }
      updateLicenseFlow(licenseFlow);
    });

    return new FlowPane(5, 5, dependencyName, new Rectangle(20, 1, Color.TRANSPARENT), licenseFlow);
  }

  private void updateLicenseFlow(TextFlow licenseFlow) {
    licenseFlow.getChildren().clear();
    final Dependency d = itemProperty().get();
    if (d != null && d.moduleLicenses() != null) {
      List<ModuleLicense> moduleLicenses = d.moduleLicenses();
      for (int i = 0; i < moduleLicenses.size(); i++) {
        ModuleLicense license = moduleLicenses.get(i);
        Hyperlink link = FxLabels.newHyperlink(() -> {
          final String url = license.moduleLicenseUrl();
          if (url != null && !url.isBlank()) {
            MZmineCore.getDesktop().openWebPage(url);
          }
        }, license.moduleLicense());
        licenseFlow.getChildren().add(link);
        if (i < moduleLicenses.size() - 1) {
          licenseFlow.getChildren().add(FxTexts.text(", "));
        }
      }
    }
    licenseFlow.setDisable(licenseFlow.getChildren().isEmpty());
  }
}
