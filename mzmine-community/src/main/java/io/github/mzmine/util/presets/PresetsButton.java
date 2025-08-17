/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.util.presets;

import io.github.mzmine.javafx.components.factories.FxButtons;
import io.github.mzmine.javafx.dialogs.FilterableMenuPopup;
import io.github.mzmine.javafx.util.FxIconUtil;
import io.github.mzmine.javafx.util.FxIcons;
import io.github.mzmine.util.StringUtils;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.layout.StackPane;
import org.jetbrains.annotations.NotNull;

/**
 * Preset button that shows a menu with fixed items like save, load; and items for presets.
 * <p>
 * Presets are stored by {@link PresetStore}
 *
 * @param <T>
 */
public class PresetsButton<T extends Preset> extends StackPane {

  // handles storage in files
  private final PresetStore<T> presetStore;
  private final Function<String, T> presetNameFactory;
  private final FilterableMenuPopup<T> popup;
  private final boolean showText;


  public PresetsButton(boolean showText, @NotNull PresetStore<T> presetStore,
      @NotNull Function<String, T> presetNameFactory, Consumer<T> onClick) {
    this(showText, FxIcons.GROUPED_PARAMETERS, presetStore, presetNameFactory, onClick);
  }

  public PresetsButton(boolean showText, FxIcons fxIcons, @NotNull PresetStore<T> presetStore,
      @NotNull Function<String, T> presetNameFactory, Consumer<T> onClick) {
    this.presetStore = presetStore;
    this.presetNameFactory = presetNameFactory;
    this.showText = showText;

    final Button[] buttons = new Button[]{
        FxButtons.createButton("Save preset", FxIcons.SAVE, "Save preset to .mzmine/presets folder",
            this::showSaveDialog),
        FxButtons.createButton("Load presets", FxIcons.LOAD, "Load presets from file",
            presetStore::loadPresetsInDialog) //
    };

    popup = new FilterableMenuPopup<>(presetStore.getCurrentPresets(), true, buttons) {
      @Override
      public void onItemActivated(@NotNull T item) {
        onClick.accept(item);
      }

      @Override
      public @NotNull Predicate<T> createPredicate(String searchText) {
        return StringUtils.allWordsSubMatchPredicate(searchText, T::toString);
      }
    };

    // start by loading all presets or defaults
    presetStore.loadAllPresetsOrDefaults();

    final ButtonBase presetButton;
    if (showText) {
      presetButton = FxButtons.createButton("Presets", fxIcons, "Manage presets", this::showMenu);
    } else {
      presetButton = FxIconUtil.newIconButton(fxIcons, "Manage presets", this::showMenu);
    }
    getChildren().add(presetButton);
  }

  private void showSaveDialog() {
    // create a preset to see if saving is valid
    final T testPreset = presetNameFactory.apply("placeholder");
    if (testPreset == null) {
      return;
    }
    // rename the test preset. this way we know the current value is not null
    presetStore.showSaveDialog(testPreset::withName);
  }

  public void showMenu() {
    showMenu(this);
  }

  public void showMenu(@NotNull Node parent) {
    final FilterableMenuPopup<T> pop = popup;
    if (pop == null) {
      return;
    }
    if (pop.isShowing()) {
      pop.hide();
    } else {
      pop.show(parent);
    }
  }

}
