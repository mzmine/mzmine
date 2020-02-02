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

import java.util.List;
import java.util.logging.Logger;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.color.ColorsFX;
import io.github.mzmine.util.color.SimpleColorPalette;
import io.github.mzmine.util.color.Vision;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.FlowPane;

/**
 * Gui component for a SimpleColorPalette. Allows editing and selection of different palettes.
 * 
 * @author SteffenHeu steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 *
 */
public class ColorPaletteComponent extends FlowPane {

  private static final Logger logger = Logger.getLogger(ColorPaletteComponent.class.getName());

  protected ComboBox<SimpleColorPalette> box;
  protected Button addPalette;
  protected Button editPalette;
  protected Button deletePalette;

  public ColorPaletteComponent() {
    super();

    box = new ComboBox<>();
    box.setMinWidth(200);
    box.setCellFactory(p -> {
      return new ColorPaletteCell(17);
    });
    box.setButtonCell(new ColorPaletteCell(15));
    box.setMaxHeight(35);

    addPalette = new Button("New palette");
    addPalette.setOnAction(e -> {
      SimpleColorPalette pal;
      if(!itemsContainDefaultPalette()) {
        pal = new SimpleColorPalette(ColorsFX.getSevenColorPalette(Vision.DEUTERANOPIA, true));
        pal.setName("Deuternopia");
      }
      else
        pal = new SimpleColorPalette();
      box.getItems().add(pal);
      box.getSelectionModel().select(box.getItems().indexOf(pal));
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
      box.getItems().remove(box.getValue());
      box.setValue(box.getItems().get(0));
    });

    this.getChildren().addAll(box, addPalette, editPalette, deletePalette);
  }

  public SimpleColorPalette getValue() {
    return box.getValue();
  }

  public void setValue(SimpleColorPalette value) {
    if (box.getItems().indexOf(value) == -1)
      logger.warning("Value of ColorPaletteComponent was set to a value not contained "
          + "in the items. This might lead to unexpected behaviour.");
    box.setValue(value);
    box.autosize();
  }

  public List<SimpleColorPalette> getPalettes() {
    return box.getItems();
  }

  public void setPalettes(List<SimpleColorPalette> list) {
    if (list.isEmpty())
      return;

    box.getItems().clear();
    for (SimpleColorPalette p : list)
      box.getItems().add(p);
  }

  protected boolean itemsContainDefaultPalette() {
    List<SimpleColorPalette> items = box.getItems();
    SimpleColorPalette def = new SimpleColorPalette(
        ColorsFX.getSevenColorPalette(Vision.DEUTERANOPIA, true));
    for(SimpleColorPalette p : items)
      if(p.equals(def))
        return true;
    
    return false;
  }
}

