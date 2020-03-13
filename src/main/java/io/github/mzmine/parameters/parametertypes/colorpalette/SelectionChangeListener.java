package io.github.mzmine.parameters.parametertypes.colorpalette;

import java.util.EventListener;
import javafx.scene.paint.Color;

public interface SelectionChangeListener extends EventListener {

  void selectionChanged(Color newColor, int newIndex);
}
