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

import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.color.ColorsFX;
import io.github.mzmine.util.color.SimpleColorPalette;
import io.github.mzmine.util.color.Vision;
import java.util.List;
import java.util.logging.Logger;
import javafx.collections.ListChangeListener;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;

/**
 * Gui component for a SimpleColorPalette. Allows editing and selection of different palettes.
 *
 * @author SteffenHeu steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 */
public class ColorPaletteComponent extends GridPane {

  private static final Logger logger = Logger.getLogger(ColorPaletteComponent.class.getName());

  protected ComboBox<SimpleColorPalette> box;
  protected Button addPalette;
  protected Button editPalette;
  protected Button deletePalette;
  protected Button duplicatePalette;
  protected Button addDefault;
  protected FlowPane pnButtons;

  public ColorPaletteComponent() {
    super();

    box = new ComboBox<>();
    box.setMinWidth(200);
    box.setCellFactory(p -> {
      return new ColorPaletteCell(17);
    });
    box.setButtonCell(new ColorPaletteCell(15));
    box.setMinHeight(35);
    box.setMaxHeight(35);

    box.getItems().addListener((ListChangeListener<? super SimpleColorPalette>) e ->
        logger.info("Item added" + e.toString()));

    box.valueProperty().addListener(
        (observable, oldValue, newValue) -> logger.info("value " + newValue.toString()));

    addPalette = new Button("New");
    addPalette.setOnAction(e -> {
      SimpleColorPalette pal = new SimpleColorPalette();
      box.getItems().add(pal);
//      box.getSelectionModel().select(box.getItems().indexOf(pal));
//      box.setValue(pal);
    });

    duplicatePalette = new Button("Duplicate");
    duplicatePalette.setOnAction(e -> {
      SimpleColorPalette pal = box.getValue();
      if (pal == null) {
        logger.warning("No color palette selected. Cannot duplicate.");
        return;
      }

      SimpleColorPalette newPal = pal.clone();
      box.getItems().add(newPal);

      logger.info("index of new value: " + box.getItems().indexOf(newPal));
      logger.info("hash - old: " + pal.hashCode() + " new: " + newPal.hashCode());

//      box.setValue(newPal);
    });

    editPalette = new Button("Edit");
    editPalette.setOnAction(e -> {
      ColorPalettePickerDialog d = new ColorPalettePickerDialog(box.getValue().clone());
      d.show();

      d.setOnHiding(f -> {
        if (d.getExitCode() == ExitCode.OK) {
          box.getItems().remove(box.getValue());
          SimpleColorPalette newVal = d.getPalette();
          box.getItems().add(newVal);
          setValue(newVal);
        }
      });
    });

    deletePalette = new Button("Delete");
    deletePalette.setOnAction(e -> {
      if (box.getItems().size() <= 1) {
        logger.warning(
            "Cannot remove palettes. Only 1 palette present. Please add a new palette first.");
        return;
      }
      box.getItems().remove(box.getValue());
      box.setValue(box.getItems().get(0));
    });

    addDefault = new Button("Add default");
    addDefault.setOnAction(e -> {
      SimpleColorPalette pal;
      pal = new SimpleColorPalette(ColorsFX.getSevenColorPalette(Vision.DEUTERANOPIA, true));
      pal.setName("Deuternopia");
      box.getItems().add(pal);
//      setValue(pal);
    });

    pnButtons = new FlowPane();
    pnButtons.getChildren()
        .addAll(addPalette, duplicatePalette, editPalette, deletePalette, addDefault);

    add(box, 0, 0);
    add(pnButtons, 0, 1);
  }

  public SimpleColorPalette getValue() {
    return box.getValue();
  }

  public void setValue(SimpleColorPalette value) {
    if (box.getItems().indexOf(value) == -1) {
      logger.warning("Value of ColorPaletteComponent was set to a value not contained "
          + "in the items. This might lead to unexpected behaviour.");
    }
    box.setValue(value);
    box.autosize();
  }

  public List<SimpleColorPalette> getPalettes() {
    return box.getItems();
  }

  public void setPalettes(List<SimpleColorPalette> list) {
    if (list.isEmpty()) {
      return;
    }

    box.getItems().clear();
    for (SimpleColorPalette p : list) {
      box.getItems().add(p);
    }
  }

  protected boolean itemsContainDefaultPalette() {
    List<SimpleColorPalette> items = box.getItems();
    SimpleColorPalette def = new SimpleColorPalette(
        ColorsFX.getSevenColorPalette(Vision.DEUTERANOPIA, true));
    for (SimpleColorPalette p : items) {
      if (p.equals(def)) {
        return true;
      }
    }

    return false;
  }
}

