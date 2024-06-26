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

package io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution;

import io.github.mzmine.javafx.mvci.FxController;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import javafx.scene.layout.HBox;
import org.jetbrains.annotations.NotNull;

public class FeatureResolverUpdateController extends FxController<Object> {

  protected FeatureResolverUpdateController(@NotNull Object model) {
    super(model);
  }

  @Override
  protected @NotNull FxViewBuilder<Object> getViewBuilder() {
    return new Builder(model);
  }

  private class Builder extends FxViewBuilder {

    protected Builder(Object model) {
      super(model);
    }

    @Override
    public Object build() {
      return new HBox();
    }
  }
}
