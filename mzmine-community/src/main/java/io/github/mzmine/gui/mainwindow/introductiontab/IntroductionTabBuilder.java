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
import io.github.mzmine.javafx.util.FxIcons;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.tools.batchwizard.BatchWizardTab;
import io.github.mzmine.util.javafx.LightAndDarkModeIcon;
import java.util.logging.Logger;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
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
import javafx.scene.paint.Color;

public class IntroductionTabBuilder extends FxViewBuilder<IntroductionTabModel> {

  private static final Logger logger = Logger.getLogger(IntroductionTabBuilder.class.getName());
  private LightAndDarkModeIcon mzmineIcon;
  private LightAndDarkModeIcon wizardIcon;

  protected IntroductionTabBuilder(IntroductionTabModel model) {
    super(model);
  }

  @Override
  public Region build() {
    final VBox main = new VBox(40);
    main.setAlignment(Pos.CENTER);
    model.isDarkModeProperty().bind(ConfigService.isDarkModeProperty());

    mzmineIcon = new LightAndDarkModeIcon("icons/introductiontab/logos_mzio_mzmine.png",
        "icons/introductiontab/logos_mzio_mzmine_light.png", 350, 200);

    HBox title = new HBox(FxLayout.DEFAULT_SPACE,
        /*FxLabels.styled("Welcome to ", "huge-title-label"),*/ mzmineIcon /*,
        FxLabels.styled("!", "huge-title-label")*/);
    title.setAlignment(Pos.TOP_CENTER);

    main.getChildren().add(title);
    main.getChildren().add(createWizardRow());
    main.getChildren().add(createManagementRow());
    main.getChildren().add(createHowToCite());

    final Pane versionPane = createNewVersionPane();
    model.newVersionAvailableProperty().subscribe((newVersion) -> {
      if (newVersion == null) {
        return;
      } else if (newVersion) {
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

    final HBox wizardImageWrapper = new LightAndDarkModeIcon(
        "icons/introductiontab/logos_mzio_mzwizard.png",
        "icons/introductiontab/logos_mzio_mzwizard_light.png", 300, 150);
    final Label lblWizard = FxLabels.newBoldTitle("Easy workflow setup");
    final Button btnWizard = FxButtons.graphicButton(wizardImageWrapper,
        "Open the mzwizard to easily configure a workflow.",
        _ -> MZmineCore.getDesktop().addTab(new BatchWizardTab()));

//    pane.getColumnConstraints()
//        .addAll(createColumnConstraints(), createColumnConstraints(), createColumnConstraints());

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

    final ButtonBase btnYoutube = FxIconUtil.newIconButton(FxIcons.YOUTUBE, 45, "Video tutorials",
        () -> MZmineCore.getDesktop()
            .openWebPage("https://www.youtube.com/channel/UCXsBoraCbK80xtf4jCpJHYQ"));

    final ButtonBase btnWebsite = FxIconUtil.newIconButton(FxIcons.WEBSITE, 45, "mzmine website",
        () -> MZmineCore.getDesktop().openWebPage("https://mzio.io/#mzmine"));

    final ButtonBase btnUserManagement = FxIconUtil.newIconButton(FxIcons.USER, 45,
        "Account management", () -> MZmineCore.getDesktop().addTab(new UsersTab()));

    final ButtonBase btnDevelopment = FxIconUtil.newIconButton(FxIcons.DEVELOPMENT, 45,
        "Join the development", () -> MZmineCore.getDesktop()
            .openWebPage("https://mzmine.github.io/mzmine_documentation/contribute_intellij.html"));

    final ButtonBase btnDocs = FxIconUtil.newIconButton(FxIcons.BOOK, 45,
        "Open online documentation", () -> MZmineCore.getDesktop()
            .openWebPage("https://mzmine.github.io/mzmine_documentation/getting_started.html"));

    final ButtonBase btnPreferences = FxIconUtil.newIconButton(FxIcons.GEAR_PREFERENCES, 45,
        "Configure mzmine",
        () -> MZmineCore.getConfiguration().getPreferences().showSetupDialog(true));

    FlowPane pane = new FlowPane(20, 20, btnPreferences, btnDocs, btnYoutube, btnWebsite,
        btnUserManagement, btnDevelopment);

    pane.setAlignment(Pos.CENTER);
    return pane;
  }

  private Pane createHowToCite() {
    VBox pane = new VBox(FxLayout.DEFAULT_SPACE);
    pane.setAlignment(Pos.CENTER);
    pane.getChildren().add(FxLabels.newBoldTitle("How to cite"));

    final FlowPane box = new FlowPane(new Label("Schmid, R., Heuckeroth, S., Korf, A. "),
        FxLabels.newItalicLabel("et. al. "), //
        new Label("Integrative analysis of multimodal mass spectrometry data in MZmine 3. "), //
        FxLabels.newItalicLabel("Nat Biotechnol "), FxLabels.newBoldLabel("41"),
        new Label(", 447-449 (2023)."));
    box.setAlignment(Pos.CENTER);
    pane.getChildren().add(box);

    final Hyperlink link = new Hyperlink("https://doi.org/10.1038/s41587-023-01690-2");
    link.setOnAction(e -> MZmineCore.getDesktop().openWebPage(link.getText()));
    pane.getChildren().add(link);

    return pane;
  }

  private Pane createNewVersionPane() {
    final VBox box = new VBox(20);
    final Label label = FxLabels.newBoldTitle("New version available!");
    final Button downloadButton = FxButtons.createButton(FxIconUtil.getFontIcon("bi-download", 60,
            Color.web("3391C1")),
        () -> MZmineCore.getDesktop()
            .openWebPage("https://github.com/mzmine/mzmine3/releases/tag/v3.9.0"));
    box.getChildren().addAll(label, downloadButton);
//    box.visibleProperty().bindBidirectional(model.newVersionAvailableProperty());
    box.setAlignment(Pos.CENTER);
    return box;
  }

  void unsubsribe() {
    if (mzmineIcon != null) {
      mzmineIcon.unsubscribe();
    }
    if (wizardIcon != null) {
      wizardIcon.unsubscribe();
    }
  }
}
