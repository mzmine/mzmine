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

package io.github.mzmine.util.javafx;

import io.github.mzmine.datamodel.features.types.annotations.iin.IonTypeType;
import io.github.mzmine.datamodel.identities.iontype.IonPart;
import io.github.mzmine.datamodel.identities.iontype.IonPartParsingException;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.datamodel.identities.iontype.IonTypeParser;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.validation.FxValidation;
import java.util.List;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class IonTypeTextField extends HBox {

  private final ObjectProperty<@Nullable IonType> ionTypeProperty = new SimpleObjectProperty<>();
  private static final IonTypeType ionType = new IonTypeType();
  private final TextField tf = new TextField();
  private final Label parsed = new Label();
  private final StringProperty stringProperty = new SimpleStringProperty();
  private final StringProperty parsingError = new SimpleStringProperty();

  public IonTypeTextField() {
    super(FxLayout.DEFAULT_SPACE);

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
        try {
          final IonType ion = IonTypeParser.parseOrThrow(string);
          if (ion == null) {
            parsingError.set(null);
            return null;
          }

          final List<@NotNull IonPart> undefined = ion.stream().filter(IonPart::isUndefinedMass)
              .toList();
          parsingError.set(undefined.isEmpty() ? null : """
              Ion type contains parts with undefined mass, open the %s tab to define these ion building blocks and re-enter this ion.""");
          return ion;
        } catch (IonPartParsingException e) {
          parsingError.set(e.getMessage());
          return null;
        }
      }
    };

    Bindings.bindBidirectional(stringProperty, ionTypeProperty, converter);

    tf.textProperty().bindBidirectional(stringProperty);

    ionTypeProperty.addListener((__, old, newType) -> {
      parsed.setText(converter.toString(newType));
    });

    FxValidation.registerErrorValidator(tf, parsingError);

    setAlignment(Pos.CENTER_LEFT);
    getChildren().addAll(tf, parsed);
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
