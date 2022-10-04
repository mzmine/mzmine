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

package io.github.mzmine.parameters.parametertypes.colorpalette;

import io.github.mzmine.util.color.Vision;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.util.color.SimpleColorPalette;

/**
 * User parameter for color palette selection.
 *
 * @author SteffenHeu steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 */
public class ColorPaletteParameter
    implements UserParameter<SimpleColorPalette, ColorPaletteComponent> {

  private static final String PALETTE_ELEMENT = "palette";
  private static final String SELECTED_INDEX = "selected";

  private static final Logger logger = Logger.getLogger(ColorPaletteParameter.class.getName());

  protected String name;
  protected String descr;
  protected SimpleColorPalette value;
  protected List<SimpleColorPalette> palettes;

  public ColorPaletteParameter(String name, String descr) {
    this.name = name;
    this.descr = descr;
    value = SimpleColorPalette.DEFAULT.get(Vision.DEUTERANOPIA);
    palettes = new ArrayList<>();
    palettes.addAll(SimpleColorPalette.DEFAULT.values());
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public SimpleColorPalette getValue() {
    return value;
  }

  @Override
  public void setValue(SimpleColorPalette newValue) {
    if (!palettes.contains(newValue)) {
      palettes.add(newValue);
      logger.fine("Did not contain palette " + newValue.toString() + ". Value was added.");
    }
    value = newValue;
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    if (getValue() == null || !getValue().isValid()) {
      errorMessages.add("Not enough colors in color palette " + getName());
      return false;
    }
    return true;
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    int selected = Integer.valueOf(xmlElement.getAttribute(SELECTED_INDEX));

    NodeList childs = xmlElement.getElementsByTagName(PALETTE_ELEMENT);
    palettes.clear();
    for (int i = 0; i < childs.getLength(); i++) {
      Element p = (Element) childs.item(i);
      if (p.getNodeName().equals(PALETTE_ELEMENT)) {
        palettes.add(SimpleColorPalette.createFromXML(p));
      }
    }

    setValue(palettes.get(selected));

    int index = 0;
    for (SimpleColorPalette def : SimpleColorPalette.DEFAULT.values()) {
      if (!palettes.contains(def)) {
        palettes.add(index, def);
        logger.info("Loaded color palettes did not contain default " + def.getName()
            + " palette. Adding...");
      }
      index++;
    }
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    Document doc = xmlElement.getOwnerDocument();

    xmlElement.setAttribute(SELECTED_INDEX, Integer.toString(palettes.indexOf(value)));

    for (SimpleColorPalette p : palettes) {
      Element palElement = doc.createElement(PALETTE_ELEMENT);
      p.saveToXML(palElement);
      xmlElement.appendChild(palElement);
    }
  }

  @Override
  public String getDescription() {
    return descr;
  }

  @Override
  public ColorPaletteComponent createEditingComponent() {
    ColorPaletteComponent comp = new ColorPaletteComponent();

    return comp;
  }

  @Override
  public void setValueFromComponent(ColorPaletteComponent component) {
    value = component.getValue();
    palettes = component.getPalettes();
  }

  @Override
  public void setValueToComponent(ColorPaletteComponent component, SimpleColorPalette newValue) {
    component.setPalettes(palettes);
    component.setValue(newValue);
  }

  protected @NotNull
  List<SimpleColorPalette> getPalettes() {
    return palettes;
  }

  protected void setPalettes(@NotNull List<SimpleColorPalette> palettes) {

    int index = 0;
    for (SimpleColorPalette def : SimpleColorPalette.DEFAULT.values()) {
      if (!palettes.contains(def)) {
        palettes.add(index, def);
        logger.info("Loaded color palettes did not contain default " + def.getName()
            + " palette. Adding...");
      }
      index++;
    }

    this.palettes = palettes;
  }

  @Override
  public UserParameter<SimpleColorPalette, ColorPaletteComponent> cloneParameter() {
    ColorPaletteParameter clone = new ColorPaletteParameter(name, descr);
    clone.setValue(getValue().clone());

    List<SimpleColorPalette> pals = new ArrayList<>();
    palettes.forEach(p -> pals.add(p.clone()));
    clone.setPalettes(pals);

    return clone;
  }

}
