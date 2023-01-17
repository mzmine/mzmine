/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.parameters.parametertypes.paintscale;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.color.ColorsFX;
import io.github.mzmine.util.color.SimpleColorPalette;
import io.github.mzmine.util.color.Vision;
import java.util.List;
import java.util.logging.Logger;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;

/**
 * Gui component for a SimpleColorPalette. Allows editing and selection of different palettes.
 *
 * @author SteffenHeu steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 */
public class PaintScalePaletteComponent extends GridPane {

  private static final Logger logger = Logger.getLogger(PaintScalePaletteComponent.class.getName());

  protected final ComboBox<SimpleColorPalette> box;
  protected final Button btnAddPalette;
  protected final Button btnEditPalette;
  protected final Button btnDeletePalette;
  protected final Button duplicatePalette;
  protected final FlowPane pnButtons;

  public PaintScalePaletteComponent() {
    super();

    box = new ComboBox<>();
    box.setMinWidth(200);
    box.setCellFactory(p -> new PaintScalePaletteCell(17));
    box.setButtonCell(new PaintScalePaletteCell(15));
    box.setMinHeight(37);
    box.setMaxHeight(37);

    btnAddPalette = new Button("New");
    btnAddPalette.setOnAction(e -> {
      SimpleColorPalette pal = new SimpleColorPalette();
      pal.setName("New palette");
      pal.add(Color.BLACK);
      addPalette(pal);
      setValue(pal);
    });

    duplicatePalette = new Button("Duplicate");
    duplicatePalette.setOnAction(e -> {
      SimpleColorPalette pal = box.getValue();
      if (pal == null) {
        logger.warning("No color palette selected. Cannot duplicate.");
        return;
      }

      SimpleColorPalette newPal = pal.clone();
      newPal.setName(pal.getName() + " (copy)");
      if (addPalette(newPal)) {
        box.setValue(newPal);
      }

    });

    btnEditPalette = new Button("Edit");
    btnEditPalette.setOnAction(e -> {
      if (SimpleColorPalette.DEFAULT_PAINT_SCALES.contains(box.getValue())) {
        MZmineCore.getDesktop().displayErrorMessage(
            "Cannot edit default palette.\n" + "Duplicate this palette first.");
        return;
      }

      PaintScalePalettePickerDialog d = new PaintScalePalettePickerDialog(box.getValue().clone());
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

    btnDeletePalette = new Button("Delete");
    btnDeletePalette.setOnAction(e -> {
      if (SimpleColorPalette.DEFAULT_PAINT_SCALES.contains(box.getValue())) {
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
        .addAll(btnAddPalette, duplicatePalette, btnEditPalette, btnDeletePalette);

    add(box, 0, 0);
    add(pnButtons, 0, 1);

//    getChildren().forEach(c -> GridPane.setMargin(c, new Insets(5.0, 0.0, 5.0, 0.0)));
    pnButtons.getChildren().forEach(c -> FlowPane.setMargin(c, new Insets(5.0, 5.0, 0.0, 0.0)));
  }

  public SimpleColorPalette getValue() {
    return box.getValue();
  }

  public void setValue(SimpleColorPalette value) {
    if (box.getItems().indexOf(value) == -1) {
      logger.warning("Value of PaintScalePaletteComponent was set to a value not contained "
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

  /**
   * Palettes should be added via this method and not via {@link ComboBox#getItems#add()}, since
   * this method provides additional checks for duplicates.
   *
   * @param pal
   * @return
   */
  public boolean addPalette(SimpleColorPalette pal) {
    if (box.getItems().contains(pal)) {
      logger.fine("Cannot add duplicates. A palette with same name and colors already exists.");
      return false;
    }

    return box.getItems().add(pal);
  }
}

