/*
 * Copyright 2006-2022 The MZmine Development Team
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
 *
 */

package io.github.mzmine.modules.tools.fraggraphdashboard.nodetable;

import java.util.function.Function;
import javafx.beans.property.BooleanProperty;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableCell;

public class CheckTreeCell<S, T> extends TableCell<S, T> {

  private final CheckBox box;

  public CheckTreeCell(Function<T, BooleanProperty> propertyGetter) {
    box = new CheckBox();
    setGraphic(box);

    itemProperty().addListener((_, old, n) -> {
      if (old != null) {
        box.selectedProperty().unbindBidirectional(propertyGetter.apply(old));
      }
      if(n != null) {
        box.selectedProperty().set(propertyGetter.apply(n).get());
        box.selectedProperty().bindBidirectional(propertyGetter.apply(n));
      }
    });
  }
}
