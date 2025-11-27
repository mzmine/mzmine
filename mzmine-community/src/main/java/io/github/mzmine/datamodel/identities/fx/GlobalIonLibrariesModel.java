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
import io.github.mzmine.datamodel.identities.IonPart;
import io.github.mzmine.datamodel.identities.IonType;
import io.github.mzmine.datamodel.identities.global.GlobalIonLibraryService;
import io.github.mzmine.javafx.properties.LastUpdateProperty;
import java.time.Instant;
import java.util.function.Consumer;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.NotNull;

class GlobalIonLibrariesModel {

  /**
   * All defined libraries
   */
  private final ReadOnlyListWrapper<IonLibrary> libraries = new ReadOnlyListWrapper<>(
      FXCollections.observableArrayList());

  /**
   * Global ion types list of all defined ion types that may be used in libraries
   */
  private final ReadOnlyListWrapper<IonType> ionTypes = new ReadOnlyListWrapper<>(
      FXCollections.observableArrayList());
  /**
   * Global parts list of all defined parts
   */
  private final ReadOnlyListWrapper<IonPart> parts = new ReadOnlyListWrapper<>(
      FXCollections.observableArrayList());

  /**
   * true if the global ions version has changed since retrieving the ions libraries etc. This may
   * be used to show a notification to the user to update the model.
   */
  private final BooleanBinding globalVersionChanged;

  /**
   * The last change to the internal data model - might show a notification to update the global
   * model based on this
   */
  private final LastUpdateProperty lastModelUpdateProperty = new LastUpdateProperty();

  /**
   * Holds the global library version number last retrieved for updating the model
   * {@link GlobalIonLibraryService#getVersion()}. This is a modification counter.
   */
  private final IntegerProperty retrivalVersion = new SimpleIntegerProperty();
  /**
   * The current version - mismatch to retrieval date signals change.
   * {@link GlobalIonLibraryService#getVersion()}. This is a modification counter.
   */
  private final IntegerProperty globalIonsVersion = new SimpleIntegerProperty();


  private Consumer<IonLibrary> editSelectedAction;
  private Runnable createNewAction;

  public GlobalIonLibrariesModel() {
    globalVersionChanged = retrivalVersion.isEqualTo(globalIonsVersion);
  }

  public int getGlobalIonsVersion() {
    return globalIonsVersion.get();
  }

  public IntegerProperty globalIonsVersionProperty() {
    return globalIonsVersion;
  }

  public void setGlobalIonsVersion(int globalIonsVersion) {
    this.globalIonsVersion.set(globalIonsVersion);
  }

  public boolean isGlobalVersionChanged() {
    return globalVersionChanged.get();
  }

  public BooleanBinding globalVersionChangedProperty() {
    return globalVersionChanged;
  }

  public Instant getLastModelUpdateProperty() {
    return lastModelUpdateProperty.get();
  }

  public LastUpdateProperty lastModelUpdatePropertyProperty() {
    return lastModelUpdateProperty;
  }

  public void setLastModelUpdateProperty(Instant lastModelUpdateProperty) {
    this.lastModelUpdateProperty.set(lastModelUpdateProperty);
  }

  public void setCreateNewAction(@NotNull Runnable createNewAction) {
    this.createNewAction = createNewAction;
  }

  public void setEditSelectedAction(@NotNull Consumer<IonLibrary> editSelectedAction) {
    this.editSelectedAction = editSelectedAction;
  }

  public @NotNull Consumer<IonLibrary> getEditSelectedAction() {
    return editSelectedAction;
  }

  public @NotNull Runnable getCreateNewAction() {
    return createNewAction;
  }

  public ObservableList<IonLibrary> getLibraries() {
    return libraries.get();
  }

  public ReadOnlyListWrapper<IonLibrary> librariesProperty() {
    return libraries;
  }

  public void setLibraries(ObservableList<IonLibrary> libraries) {
    this.libraries.set(libraries);
  }

  public ObservableList<IonPart> getParts() {
    return parts.get();
  }

  public ReadOnlyListWrapper<IonPart> partsProperty() {
    return parts;
  }

  public void setParts(final ObservableList<IonPart> parts) {
    this.parts.set(parts);
  }

  public ObservableList<IonType> getIonTypes() {
    return ionTypes.get();
  }

  public ReadOnlyListWrapper<IonType> ionTypesProperty() {
    return ionTypes;
  }

  public void setIonTypes(final ObservableList<IonType> ionTypes) {
    this.ionTypes.set(ionTypes);
  }

  public int getRetrivalVersion() {
    return retrivalVersion.get();
  }

  public IntegerProperty retrivalVersionProperty() {
    return retrivalVersion;
  }

  public void setRetrivalVersion(int retrivalVersion) {
    this.retrivalVersion.set(retrivalVersion);
  }
}
