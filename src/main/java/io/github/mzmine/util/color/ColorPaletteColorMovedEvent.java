package io.github.mzmine.util.color;

import java.util.ArrayList;
import java.util.List;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.paint.Color;

public class ColorPaletteColorMovedEvent extends ListChangeListener.Change<Color> {

  protected final int oldIndex;
  protected final int newIndex;
  
  public ColorPaletteColorMovedEvent(ObservableList<Color> list, int oldIndex, int newIndex) {
    super(list);
    this.oldIndex = oldIndex;
    this.newIndex = newIndex;
  }

  @Override
  public boolean next() {
    return false;
  }

  @Override
  public void reset() {
  }

  @Override
  public int getFrom() {
    return oldIndex;
  }

  @Override
  public int getTo() {
    return newIndex;
  }

  @Override
  public List<Color> getRemoved() {
    return new ArrayList<Color>();
  }

  @Override
  protected int[] getPermutation() {
    return new int[0];
  }

}
