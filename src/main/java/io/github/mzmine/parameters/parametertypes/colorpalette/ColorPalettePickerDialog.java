/*
 * Copyright 2006-2020 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.parameters.parametertypes.colorpalette;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.color.SimpleColorPalette;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;

/**
 * Dialog to pick colors for a color palette.
 * 
 * @author SteffenHeu steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 *
 */
public class ColorPalettePickerDialog extends Stage {

  protected ExitCode exitCode;

  protected BorderPane pnSuper;
  protected BorderPane pnPicker;
  protected FlowPane pnButtons;
  protected ColorPalettePreviewField pnPalette;
  protected Button btnAccept;
  protected Button btnCancel;
  protected Button btnAddColor;
  protected Button btnRemoveColor;
  protected ColorPicker colorPicker;
  protected TextField txtName;

  protected SimpleColorPalette palette;
  protected int selected;

  public ColorPalettePickerDialog(@Nullable SimpleColorPalette palette) {
    super();

    exitCode = ExitCode.CANCEL;

    if (palette == null)
      palette = new SimpleColorPalette();
    this.palette = palette;

    // Create gui components
    pnSuper = new BorderPane();
    pnPicker = new BorderPane();
    pnPalette = new ColorPalettePreviewField(palette);
    pnButtons = new FlowPane();
    btnAccept = new Button("Accept");
    btnCancel = new Button("Cancel");
    btnAddColor = new Button("Add color");
    btnRemoveColor = new Button("Remove color");
    colorPicker = new ColorPicker();
    txtName = new TextField(palette.getName());

    // organize gui components
    pnSuper.setTop(pnPalette);
    pnSuper.setCenter(pnPicker);
    pnSuper.setBottom(pnButtons);
    pnPicker.setCenter(colorPicker);
    pnPicker.setBottom(new FlowPane(new Label("Name "), txtName));
    pnButtons.getChildren().addAll(btnAddColor, btnRemoveColor, new Separator(Orientation.VERTICAL),
        btnAccept, btnCancel);
    pnButtons.setAlignment(Pos.TOP_CENTER);

    // colorPicker.getStyleClass().add("split-button");
    colorPicker.setOnAction(e -> {
      if (colorPicker.getValue() != null) {
        int selected = pnPalette.getSelected();
        this.palette.set(selected, colorPicker.getValue());
        pnPalette.updatePreview();
      }
    });

    // set button actions
    btnAddColor.setOnAction(e -> btnAddColorAction());
    btnRemoveColor.setOnAction(e -> btnRemoveColorAction());
    btnAccept.setOnAction(e -> hideWindow(ExitCode.OK));
    btnCancel.setOnAction(e -> hideWindow(ExitCode.CANCEL));

    Scene scene = new Scene(pnSuper);
    setScene(scene);
  }

  private void btnAddColorAction() {
    palette.add(colorPicker.getValue());
    pnPalette.updatePreview();
  }

  private void btnRemoveColorAction() {
    palette.remove(pnPalette.getSelected());
    pnPalette.updatePreview();
  }

  private void hideWindow(ExitCode exitCode) {
    String name = txtName.getText();
    if(name == null)
      name = "unnamed";
    palette.setName(name);
    this.exitCode = exitCode;
    hide();
  }

  public ExitCode getExitCode() {
    return exitCode;
  }

  public @Nonnull SimpleColorPalette getPalette() {
    return palette;
  }
}
