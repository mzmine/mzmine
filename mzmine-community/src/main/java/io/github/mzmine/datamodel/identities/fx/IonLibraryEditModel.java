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

package io.github.mzmine.datamodel.identities.fx;

import io.github.mzmine.datamodel.identities.IonLibrary;
import io.github.mzmine.datamodel.identities.IonType;
import io.github.mzmine.javafx.properties.PropertyUtils;
import java.util.List;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.Nullable;

/**
 * Opens a new pane to edit a single ion library in a new Tab
 */
class IonLibraryEditModel {

  private final ReadOnlyObjectWrapper<IonLibrary> library = new ReadOnlyObjectWrapper<>();
  private final ReadOnlyBooleanWrapper isNewlyCreated = new ReadOnlyBooleanWrapper();

  private final StringProperty name = new SimpleStringProperty();
  private final StringProperty title = new SimpleStringProperty("");
  private final ObservableList<IonType> ionTypes = FXCollections.observableArrayList();
  private final BooleanProperty sameAsOriginal = new SimpleBooleanProperty(true);
  private final ObjectProperty<EditState> editState = new SimpleObjectProperty<>(EditState.MAIN);


  public IonLibraryEditModel(@Nullable IonLibrary library) {
    setLibrary(library);

    // ion types or name changed so save button is active
    PropertyUtils.onChange(() -> sameAsOriginal.set(false), ionTypes, name);
  }

  public boolean isSameAsOriginal() {
    return sameAsOriginal.get();
  }

  public BooleanProperty sameAsOriginalProperty() {
    return sameAsOriginal;
  }

  public void setSameAsOriginal(boolean sameAsOriginal) {
    this.sameAsOriginal.set(sameAsOriginal);
  }

  public @Nullable IonLibrary getLibrary() {
    return library.get();
  }

  public ReadOnlyObjectProperty<IonLibrary> libraryProperty() {
    return library.getReadOnlyProperty();
  }

  /**
   * Used when library is saved to reset changed value etc
   */
  public void setLibrary(IonLibrary library) {
    this.isNewlyCreated.set(library == null);
    this.library.set(library);

    if (library == null) {
      name.set("unnamed");
    } else {
      name.set(library.getName());
      ionTypes.setAll(library.getIons());
    }
    sameAsOriginal.set(true);
    title.set(isNewlyCreated() ? "Creating new library" : "Editing ion library " + getName());
  }

  public String getName() {
    return name.get();
  }

  public StringProperty nameProperty() {
    return name;
  }

  public void setName(String name) {
    this.name.set(name);
  }

  public ObservableList<IonType> getIonTypes() {
    return ionTypes;
  }

  public void addIonTypes(List<IonType> ionTypes) {
    this.ionTypes.addAll(ionTypes);
  }

  public ReadOnlyBooleanProperty isNewlyCreatedProperty() {
    return isNewlyCreated.getReadOnlyProperty();
  }

  public boolean isNewlyCreated() {
    return isNewlyCreated.get();
  }

  public String getTitle() {
    return title.get();
  }

  public StringProperty titleProperty() {
    return title;
  }

  public ObservableValue<EditState> editStateProperty() {
    return editState;
  }

  public void setEditState(EditState state) {
    editState.set(state);
  }

  public EditState getEditState() {
    return editState.get();
  }

  enum EditState {
    MAIN, ADD_FROM_GLOBAL_IONS, COMPOSE_LIBRARIES
  }
}
