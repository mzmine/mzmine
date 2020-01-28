package io.github.mzmine.parameters.parametertypes.colorpalette;

import io.github.mzmine.util.color.ColorsFX;
import io.github.mzmine.util.color.Vision;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.FlowPane;

public class ColorPaletteComponent extends FlowPane {

  protected SimpleColorPalette value;
  protected ComboBox<SimpleColorPalette> box;
  protected Button addPalette;
//  protected ColorPicker picker;
  protected Button editPalette;
  protected Button deletePalette;

  public ColorPaletteComponent() {
    super();

    box = new ComboBox<>();
    box.setCellFactory(p -> {
      return new ColorPaletteCell(200, box.getPrefHeight());
    });
    
    box.getItems().add(new SimpleColorPalette(ColorsFX.getSevenColorPalette(Vision.NORMAL_VISION, true)));
    
    addPalette = new Button("New palette");
    addPalette.setOnAction(e -> {
      SimpleColorPalette pal = new SimpleColorPalette();
      box.getItems().add(pal);
      box.getSelectionModel().select(box.getItems().indexOf(pal));
    });
    
    editPalette = new Button("Edit");
    
    deletePalette = new Button("Delete");
    
    this.getChildren().addAll(box, addPalette, editPalette, deletePalette);
  }

  public SimpleColorPalette getValue() {
    return box.getValue();
  }

  public void setValue(SimpleColorPalette value) {
    box.getItems().add(value);
    box.setValue(value);
  }

}

