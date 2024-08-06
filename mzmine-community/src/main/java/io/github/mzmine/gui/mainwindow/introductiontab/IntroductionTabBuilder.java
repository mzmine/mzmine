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

import static io.github.mzmine.javafx.components.util.FxLayout.newHBox;
import static io.github.mzmine.javafx.components.util.FxLayout.newScrollPane;
import static io.github.mzmine.javafx.components.util.FxLayout.newVBox;

import io.github.mzmine.gui.mainwindow.UsersTab;
import io.github.mzmine.javafx.components.animations.FxFlashingAnimation;
import io.github.mzmine.javafx.components.factories.FxButtons;
import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.javafx.util.FxIconUtil;
import io.github.mzmine.javafx.util.FxIcons;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.tools.batchwizard.BatchWizardTab;
import io.github.mzmine.util.javafx.LightAndDarkModeIcon;
import java.util.logging.Logger;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.controlsfx.control.ToggleSwitch;
import org.jetbrains.annotations.NotNull;

public class IntroductionTabBuilder extends FxViewBuilder<IntroductionTabModel> {

  private static final Logger logger = Logger.getLogger(IntroductionTabBuilder.class.getName());

  protected IntroductionTabBuilder(IntroductionTabModel model) {
    super(model);
  }

  @Override
  public Region build() {
    final VBox main = new VBox(40);
    main.setAlignment(Pos.CENTER);

    LightAndDarkModeIcon mzmineIcon = LightAndDarkModeIcon.mzmineImage(350, 200);

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

    var darkMode = createDarkModeButton();
    var root = FxLayout.newBorderPane(main);
    root.setRight(darkMode);

    return newScrollPane(root);
  }

  @NotNull
  private Region createDarkModeButton() {
    var darkModeSwitch = new ToggleSwitch("");
    var icon = FxIconUtil.getFontIcon(FxIcons.DARK_MODE_SWITCH, 26);
    darkModeSwitch.selectedProperty().bindBidirectional(model.isDarkModeProperty());
    return newVBox(Pos.TOP_RIGHT, newHBox(Pos.CENTER_RIGHT, darkModeSwitch, icon));
  }

  private Region createWizardRow() {
    final GridPane pane = new GridPane(20, 5);

    final HBox wizardImageWrapper = LightAndDarkModeIcon.mzwizardImage(300, 150);
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
        () -> MZmineCore.getDesktop().openWebPage("https://www.youtube.com/@mzioGmbH/playlists"));

    final ButtonBase btnWebsite = FxIconUtil.newIconButton(FxIcons.WEBSITE, 45, "mzmine website",
        () -> MZmineCore.getDesktop().openWebPage("https://mzio.io/#mzmine"));

    final ButtonBase btnUserManagement = FxIconUtil.newIconButton(FxIcons.USER, 45,
        "User management", UsersTab::showTab);
    FxFlashingAnimation.animate(btnUserManagement, model.needsUserLoginProperty());

    final ButtonBase btnDevelopment = FxIconUtil.newIconButton(FxIcons.DEVELOPMENT, 45,
        "Join the development", () -> MZmineCore.getDesktop()
            .openWebPage("https://mzmine.github.io/mzmine_documentation/contribute_intellij.html"));

    final ButtonBase btnDocs = FxIconUtil.newIconButton(FxIcons.BOOK, 45,
        "Open online documentation", () -> MZmineCore.getDesktop()
            .openWebPage("https://mzmine.github.io/mzmine_documentation/getting_started.html"));

    final ButtonBase btnPreferences = FxIconUtil.newIconButton(FxIcons.GEAR_PREFERENCES, 45,
        "Configure mzmine",
        () -> MZmineCore.getConfiguration().getPreferences().showSetupDialog(true));

    final ButtonBase btnWhatsNew = FxIconUtil.newIconButton(FxIcons.ROCKET, 45,
        "See what's new in mzmine",
        () -> MZmineCore.getDesktop().openWebPage("https://mzio.io/mzmine-news/"));

    FlowPane pane = new FlowPane(20, 20, btnPreferences, btnDocs, btnYoutube, btnWebsite,
        btnUserManagement, btnDevelopment, btnWhatsNew);
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
    final Button downloadButton = FxButtons.createButton(
        FxIconUtil.getFontIcon(FxIcons.DOWNLOAD, 60, Color.web("3391C1")),
        () -> MZmineCore.getDesktop()
            .openWebPage("https://github.com/mzmine/mzmine3/releases/latest"));
    box.getChildren().addAll(label, downloadButton);
//    box.visibleProperty().bindBidirectional(model.newVersionAvailableProperty());
    box.setAlignment(Pos.CENTER);
    return box;
  }

}
