package io.github.mzmine.parameters.parametertypes.colorpalette;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ListCell;
import javafx.scene.layout.FlowPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * Implementation of ListCell to display color palettes and select between them.
 * 
 * @author SteffenHeu steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 *
 */
public class ColorPaletteCell extends ListCell<SimpleColorPalette> {

  private final double height;
  private final List<Rectangle> rects;
  private final FlowPane pane;

  /**
   * 
   * @param w The width of the combo box.
   * @param h The height of the combo box.
   */
  public ColorPaletteCell(double w, double h) {
    super();
    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
    pane = new FlowPane();
    pane.setMaxSize(w, h);
    height = h;
    rects = new ArrayList<Rectangle>();
//    if (palette != null) {
//      setRectangles(palette);
//      pane.getChildren().clear();
//      pane.getChildren().addAll(rects);
//    }
  }

  private void setRectangles(@Nullable SimpleColorPalette palette) {
    rects.clear();

    if (palette == null)
      return;

    for (Color clr : palette) {
      System.out.println("rectangle color" + clr.toString());
      rects.add(new Rectangle(height, height, clr));
    }
  }

  @Override
  protected void updateItem(@Nullable SimpleColorPalette palette, boolean empty) {
    super.updateItem(palette, empty);

    if (palette == null || empty) {
      setGraphic(null);
      System.out.println("palette null");
    }
    else {
      setRectangles(palette);
      pane.getChildren().clear();
      pane.getChildren().addAll(rects);
      System.out.println("updated");
    }
  }
};


/*
 * ComboBox<Color> cmb = new ComboBox<>(); cmb.getItems().addAll( Color.RED, Color.GREEN,
 * Color.BLUE);
 * 
 * cmb.setCellFactory(p -> { return new ListCell<>() { private final Rectangle rectangle; {
 * setContentDisplay(ContentDisplay.GRAPHIC_ONLY); rectangle = new Rectangle(10, 10); }
 * 
 * @Override protected void updateItem(Color item, boolean empty) { super.updateItem(item, empty);
 * 
 * if (item == null || empty) { setGraphic(null); } else { rectangle.setFill(item);
 * setGraphic(rectangle); } } }; });
 */
