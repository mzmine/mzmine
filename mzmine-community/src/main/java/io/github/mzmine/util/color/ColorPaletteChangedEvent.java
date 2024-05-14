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

package io.github.mzmine.util.color;

import java.util.List;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.Nullable;

public class ColorPaletteChangedEvent extends ListChangeListener.Change<Color> {

  protected ColorPaletteChangeEventType type;

  protected final int oldIndex;
  protected final int newIndex;

  protected final Color newColor;

  /**
   * Constructs a new Change instance on the given list.
   *
   * @param list The list that was changed
   */
  public ColorPaletteChangedEvent(ObservableList<Color> list, int oldIndex, int newIndex) {
    super(list);
    type = ColorPaletteChangeEventType.LIST_PERMUTED;
    this.newIndex = newIndex;
    this.oldIndex = oldIndex;
    newColor = null;
  }

  /**
   * @param list
   * @param type
   * @param color
   */
  protected ColorPaletteChangedEvent(ObservableList<Color> list, ColorPaletteChangeEventType type,
      Color color) {
    super(list);
    this.type = type;
    this.newIndex = 0;
    this.oldIndex = 0;
    this.newColor = color;
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
    return null;
  }

  @Override
  protected int[] getPermutation() {
    return new int[0];
  }

  /**
   * @return The new color if a color was updated/added, null if no color was added/updated.
   */
  @Nullable
  public Color getNewColor() {
    return this.newColor;
  }

  /**
   *
   * @return The event type. See {@link ColorPaletteChangeEventType}
   */
  public ColorPaletteChangeEventType getType() {
    return type;
  }
}
