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

package io.github.mzmine.parameters.parametertypes.elements;

import io.github.mzmine.parameters.UserParameter;
import java.io.IOException;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openscience.cdk.config.IsotopeFactory;
import org.openscience.cdk.config.Isotopes;
import org.openscience.cdk.formula.MolecularFormulaRange;
import org.openscience.cdk.interfaces.IIsotope;
import org.w3c.dom.Element;

/**
 * Parameter to setup element composition range represented by MolecularFormulaRange (CDK class).
 */
public class ElementsCompositionRangeParameter implements
    UserParameter<MolecularFormulaRange, ElementsCompositionRangeComponent> {

  private static final Logger logger = Logger.getLogger(
      ElementsCompositionRangeParameter.class.getName());
  private final String name;
  private final String description;
  private MolecularFormulaRange value;

  public ElementsCompositionRangeParameter(String name, String description) {
    this.name = name;
    this.description = description;
    this.value = new MolecularFormulaRange();
    try {
      IsotopeFactory iFac = Isotopes.getInstance();
      value.addIsotope(iFac.getMajorIsotope("C"), 0, 200);
      value.addIsotope(iFac.getMajorIsotope("H"), 0, 300);
      value.addIsotope(iFac.getMajorIsotope("N"), 0, 15);
      value.addIsotope(iFac.getMajorIsotope("O"), 0, 25);
      value.addIsotope(iFac.getMajorIsotope("P"), 0, 2);
      value.addIsotope(iFac.getMajorIsotope("S"), 0, 2);
    } catch (IOException e) {
      logger.log(Level.WARNING, e.getMessage(), e);
    }
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public ElementsCompositionRangeComponent createEditingComponent() {
    ElementsCompositionRangeComponent editor = new ElementsCompositionRangeComponent();
    return editor;
  }

  @Override
  public ElementsCompositionRangeParameter cloneParameter() {
    ElementsCompositionRangeParameter copy = new ElementsCompositionRangeParameter(name,
        description);
    copy.value = value;
    return copy;
  }

  @Override
  public void setValueFromComponent(ElementsCompositionRangeComponent component) {
    value = component.getElements();
  }

  @Override
  public void setValueToComponent(ElementsCompositionRangeComponent component,
      MolecularFormulaRange newValue) {
    component.setElements(newValue);
  }

  @Override
  public MolecularFormulaRange getValue() {
    return value;
  }

  @Override
  public void setValue(MolecularFormulaRange newValue) {
    this.value = newValue;
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {

    try {
      MolecularFormulaRange newValue = new MolecularFormulaRange();
      IsotopeFactory iFac = Isotopes.getInstance();

      String s = xmlElement.getTextContent();
      Pattern p = Pattern.compile("([a-zA-Z]+)\\[([0-9]+)-([0-9]+)\\]");
      Matcher m = p.matcher(s);
      while (m.find()) {
        String elementSymbol = m.group(1);
        int minCount = Integer.parseInt(m.group(2));
        int maxCount = Integer.parseInt(m.group(3));
        newValue.addIsotope(iFac.getMajorIsotope(elementSymbol), minCount, maxCount);
      }
      this.value = newValue;

    } catch (IOException e) {
      logger.log(Level.WARNING, e.getMessage(), e);
    }
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    if (value == null) {
      return;
    }
    StringBuilder s = new StringBuilder();
    for (IIsotope i : value.isotopes()) {
      s.append(i.getSymbol());
      s.append("[");
      s.append(value.getIsotopeCountMin(i));
      s.append("-");
      s.append(value.getIsotopeCountMax(i));
      s.append("]");
    }
    xmlElement.setTextContent(s.toString());
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    if ((value == null) || (value.getIsotopeCount() == 0)) {
      errorMessages.add("Please set the chemical elements");
      return false;
    }
    return true;
  }

}
