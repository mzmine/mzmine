/*
 * Copyright 2006-2022 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *
 */

package io.github.mzmine.util.components;

import io.github.mzmine.util.FormulaUtils;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public class FormulaTextField extends TextField {

  private final ObjectProperty<@Nullable IMolecularFormula> formula = new SimpleObjectProperty<>();

  public FormulaTextField() {

    Bindings.bindBidirectional(textProperty(), formulaProperty(), new StringConverter<>() {
      @Override
      public String toString(IMolecularFormula object) {
        if (object == null) {
          return "";
        }
        return MolecularFormulaManipulator.getString(object);
      }

      @Override
      public IMolecularFormula fromString(String string) {
        final IMolecularFormula formula = FormulaUtils.createMajorIsotopeMolFormula(string);
        return formula;
      }
    });
  }


  public @Nullable IMolecularFormula getFormula() {
    return formula.get();
  }

  public ObjectProperty<@Nullable IMolecularFormula> formulaProperty() {
    return formula;
  }

  public void setFormula(@Nullable IMolecularFormula formula) {
    this.formula.set(formula);
  }
}
