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

package io.github.mzmine.util.presets.manage;

import static java.util.Objects.requireNonNullElse;

import io.github.mzmine.javafx.properties.PropertyUtils;
import io.github.mzmine.util.presets.Preset;
import java.util.logging.Logger;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Properties are not bound to the preset but initialized with its values. On change the modified
 * property will be true.
 */
public class EditPresetModel {

  private static final Logger logger = Logger.getLogger(EditPresetModel.class.getName());
  private final ObjectProperty<@NotNull Preset> originalPreset = new SimpleObjectProperty<>();
  /**
   * Editor may modify this preset
   */
  private final ObjectProperty<@Nullable Preset> modifiedPreset = new SimpleObjectProperty<>();

  private final StringProperty name = new SimpleStringProperty();
  private final BooleanProperty modified = new SimpleBooleanProperty(false);
  private final BooleanProperty invalid = new SimpleBooleanProperty(false);

  public EditPresetModel(@NotNull Preset originalPreset) {
    this.originalPreset.set(originalPreset);
    name.set(originalPreset.name());
    modified.set(false);
    PropertyUtils.onChange(() -> modified.set(true), name);
    modifiedPreset.subscribe(nv -> setModifiedIfDifferent(nv));
  }

  private void setModifiedIfDifferent(@Nullable Preset modPreset) {
    if (originalPreset.get() == null) {
      logger.info("original preset is null, cannot compare");
      return;
    }
    if (modPreset == null || modified.get()) {
      return;
    }
    // has changed
    modified.set(!modPreset.equalsByContent(getOriginalPreset()));
  }

  public Preset getOriginalPreset() {
    return originalPreset.get();
  }

  public ObjectProperty<@Nullable Preset> presetOriginalProperty() {
    return originalPreset;
  }

  public String getName() {
    return name.get();
  }

  public StringProperty nameProperty() {
    return name;
  }

  public boolean isModified() {
    return modified.get();
  }

  public BooleanProperty modifiedProperty() {
    return modified;
  }

  public String getOriginalName() {
    return originalPreset.getName();
  }

  @Nullable
  public Preset createModifiedWithName() {
    final Preset modified = getModifiedPreset();
    // set name for modified if available
    return requireNonNullElse(modified, getOriginalPreset()).withName(getName());
  }

  /**
   * invalid means that renaming is not applied. This is usually just before or after this preset
   * was removed
   */
  public void setInvalid() {
    invalid.set(true);
  }

  public boolean isInvalid() {
    return invalid.get();
  }

  public BooleanProperty invalidProperty() {
    return invalid;
  }

  public void setModifiedPreset(Preset modified) {
    modifiedPreset.set(modified);
  }

  public @Nullable Preset getModifiedPreset() {
    return modifiedPreset.get();
  }

  public ObjectProperty<@Nullable Preset> modifiedPresetProperty() {
    return modifiedPreset;
  }
}
