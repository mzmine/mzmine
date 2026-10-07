/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.parameters.parametertypes.ionidentity;

import io.github.mzmine.datamodel.identities.iontype.IonLibrary;
import io.github.mzmine.javafx.components.factories.FxPopOvers;
import io.github.mzmine.javafx.dialogs.DialogLoggerUtil;
import io.github.mzmine.javafx.mvci.FxController;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.scene.control.ButtonBase;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Screen;
import org.controlsfx.control.PopOver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Controller for the popover that lets the user inspect the currently selected {@link IonLibrary}
 * and pick a different one from the list of global ion libraries.
 */
public class IonLibraryComponentPopover extends FxController<IonLibraryComponentPopoverModel> {

  private final IonLibraryComponentPopoverViewBuilder viewBuilder;
  private @Nullable PopOver popOver;

  public IonLibraryComponentPopover(@NotNull ReadOnlyListProperty<IonLibrary> libraries) {
    super(new IonLibraryComponentPopoverModel(libraries));
    viewBuilder = new IonLibraryComponentPopoverViewBuilder(model, this::hide);
  }

  /**
   * Bidirectional selection property – callers bind this to their own state.
   */
  public @NotNull ObjectProperty<@Nullable IonLibrary> selectedLibraryProperty() {
    return model.selectedLibraryProperty();
  }

  /**
   * Wire the popover to a button so it toggles on click.
   */
  public void installOn(@NotNull ButtonBase button) {
    FxPopOvers.install(button, getOrCreatePopOver());
  }

  public void hide() {
    if (popOver != null) {
      popOver.hide();
    }
  }

  @Override
  protected @NotNull FxViewBuilder<IonLibraryComponentPopoverModel> getViewBuilder() {
    return viewBuilder;
  }

  private PopOver getOrCreatePopOver() {
    if (popOver == null) {
      popOver = FxPopOvers.newPopOver(buildView());

      // Prevent popover from closing when Enter is pressed
      popOver.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
        if (event.getCode() == KeyCode.ENTER) {
          event.consume();
        }
      });

      popOver.setOnShown(_ -> {
        viewBuilder.syncListSelectionFromModel();
        viewBuilder.focusSearchField();

        final Screen screen = DialogLoggerUtil.getCurrentScreen(popOver);
        if (screen != null) {
          final double height = screen.getBounds().getHeight() / 3d;
          popOver.setMinHeight(Math.min(height, 400));
          popOver.setMaxHeight(height);
        }
      });
    }
    return popOver;
  }
}
