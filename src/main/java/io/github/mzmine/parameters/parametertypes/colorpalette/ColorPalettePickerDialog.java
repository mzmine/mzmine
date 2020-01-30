package io.github.mzmine.parameters.parametertypes.colorpalette;

import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;

public class ColorPalettePickerDialog extends Stage {

  protected BorderPane pnSuper;
  protected BorderPane pnPicker;
  protected FlowPane pnPalette;
  protected FlowPane pnButtons;
  protected Button btnAccept;
  protected Button btnCancel;
  protected Button btnAddColor;
  protected Button btnRemoveColor;
  
  protected ColorPicker colorPicker;

  protected SimpleColorPalette palette;
  protected int selected;

  public ColorPalettePickerDialog(SimpleColorPalette palette) {
    super();

    if (palette == null)
      palette = new SimpleColorPalette();
    this.palette = palette;

    // Create gui components
    pnSuper = new BorderPane();
    pnPicker = new BorderPane();
    pnPalette = new FlowPane();
    pnButtons = new FlowPane();
    btnAccept = new Button("Accept");
    btnCancel = new Button("Cancel");
    btnAddColor = new Button("Add color");
    btnRemoveColor = new Button("Remove color");
    colorPicker = new ColorPicker();
    
    // organize gui components
    pnSuper.setCenter(pnPicker);
    pnSuper.setBottom(pnButtons);
    pnPicker.setBottom(pnButtons);
    pnPicker.setCenter(colorPicker);
    pnButtons.getChildren().addAll(btnAddColor, btnRemoveColor, new Separator(Orientation.VERTICAL),
        btnAccept, btnCancel);
    
    colorPicker.getStyleClass().add("split-button");
    
    Scene scene = new Scene(pnSuper);
    setScene(scene);
  }



}
