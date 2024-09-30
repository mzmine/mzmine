/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

import io.github.mzmine.datamodel.identities.IonPart;
import io.github.mzmine.datamodel.identities.IonType;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class IonTypeCreatorModel {

  private final ListProperty<IonPart> parts = new SimpleListProperty<>(
      FXCollections.observableArrayList());

  private final ListProperty<IonType> ionTypes = new SimpleListProperty<>(
      FXCollections.observableArrayList());

  public ObservableList<IonPart> getParts() {
    return parts.get();
  }

  public ListProperty<IonPart> partsProperty() {
    return parts;
  }

  public void setParts(final ObservableList<IonPart> parts) {
    this.parts.set(parts);
  }

  public ObservableList<IonType> getIonTypes() {
    return ionTypes.get();
  }

  public ListProperty<IonType> ionTypesProperty() {
    return ionTypes;
  }

  public void setIonTypes(final ObservableList<IonType> ionTypes) {
    this.ionTypes.set(ionTypes);
  }
}
