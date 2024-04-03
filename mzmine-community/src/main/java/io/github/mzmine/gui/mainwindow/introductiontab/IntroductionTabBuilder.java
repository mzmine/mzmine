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
//    mzmineImage.setAlignment(Pos.TOP_CENTER);

    main.getChildren().add(title);
    main.getChildren().add(createWizardDocsRow());
    main.getChildren().add(createManagementRow());
    main.getChildren().add(createHowToCite());
    main.getChildren().add(createNewVersionPane());

    return main;
  }

  private Region createWizardDocsRow() {
    final GridPane pane = new GridPane(20, 5);

    final HBox wizardImageWrapper = new HBox(
        model.isIsDarkMode() ? wizardImageDarkMode : wizardImageLightMode);
    model.isDarkModeProperty().addListener((_, _, isDarkMode) -> {
      wizardImageWrapper.getChildren().clear();
      wizardImageWrapper.getChildren().add(isDarkMode ? wizardImageDarkMode : wizardImageLightMode);
    });
    final Label lblWizard = FxLabels.styled("Easy workflow setup", "bold-title-label");
    Button btnWizard = new Button(null, wizardImageWrapper);
    btnWizard.setOnAction(_ -> MZmineCore.getDesktop().addTab(new BatchWizardTab()));

    final Label lblDocs = FxLabels.styled("Open online documentation", "bold-title-label");
    Button btnDocs = new Button(null, FxIconUtil.getFontIcon("bi-book-half", 75));
    btnDocs.setOnAction(_ -> MZmineCore.getDesktop()
        .openWebPage("https://mzmine.github.io/mzmine_documentation/getting_started.html"));

    final Label lblPrefs = FxLabels.styled("Configure mzmine", "bold-title-label");
    Button btnPreferences = new Button(null, FxIconUtil.getFontIcon("bi-gear", 75));

    btnPreferences.setOnAction(
        _ -> MZmineCore.getConfiguration().getPreferences().showSetupDialog(true));

    pane.getColumnConstraints().addAll(createColumnConstraints(.3), createColumnConstraints(.3),
        createColumnConstraints(.3));

    pane.add(lblWizard, 0, 0);
    pane.add(btnWizard, 0, 1);
    pane.add(lblDocs, 1, 0);
    pane.add(btnDocs, 1, 1);
    pane.add(lblPrefs, 2, 0);
    pane.add(btnPreferences, 2, 1);

    FxLayout.centerAllNodesHorizontally(pane);
    pane.setAlignment(Pos.CENTER);

    return pane;
  }

  private ColumnConstraints createColumnConstraints(double percentWidth) {
    final ColumnConstraints c = new ColumnConstraints();
    c.setFillWidth(true);
    return c;
  }

  private Pane createManagementRow() {
    GridPane pane = new GridPane(20, 5);

    final Label youtube = FxLabels.boldTitle("Video tutorials");
    final Button btnYoutube = new Button(null, FxIconUtil.getFontIcon("bi-youtube", 45));
    btnYoutube.setOnAction(_ -> MZmineCore.getDesktop()
        .openWebPage("https://www.youtube.com/channel/UCXsBoraCbK80xtf4jCpJHYQ"));

    final var lblWebsite = FxLabels.boldTitle("mzmine website");
    final Button btnWebsite = new Button(null, FxIconUtil.getFontIcon("bi-globe2", 45));
    btnWebsite.setOnAction(_ -> MZmineCore.getDesktop().openWebPage("https://mzio.io/#mzmine"));

    final var lblUserManagement = FxLabels.boldTitle("Account management");
    final Button btnUserManagement = new Button(null,
        FxIconUtil.getFontIcon("bi-person-badge", 45));
    btnUserManagement.setOnAction(
        _ -> MZmineCore.getDesktop().openWebPage("https://auth.mzio.io/"));

    final Label lblDevelopment = FxLabels.boldTitle("Join the development");
    final Button btnDevelopment = new Button(null,
        FxIconUtil.getFontIcon("hwf-document-file-java", 45));
    btnDevelopment.setOnAction(_ -> MZmineCore.getDesktop()
        .openWebPage("https://mzmine.github.io/mzmine_documentation/contribute_intellij.html"));

    pane.add(youtube, 0, 0);
    pane.add(btnYoutube, 0, 1);
    pane.add(lblWebsite, 1, 0);
    pane.add(btnWebsite, 1, 1);
    pane.add(lblUserManagement, 2, 0);
    pane.add(btnUserManagement, 2, 1);
    pane.add(lblDevelopment, 3, 0);
    pane.add(btnDevelopment, 3, 1);

    FxLayout.centerAllNodesHorizontally(pane);
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
    box.visibleProperty().bindBidirectional(model.newVersionAvailableProperty());
    box.setAlignment(Pos.CENTER);
    return box;
  }
}
