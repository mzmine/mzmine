/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

<<<<<<<< HEAD:mzmine-community/src/main/java/io/github/mzmine/modules/batchmode/change_outfiles/ChangeOutputFilesModule.java
package io.github.mzmine.modules.batchmode.change_outfiles;

import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Change all output files at once
 */
public class ChangeOutputFilesModule implements MZmineModule {

  @Override
  public @NotNull String getName() {
    return "Change output files";
========
package io.github.mzmine.javafx.components.factories;

import javafx.beans.property.BooleanProperty;
import javafx.scene.control.CheckBox;

public class FxCheckBox {

  public static CheckBox newCheckBox(String text, BooleanProperty selectedProperty) {
    var box = new CheckBox(text);
    box.selectedProperty().bindBidirectional(selectedProperty);
    return box;
>>>>>>>> 69eda4f52 (Merge MZMine changes):javafx-framework/src/main/java/io/github/mzmine/javafx/components/factories/FxCheckBox.java
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return ChangeOutputFilesParameters.class;
  }
}
