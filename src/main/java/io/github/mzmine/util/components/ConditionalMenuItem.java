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

package io.github.mzmine.util.components;

import java.util.function.BooleanSupplier;
import javafx.scene.Node;
import javafx.scene.control.MenuItem;
import org.jetbrains.annotations.NotNull;

public class ConditionalMenuItem extends MenuItem {

  private final BooleanSupplier enableCondition;

  public ConditionalMenuItem(@NotNull BooleanSupplier enableCondition) {
    super();
    this.enableCondition = enableCondition;
//    setDisable(!enableCondition.getAsBoolean());
  }

  public ConditionalMenuItem(String text, @NotNull BooleanSupplier enableCondition) {
    this(enableCondition);
    setText(text);
  }

  public ConditionalMenuItem(String text, Node graphic, @NotNull BooleanSupplier enableCondition) {
    this(text, enableCondition);
    setGraphic(graphic);
  }

  public void updateVisibility() {
    setDisable(!enableCondition.getAsBoolean());
  }
}
