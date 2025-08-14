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

import io.github.mzmine.javafx.properties.PropertyUtils;
import io.github.mzmine.util.presets.Preset;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.jetbrains.annotations.Nullable;

/**
 * Properties are not bound to the preset but initialized with its values. On change the modified
 * property will be true.
 */
class EditPresetModel {

  private final ObjectProperty<@Nullable Preset> preset = new SimpleObjectProperty<>();
  private final StringProperty name = new SimpleStringProperty();
  private final BooleanProperty modified = new SimpleBooleanProperty(false);
  private final BooleanProperty invalid = new SimpleBooleanProperty(false);

  public EditPresetModel(Preset preset) {
    this.preset.set(preset);
    name.set(preset.name());
    modified.set(false);
    PropertyUtils.onChange(() -> modified.set(true), name);
  }

  public @Nullable Preset getPreset() {
    return preset.get();
  }

  public ObjectProperty<@Nullable Preset> presetProperty() {
    return preset;
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
    return preset.getName();
  }

  public Preset createModified() {
    return getPreset().withName(getName());
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
}
