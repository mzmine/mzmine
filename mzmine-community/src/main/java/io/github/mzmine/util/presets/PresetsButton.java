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

import static java.util.Objects.requireNonNullElse;

import io.github.mzmine.javafx.components.factories.FxTextFields;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.util.FxIconUtil;
import io.github.mzmine.javafx.util.FxIcons;
import io.github.mzmine.util.StringUtils;
import io.github.mzmine.util.javafx.FxMenuUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Logger;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
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

  private static final Logger logger = Logger.getLogger(PresetsButton.class.getName());
  private final BooleanProperty saveReady = new SimpleBooleanProperty(false);
  private final ObservableList<MenuItem> fixedItems = FXCollections.observableArrayList();
  private final ObservableList<PresetMenuItem<T>> presetItems = FXCollections.observableArrayList();
  private final StringProperty searchText = new SimpleStringProperty();
  private final TextField searchField;
  private final ContextMenu contextMenu;
  // handles storage in files
  private final PresetStore<T> presetStore;
  private final Function<String, T> presetNameFactory;
  private final Consumer<T> onClick;


  public PresetsButton(@NotNull PresetStore<T> presetStore,
      @NotNull Function<String, T> presetNameFactory, Consumer<T> onClick) {
    this(FxIcons.GROUPED_PARAMETERS, presetStore, presetNameFactory, onClick);
  }

  public PresetsButton(FxIcons fxIcons, @NotNull PresetStore<T> presetStore,
      @NotNull Function<String, T> presetNameFactory, Consumer<T> onClick) {
    this.presetStore = presetStore;
    this.presetNameFactory = presetNameFactory;
    this.onClick = onClick;

    searchField = FxTextFields.newTextField(15, searchText, "Search...", null);

    final SortedList<PresetMenuItem<T>> sortedList = new SortedList<>(presetItems,
        Comparator.comparing(PresetMenuItem::getText));

    final FilteredList<PresetMenuItem<T>> filteredItems = new FilteredList<>(sortedList);

    // property.map somehow did not work
    searchText.subscribe((_, nv) -> {
      filteredItems.setPredicate(createPredicate(nv));
    });

    contextMenu = new ContextMenu();
    createFixedMenu();

    // update list in case presets change
    filteredItems.addListener((ListChangeListener<? super PresetMenuItem<T>>) (_) -> {
      final ObservableList<MenuItem> items = contextMenu.getItems();
      List<MenuItem> combined = new ArrayList<>(fixedItems.size() + filteredItems.size());
      combined.addAll(fixedItems);
      combined.addAll(filteredItems);
      // set in one go
      items.setAll(combined);
    });

    presetStore.getCurrentPresets().addListener((ListChangeListener<? super T>) (_) -> {
      final List<PresetMenuItem<T>> items = presetStore.getCurrentPresets().stream()
          .map(this::createPresetItem).toList();
      presetItems.setAll(items);
    });

    // start by loading all presets or defaults
    presetStore.loadAllPresetsOrDefaults();

    final ButtonBase presetButton = FxIconUtil.newIconButton(fxIcons, "Manage presets");
    presetButton.setOnAction(_ -> showMenu());
    presetButton.setContextMenu(contextMenu);
    getChildren().add(presetButton);
  }

  private Predicate<MenuItem> createPredicate(String searchText) {
    return StringUtils.allWordsSubMatchPredicate(searchText, MenuItem::getText);
  }

  private void createFixedMenu() {
    fixedItems.addAll( //
        FxMenuUtil.newMenuItem(FxLayout.newStackPane(searchField)), //
        FxMenuUtil.newMenuItem("Save preset", this::showSaveDialog), //
        FxMenuUtil.newMenuItem("Load presets", presetStore::loadPresetsInDialog), //
        // TODO later add dialog to manage presets like delete old redefine etc
//    FxMenuUtil.newMenuItem("Manage preset", this::managePreset), //
        new SeparatorMenuItem() //
    );
  }

  private void showSaveDialog() {
    // create a preset to see if saving is valid
    final T testPreset = presetNameFactory.apply("placeholder");
    if (testPreset == null) {
      return;
    }
    presetStore.showSaveDialog(name -> testPreset.withName(name));
  }

  private PresetMenuItem<T> createPresetItem(T preset) {
    final PresetMenuItem<T> menuItem = new PresetMenuItem<>(preset);
    menuItem.setUserData(preset); // needed for sorting
    menuItem.setOnAction(e -> {
      onClick.accept(preset);
    });
    return menuItem;
  }


  public void showMenu() {
    showMenu(this);
  }

  public void showMenu(@NotNull Node parent) {
    final Bounds boundsInScreen = parent.localToScreen(parent.getBoundsInLocal());
    contextMenu.show(parent, boundsInScreen.getCenterX(), boundsInScreen.getCenterY());
    searchField.requestFocus();
  }

  public ContextMenu getContextMenu() {
    return contextMenu;
  }

  public void setSearchText(String searchText) {
    this.searchText.set(requireNonNullElse(searchText, ""));
  }

  public ObservableList<MenuItem> getFixedItems() {
    return fixedItems;
  }

  public boolean isSaveReady() {
    return saveReady.get();
  }

  public BooleanProperty saveReadyProperty() {
    return saveReady;
  }

  private static final class PresetMenuItem<T> extends MenuItem {

    private final T filter;

    private PresetMenuItem(T filter) {
      super(filter.toString());
      this.filter = filter;
      setUserData(filter);
    }

    public T getFilter() {
      return filter;
    }
  }
}
