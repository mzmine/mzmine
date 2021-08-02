/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.parameters.parametertypes.paintscale;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.parametertypes.colorpalette.ColorPalettePreviewField;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.color.SimpleColorPalette;
import java.util.Objects;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Dialog to pick colors for a color palette.
 *
 * @author SteffenHeu steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 */
public class PaintScalePalettePickerDialog extends Stage {

  protected ExitCode exitCode;

  protected BorderPane pnMain;
  protected ScrollPane pnWrapParam;
  protected GridPane pnParam;
  protected ButtonBar pnButtons;
  protected ColorPalettePreviewField pnPalette;
  protected Button btnAccept;
  protected Button btnCancel;
  protected Button btnAddColor;
  protected Button btnRemoveColor;
  protected ColorPicker colorPickerPalette;
  protected TextField txtName;

  protected SimpleColorPalette palette;
  protected int selected;

  public PaintScalePalettePickerDialog(@Nullable SimpleColorPalette palette) {
    super();

    pnMain = new BorderPane();
    pnWrapParam = new ScrollPane();
    pnButtons = new ButtonBar();

    pnWrapParam.setPadding(new Insets(10.0));

    if (palette == null) {
      palette = new SimpleColorPalette();
    }
    setTitle("Editing of color palette " + Objects.requireNonNullElse(palette.getName(), ""));

    Scene scene = new Scene(pnMain);
    setScene(scene);
    scene.getStylesheets()
        .addAll(MZmineCore.getDesktop().getMainWindow().getScene().getStylesheets());

    exitCode = ExitCode.CANCEL;

    this.palette = palette;
    selected = 0;

    // Create gui components
    pnParam = new GridPane();
    pnPalette = new ColorPalettePreviewField(palette);
    btnAccept = new Button("OK");
    btnCancel = new Button("Cancel");
    btnAddColor = new Button("Add");
    btnRemoveColor = new Button("Remove");
    colorPickerPalette = new ColorPicker();
    txtName = new TextField(palette.getName());
    txtName.setMaxWidth(250);

    // organize gui components
    pnParam.add(new Label("Name"), 0, 0);
    pnParam.add(txtName, 1, 0, 4, 1);

    pnParam.add(new Label("Palette"), 0, 1);
    pnParam.add(pnPalette, 1, 1, 4, 1);

    pnParam.add(new Label("Color"), 0, 2);
    pnParam.add(colorPickerPalette, 1, 2, 1, 1);
    pnParam.add(btnAddColor, 3, 2);
    pnParam.add(btnRemoveColor, 4, 2);

    pnButtons.getButtons().add(btnCancel);
    pnButtons.getButtons().add(btnAccept);
    pnButtons.setPadding(new Insets(10.0));

    // color picker actions
    colorPickerPalette.setOnAction(e -> {
      if (colorPickerPalette.getValue() != null) {
        int selected = pnPalette.getSelected();
        if (selected >= 0 && selected < this.palette.size()) {
          this.palette.set(selected, colorPickerPalette.getValue());
        }
      }
    });
    colorPickerPalette.setValue(palette.get(pnPalette.getSelected()));
    colorPickerPalette.getCustomColors()
        .addAll(MZmineCore.getConfiguration().getDefaultColorPalette());

    pnPalette.addListener((Color newColor, int newIndex) -> colorPickerPalette.setValue(newColor));

    // set button actions
    btnAddColor.setOnAction(e -> btnAddColorAction());
    btnRemoveColor.setOnAction(e -> btnRemoveColorAction());
    btnAccept.setOnAction(e -> hideWindow(ExitCode.OK));
    btnCancel.setOnAction(e -> hideWindow(ExitCode.CANCEL));

    // add panels together
    pnWrapParam.setContent(pnParam);
    pnMain.setCenter(pnWrapParam);
    pnParam.getChildren().forEach(c -> GridPane.setMargin(c, new Insets(5.0, 0.0, 5.0, 5.0)));
    pnMain.setBottom(pnButtons);

    // size is computed when shown, so show here and set minimum size to the computed one
    show();
    this.setMinHeight(getHeight() + 20);
    this.setMinWidth(getWidth() + 20);
  }

  private void btnAddColorAction() {
    if (palette.isEmpty()) {
      this.setHeight(this.getHeight() + 17);
    }
    palette.add(colorPickerPalette.getValue());
//    pnPalette.updatePreview();
  }

  private void btnRemoveColorAction() {
    if (!palette.isEmpty()) {
      palette.remove(pnPalette.getSelected());
    }
  }

  private void hideWindow(ExitCode exitCode) {
    if (exitCode == ExitCode.CANCEL) {
      hide();
      this.exitCode = exitCode;
      return;
    }

    if (!palette.isValid()) {
      MZmineCore.getDesktop().displayErrorMessage("Current color palette is not valid.\n"
          + "Does it contain enough colors? The minimum amount of colors is 3.");
      return;
    }

    String name = txtName.getText();
    if (name == null || name.equals("") || name.replaceAll("\\s+", "").equals("")) {
      MZmineCore.getDesktop().displayErrorMessage("Please set a name for the color palette.");
      return;
    }
    palette.setName(name);
    this.exitCode = exitCode;
    hide();
  }

  public ExitCode getExitCode() {
    return exitCode;
  }

  public @NotNull
  SimpleColorPalette getPalette() {
    return palette;
  }
}
