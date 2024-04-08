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

package io.github.mzmine.gui.mainwindow.introductiontab;

import io.github.mzmine.gui.mainwindow.UsersTab;
import io.github.mzmine.javafx.components.factories.FxButtons;
import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.javafx.util.FxIconUtil;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.tools.batchwizard.BatchWizardTab;
import java.util.logging.Logger;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class IntroductionTabBuilder extends FxViewBuilder<IntroductionTabModel> {

  private static final Logger logger = Logger.getLogger(IntroductionTabBuilder.class.getName());
  private final HBox wizardImageLightMode = FxIconUtil.resizeImage(
      "icons/introductiontab/logos_mzio_mzwizard.png", 300, 220);
  private final HBox wizardImageDarkMode = FxIconUtil.resizeImage(
      "icons/introductiontab/logos_mzio_mzwizard_light.png", 300, 220);
  private HBox mzmineImageLightMode = FxIconUtil.resizeImage(
      "icons/introductiontab/logos_mzio_mzmine.png", 350, 200);
  private HBox mzmineImageDarkMode = FxIconUtil.resizeImage(
      "icons/introductiontab/logos_mzio_mzmine_light.png", 350, 200);

  protected IntroductionTabBuilder(IntroductionTabModel model) {
    super(model);
  }

  @Override
  public Region build() {
    final VBox main = new VBox(40);
    main.setAlignment(Pos.CENTER);
    model.isDarkModeProperty()
        .bind(MZmineCore.getConfiguration().getPreferences().darkModeProperty());

    final HBox mzmineImageWrapper = new HBox(
        model.isIsDarkMode() ? mzmineImageDarkMode : mzmineImageLightMode);
    model.isDarkModeProperty().addListener((_, _, isDarkMode) -> {
      mzmineImageWrapper.getChildren().clear();
      mzmineImageWrapper.getChildren().add(isDarkMode ? mzmineImageDarkMode : mzmineImageLightMode);
    });

    HBox title = new HBox(FxLayout.DEFAULT_SPACE,
        /*FxLabels.styled("Welcome to ", "huge-title-label"),*/ mzmineImageWrapper /*,
        FxLabels.styled("!", "huge-title-label")*/);
    title.setAlignment(Pos.TOP_CENTER);

    main.getChildren().add(title);
    main.getChildren().add(createWizardRow());
    main.getChildren().add(createManagementRow());
    main.getChildren().add(createHowToCite());

    final Pane versionPane = createNewVersionPane();
    model.newVersionAvailableProperty().addListener((_, _, newVersion) -> {
      if (newVersion) {
        main.getChildren().add(versionPane);
      } else {
        main.getChildren().remove(versionPane);
      }
    });

    final ScrollPane scroll = new ScrollPane(main);
    scroll.setFitToWidth(true);
    scroll.setFitToHeight(true);
    scroll.setCenterShape(true);
    return scroll;
  }

  private Region createWizardRow() {
    final GridPane pane = new GridPane(20, 5);

    final HBox wizardImageWrapper = new HBox(
        model.isIsDarkMode() ? wizardImageDarkMode : wizardImageLightMode);
    model.isDarkModeProperty().addListener((_, _, isDarkMode) -> {
      wizardImageWrapper.getChildren().clear();
      wizardImageWrapper.getChildren().add(isDarkMode ? wizardImageDarkMode : wizardImageLightMode);
    });
    final Label lblWizard = FxLabels.styled("Easy workflow setup", "bold-title-label");
    Button btnWizard = FxButtons.createButton(wizardImageWrapper,
        () -> MZmineCore.getDesktop().addTab(new BatchWizardTab()));

    pane.getColumnConstraints()
        .addAll(createColumnConstraints(), createColumnConstraints(), createColumnConstraints());

    pane.add(lblWizard, 0, 0);
    pane.add(btnWizard, 0, 1);

    FxLayout.centerAllNodesHorizontally(pane);
    pane.setAlignment(Pos.CENTER);

    return pane;
  }

  private ColumnConstraints createColumnConstraints() {
    final ColumnConstraints c = new ColumnConstraints();
    c.setFillWidth(true);
    return c;
  }

  private Pane createManagementRow() {
//    GridPane pane = new GridPane(20, 5);

    final Button btnYoutube = FxButtons.createButton(FxIconUtil.getFontIcon("bi-youtube", 45),
        "Video tutorials", () -> MZmineCore.getDesktop()
            .openWebPage("https://www.youtube.com/channel/UCXsBoraCbK80xtf4jCpJHYQ"));

    final Button btnWebsite = FxButtons.createButton(FxIconUtil.getFontIcon("bi-globe2", 45),
        "mzmine website", () -> MZmineCore.getDesktop().openWebPage("https://mzio.io/#mzmine"));

    final Button btnUserManagement = FxButtons.createButton(
        FxIconUtil.getFontIcon("bi-person-badge", 45), "Account management",
        () -> MZmineCore.getDesktop().addTab(new UsersTab()));

    final Button btnDevelopment = FxButtons.createButton(
        FxIconUtil.getFontIcon("hwf-document-file-java", 45), "Join the development",
        () -> MZmineCore.getDesktop()
            .openWebPage("https://mzmine.github.io/mzmine_documentation/contribute_intellij.html"));

    final Button btnDocs = FxButtons.createButton(FxIconUtil.getFontIcon("bi-book-half", 45),
        "Open online documentation", () -> MZmineCore.getDesktop()
            .openWebPage("https://mzmine.github.io/mzmine_documentation/getting_started.html"));

    final Button btnPreferences = FxButtons.createButton(FxIconUtil.getFontIcon("bi-gear", 45),
        "Configure mzmine",
        () -> MZmineCore.getConfiguration().getPreferences().showSetupDialog(true));

//    pane.add(btnPreferences, 2, 0);
//    pane.add(btnDocs, 1, 0);
//    pane.add(btnYoutube, 0, 0);
//    pane.add(btnWebsite, 1, 0);
//    pane.add(btnUserManagement, 2, 0);
//    pane.add(btnDevelopment, 3, 0);
//    FxLayout.centerAllNodesHorizontally(pane);

    FlowPane pane = new FlowPane(20, 20, btnPreferences, btnDocs, btnYoutube, btnWebsite,
        btnUserManagement, btnDevelopment);

    pane.setAlignment(Pos.CENTER);
    return pane;
  }

  private Pane createHowToCite() {
    VBox pane = new VBox(FxLayout.DEFAULT_SPACE);
    pane.setAlignment(Pos.CENTER);
    pane.getChildren().add(FxLabels.styled("How to cite", "bold-title-label"));

    final FlowPane box = new FlowPane(new Label("Schmid, R., Heuckeroth, S., Korf, A. "),
        FxLabels.italic("et. al. "), //
        new Label("Integrative analysis of multimodal mass spectrometry data in MZmine 3. "), //
        FxLabels.italic("Nat Biotechnol "), FxLabels.bold("41"), new Label(", 447-449 (2023)."));
    box.setAlignment(Pos.CENTER);
    pane.getChildren().add(box);

    final Hyperlink link = new Hyperlink("https://doi.org/10.1038/s41587-023-01690-2");
    link.setOnAction(e -> MZmineCore.getDesktop().openWebPage(link.getText()));
    pane.getChildren().add(link);

    return pane;
  }

  private Pane createNewVersionPane() {
    final VBox box = new VBox(20);
    final Label label = FxLabels.boldTitle("New version available!");
    final Button downloadButton = FxButtons.createButton(FxIconUtil.getFontIcon("bi-download", 60,
            ConfigService.getConfiguration().getDefaultColorPalette().getPositiveColor()),
        () -> MZmineCore.getDesktop()
            .openWebPage("https://github.com/mzmine/mzmine3/releases/tag/v3.9.0"));
    box.getChildren().addAll(label, downloadButton);
//    box.visibleProperty().bindBidirectional(model.newVersionAvailableProperty());
    box.setAlignment(Pos.CENTER);
    return box;
  }
}
