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
import io.github.mzmine.util.FormulaUtils;
import io.github.mzmine.util.StringUtils;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.jetbrains.annotations.Nullable;

class IonPartModel {

  private final ObjectProperty<IonPart> part = new SimpleObjectProperty<>();
  private final StringProperty name = new SimpleStringProperty();
  private final StringProperty formula = new SimpleStringProperty();
  private final BooleanProperty formulaInputCorrect = new SimpleBooleanProperty();

  private final DoubleProperty absSingleMass = new SimpleDoubleProperty();
  private final IntegerProperty singleCharge = new SimpleIntegerProperty();
  private final IntegerProperty count = new SimpleIntegerProperty();

  public IonPartModel(IonPart ion) {
    part.set(ion);
    name.bind(part.map(IonPart::name));
    absSingleMass.bind(part.map(IonPart::absSingleMass));
    singleCharge.bind(part.map(IonPart::singleCharge).orElse(0));
    count.bind(part.map(IonPart::count).orElse(1));
    formula.bind(part.map(IonPart::singleFormulaUnchargedString));
    formulaInputCorrect.bind(formula.map(IonPartModel::formulaEmptyOrValid));
  }

  public IonPartModel() {
    this(null);
  }

  /**
   * Either blank or valid fomrula
   */
  private static boolean formulaEmptyOrValid(@Nullable String f) {
    return StringUtils.isBlank(f) || FormulaUtils.createMajorIsotopeMolFormulaWithCharge(f) != null;
  }

  public IonPart getPart() {
    return part.get();
  }

  public ObjectProperty<IonPart> partProperty() {
    return part;
  }

  public void setPart(final IonPart part) {
    this.part.set(part);
  }

  public String getName() {
    return name.get();
  }

  public StringProperty nameProperty() {
    return name;
  }

  public String getFormula() {
    return formula.get();
  }

  public StringProperty formulaProperty() {
    return formula;
  }

  public boolean isFormulaInputCorrect() {
    return formulaInputCorrect.get();
  }

  public BooleanProperty formulaInputCorrectProperty() {
    return formulaInputCorrect;
  }

  public double getAbsSingleMass() {
    return absSingleMass.get();
  }

  public DoubleProperty absSingleMassProperty() {
    return absSingleMass;
  }

  public void setAbsSingleMass(final double absSingleMass) {
    this.absSingleMass.set(absSingleMass);
  }

  public int getSingleCharge() {
    return singleCharge.get();
  }

  public IntegerProperty singleChargeProperty() {
    return singleCharge;
  }

  public int getCount() {
    return count.get();
  }

  public IntegerProperty countProperty() {
    return count;
  }
}
