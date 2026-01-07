/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.parameters.parametertypes.ionidentity;

import io.github.mzmine.datamodel.identities.iontype.IonModification;
import io.github.mzmine.parameters.parametertypes.CheckComboParameter;
import java.util.List;
import javafx.util.StringConverter;
import org.controlsfx.control.CheckComboBox;
import org.jetbrains.annotations.NotNull;

/**
 * Alternative simple parameter if there are only few adducts or modifications to set
 */
public class IonCheckComboBoxParameter extends CheckComboParameter<IonModification> {

  public IonCheckComboBoxParameter(final String name, final String description,
      final IonModification[] choices) {
    super(name, description, choices);
  }

  public IonCheckComboBoxParameter(final String name, final String description,
      final IonModification[] choices, final List<IonModification> defaultValue) {
    super(name, description, choices, defaultValue);
  }

  public IonCheckComboBoxParameter(final String name, final String description,
      final List<IonModification> choices) {
    super(name, description, choices);
  }

  public IonCheckComboBoxParameter(final String name, final String description,
      final List<IonModification> choices, @NotNull final List<IonModification> defaultValue) {
    super(name, description, choices, defaultValue);
  }


  @Override
  public CheckComboBox<IonModification> createEditingComponent() {
    CheckComboBox<IonModification> combo = new CheckComboBox<>(choices);
    combo.setShowCheckedCount(true);
    combo.setConverter(new StringConverter<>() {
      @Override
      public String toString(final IonModification ion) {
        return ion.toString(false);
      }

      @Override
      public IonModification fromString(final String string) {
        for (final IonModification choice : choices) {
          if (choice.toString(false).equals(string)) {
            return choice;
          }
        }
        return null;
      }
    });
    return combo;
  }

}
