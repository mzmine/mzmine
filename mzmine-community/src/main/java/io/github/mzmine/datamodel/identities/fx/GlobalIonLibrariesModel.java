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
import java.util.function.Consumer;
import javafx.beans.property.ReadOnlyListWrapper;
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


  private Consumer<IonLibrary> editSelectedAction;
  private Runnable createNewAction;


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

}
