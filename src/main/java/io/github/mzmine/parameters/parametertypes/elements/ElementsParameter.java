/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.UserParameter;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.openscience.cdk.Element;

public class ElementsParameter implements UserParameter<List<Element>, ElementsComponent> {

  private final String name, description;
  private final boolean valueRequired;
  private List<Element> value;

  public ElementsParameter(String name, String description, boolean valueRequired,
      List<Element> defaultValue) {

    this.name = name;
    this.description = description;
    this.valueRequired = valueRequired;
    this.value = defaultValue;
  }

  public ElementsParameter(String name, String description) {
    // Most abundance elements in biomolecules as a default value for elements
    this(name, description, true, Arrays.asList(new Element("H"),
        new Element("C"), new Element("N"), new Element("O"),
        new Element("S")));
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public ElementsComponent createEditingComponent() {
    return new ElementsComponent(this.value);
  }

  @Override
  public void setValueFromComponent(ElementsComponent elementsComponent) {
    this.value = elementsComponent.getValue();
  }

  @Override
  public void setValueToComponent(ElementsComponent elementsComponent, List<Element> newValue) {
    elementsComponent.setValue(newValue);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public List<Element> getValue() {
    return value;
  }

  @Override
  public void setValue(List<Element> value) {
    this.value = value;
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    if (value.isEmpty()) {
      errorMessages.add("There are no chemical elements selected.");
      return false;
    }

    return true;
  }

  @Override
  public void loadValueFromXML(org.w3c.dom.Element xmlElement) {
    if (xmlElement == null) {
      throw new NullPointerException("XML Element is null.");
    }

    String values = xmlElement.getTextContent().replaceAll("\\s", "");
    value = Arrays.stream(values.split(","))
        .map(Element::new)
        .collect(Collectors.toList());
  }

  @Override
  public void saveValueToXML(org.w3c.dom.Element xmlElement) {
    if (xmlElement == null) {
      throw new NullPointerException("XML Element is null.");
    }

    if (value == null) {
      throw new NullPointerException("Value is null.");
    }

    String text = value.stream()
        .map(Element::getSymbol)
        .map(Object::toString)
        .collect(Collectors.joining(","));

    xmlElement.setTextContent(text);
  }

  @Override
  public boolean isSensitive() {
    return false;
  }

  @Override
  public UserParameter<List<Element>, ElementsComponent> cloneParameter() {
    ElementsParameter copy = new ElementsParameter(name, description, valueRequired, value);
    copy.setValue(this.getValue());
    return copy;
  }

  @Override
  public boolean valueEquals(Parameter<?> that) {
    if(!(that instanceof ElementsParameter elementsParameter)) {
      return false;
    }

    final List<Element> thatElements = elementsParameter.getValue();
    if(thatElements.size() != value.size()) {
      return false;
    }

    for (int i = 0; i < value.size(); i++) {
      var element = value.get(i);
      if(!element.compare(thatElements.get(i))) {
        return false;
      }
    }
    return true;
  }
}
