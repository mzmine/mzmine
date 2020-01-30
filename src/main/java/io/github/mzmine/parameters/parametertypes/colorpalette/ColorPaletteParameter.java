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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.w3c.dom.Element;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.util.color.ColorsFX;
import io.github.mzmine.util.color.Vision;
import javafx.scene.Node;

public class ColorPaletteParameter
    implements UserParameter<SimpleColorPalette, ColorPaletteComponent> {

  protected String name;
  protected String descr;
  protected SimpleColorPalette value;

  public ColorPaletteParameter(String name, String descr) {
    this.name = name;
    this.descr = descr;
    value = new SimpleColorPalette();
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

  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    return false;
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {

  }

  @Override
  public void saveValueToXML(Element xmlElement) {

  }

  @Override
  public String getDescription() {
    return descr;
  }

  @Override
  public ColorPaletteComponent createEditingComponent() {
    ColorPaletteComponent comp = new ColorPaletteComponent();
    
    List<SimpleColorPalette> p = new ArrayList<SimpleColorPalette>();
    for(Vision v : Vision.values()) {
      p.add(new SimpleColorPalette(ColorsFX.getSevenColorPalette(v, true)));
    }
      
    comp.setPalettes(p);
    
    return comp;
  }

  @Override
  public void setValueFromComponent(ColorPaletteComponent component) {
    component.getValue();
  }

  @Override
  public void setValueToComponent(ColorPaletteComponent component, SimpleColorPalette newValue) {
    component.setValue(newValue);
  }

  @Override
  public UserParameter<SimpleColorPalette, ColorPaletteComponent> cloneParameter() {
    // TODO Auto-generated method stub
    return null;
  }

}
