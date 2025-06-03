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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.util.javafx;

import io.github.mzmine.datamodel.features.types.annotations.iin.IonTypeType;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.datamodel.identities.iontype.IonTypeParser;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;
import javafx.util.StringConverter;
import org.jetbrains.annotations.Nullable;

public class IonTypeTextField extends BorderPane {

  private final ObjectProperty<@Nullable IonType> ionTypeProperty = new SimpleObjectProperty<>();
  private static final IonTypeType ionType = new IonTypeType();
  private final TextField tf = new TextField();
  private final Label parsed = new Label();
  private final StringProperty stringProperty = new SimpleStringProperty();

  public IonTypeTextField() {
    super();

    final StringConverter<IonType> converter = new StringConverter<>() {
      @Override
      public java.lang.String toString(IonType object) {
        if (object == null) {
          return "";
        }
        return ionType.getFormattedStringCheckType(object);
      }

      @Override
      public IonType fromString(java.lang.String string) {
        if (string == null || string.isBlank()) {
          return null;
        }
        return IonTypeParser.parse(string);
      }
    };

    Bindings.bindBidirectional(stringProperty, ionTypeProperty, converter);

    tf.textProperty().bindBidirectional(stringProperty);
    tf.textProperty().addListener((_, _, _) -> ionTypeProperty.get());

    ionTypeProperty.addListener((__, old, newType) -> {
      parsed.setText(converter.toString(newType));
    });

    setCenter(tf);
    setRight(parsed);
    BorderPane.setAlignment(parsed, Pos.CENTER_LEFT);
  }

  public TextField getTextField() {
    return tf;
  }

  public @Nullable IonType getIonType() {
    return ionTypeProperty.get();
  }

  public ObjectProperty<@Nullable IonType> ionTypeProperty() {
    return ionTypeProperty;
  }
}
