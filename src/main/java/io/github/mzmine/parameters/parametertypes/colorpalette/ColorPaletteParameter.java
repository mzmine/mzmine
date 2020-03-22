/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.parameters.parametertypes.colorpalette;

import io.github.mzmine.util.color.Vision;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
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
    palettes.add(value);
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

  protected @Nonnull
  List<SimpleColorPalette> getPalettes() {
    return palettes;
  }

  protected void setPalettes(@Nonnull List<SimpleColorPalette> palettes) {

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
