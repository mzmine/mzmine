/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.gui.framework.fx.mvci;

import javafx.scene.layout.Region;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * MVCI controller with cached view. This is usually used when a view is expansive to create and may
 * be removed/added from/to the scene graph often. Otherwise, use the base implementation {@link FxController}
 */
public abstract class FxCachedViewController<ViewModelClass> extends FxController<ViewModelClass> {

  @Nullable
  protected Region cachedView;

  protected FxCachedViewController(@NotNull ViewModelClass model) {
    super(model);
  }

  /**
   * The cached view which is automatically created if not yet exists
   *
   * @return cached or new view
   */
  public @NotNull Region getCachedView() {
    if (cachedView == null) {
      return buildView();
    }
    return cachedView;
  }

  /**
   * Creates a view and sets the cached internal instance
   */
  public @NotNull Region buildView() {
    cachedView = super.buildView();
    return cachedView;
  }

  /**
   * Clears the internally cached view and returns the old view
   *
   * @return the old view
   */
  public @Nullable Region clearCachedView() {
    Region internalView = cachedView;
    cachedView = null;
    return internalView;
  }
}
