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

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.color.ColorsFX;
import io.github.mzmine.util.color.SimpleColorPalette;
import io.github.mzmine.util.color.Vision;
import java.util.List;
import java.util.logging.Logger;
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

    addPalette = new Button("New");
    addPalette.setOnAction(e -> {
      SimpleColorPalette pal = new SimpleColorPalette();
      box.getItems().add(pal);
    });

    duplicatePalette = new Button("Duplicate");
    duplicatePalette.setOnAction(e -> {
      SimpleColorPalette pal = box.getValue();
      if (pal == null) {
        logger.warning("No color palette selected. Cannot duplicate.");
        return;
      }

      SimpleColorPalette newPal = pal.clone();
      newPal.setName(pal.getName() + " (cpy)");
      if (addPalette(newPal)) {
        box.setValue(newPal);
      }

    });

    editPalette = new Button("Edit");
    editPalette.setOnAction(e -> {
      if (box.getValue().equals(ColorPaletteParameter.DEFAULT_DEUTERNOPIA)) {
        MZmineCore.getDesktop().displayErrorMessage("Cannot edit default palette.");
        return;
      }

      ColorPalettePickerDialog d = new ColorPalettePickerDialog(box.getValue().clone());
      d.show();

      d.setOnHiding(f -> {
        if (d.getExitCode() == ExitCode.OK) {
          // remove old
          SimpleColorPalette oldVal = box.getValue();
          box.getItems().remove(oldVal);

          // check for existing duplicates, if present add old value again
          SimpleColorPalette newVal = d.getPalette();
          if (!addPalette(newVal)) {
            addPalette(oldVal);
            setValue(oldVal);
            d.show();
            MZmineCore.getDesktop().displayErrorMessage(
                "Cannot add duplicates. Palette with same name and colors already exists.");
//          return;
          }

          setValue(newVal);
        }
      });
    });

    deletePalette = new Button("Delete");
    deletePalette.setOnAction(e -> {
      if (box.getValue().equals(ColorPaletteParameter.DEFAULT_DEUTERNOPIA)) {
        MZmineCore.getDesktop().displayErrorMessage("Cannot delete default palette.");
        return;
      }

      if (box.getItems().size() <= 1) {
        logger.warning(
            "Cannot remove palettes. Only 1 palette present. Please add a new palette first.");
        return;
      }
      box.getItems().remove(box.getValue());
      box.setValue(box.getItems().get(0));
    });

    pnButtons = new FlowPane();
    pnButtons.getChildren()
        .addAll(addPalette, duplicatePalette, editPalette, deletePalette);

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

  public boolean addPalette(SimpleColorPalette pal) {
    if (box.getItems().contains(pal)) {
      logger.fine("Cannot add duplicates. A palette with same name and colors already exists.");
      return false;
    }

    return box.getItems().add(pal);
  }
}

