/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

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
