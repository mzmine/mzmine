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

package io.github.mzmine.util.color;

import java.util.List;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.paint.Color;
import javax.annotation.Nullable;

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
