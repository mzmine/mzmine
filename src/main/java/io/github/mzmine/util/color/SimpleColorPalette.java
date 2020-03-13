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

package io.github.mzmine.util.color;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.w3c.dom.Element;
import io.github.mzmine.gui.chartbasics.chartthemes.EStandardChartTheme;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.util.javafx.FxColorUtil;
import javafx.collections.ModifiableObservableListBase;
import javafx.scene.paint.Color;

/**
 * Implementation of a color palette. It's an observable list to allow addition of listeners.
 *
 * @author SteffenHeu steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 *
 */
public class SimpleColorPalette extends ModifiableObservableListBase<Color> implements Cloneable {

  private static final String NAME_ATTRIBUTE = "name";

  private static final Color defclr = Color.BLACK;
  private static final Logger logger = Logger.getLogger(SimpleColorPalette.class.getName());
  private static final int MAIN_COLOR = 0;

  private final List<Color> delegate;

  protected String name;
  protected int next;

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  public SimpleColorPalette() {
    super();
    delegate = new ArrayList<>();
    next = 0;
    name = "";
  }

  public SimpleColorPalette(@Nonnull Color[] clrs) {
    this();
    for (Color clr : clrs) {
      add(clr);
    }
  }

  public SimpleColorPalette(@Nonnull Color[] clrs, @Nonnull String name) {
    this(clrs);
    setName(name);
  }

  public void applyToChartTheme(EStandardChartTheme theme) {

    List<java.awt.Color> awtColors = new ArrayList<>();
    this.forEach(c -> awtColors.add(FxColorUtil.fxColorToAWT(c)));
    java.awt.Color colors[] = awtColors.toArray(new java.awt.Color[0]);

    theme.setDrawingSupplier(new DefaultDrawingSupplier(colors, colors, colors,
        DefaultDrawingSupplier.DEFAULT_STROKE_SEQUENCE,
        DefaultDrawingSupplier.DEFAULT_OUTLINE_STROKE_SEQUENCE,
        DefaultDrawingSupplier.DEFAULT_SHAPE_SEQUENCE));
  }

  /**
   * 
   * @return The next color in the color palette.
   */
  public Color getNextColor() {
    if (this.isEmpty()) {
      return defclr;
    }

    if (next >= this.size() - 1) {
      next = 0;
    }
    next++;

    return get(next - 1);
  }

  @Nonnull public Color getMainColor() {
    if (isValidPalette()) {
      return get(MAIN_COLOR);
    }
    return Color.BLACK;
  }

  public boolean isValidPalette() {
    if (this.isEmpty()) {
      return false;
    }
    for (Color clr : this) {
      if (clr == null) {
        return false;
      }
    }
    if (this.size() < 3) {
      return false;
    }
    return true;
  }

  @Nonnull public String getName() {
    return name;
  }

  public void setName(@Nonnull String name) {
    this.name = name;
  }

  public void moveColor(Color color, int newIndex) {
    moveColor(indexOf(color), newIndex);
  }

  public void moveColor(int oldIndex, int newIndex) {
    if (oldIndex < 0 || newIndex < 0 || oldIndex >= size() || newIndex >= size()) {
      logger.info("move called with invalid parameters " + oldIndex + " to " + newIndex);
      return;
    }

    List<Color> sublist = new ArrayList<>();
    delegate.subList(newIndex, delegate.size()).forEach(c -> sublist.add(c));
    Color clr = delegate.get(oldIndex);

    sublist.remove(clr);
    delegate.remove(clr);

    delegate.removeAll(sublist);
    delegate.add(clr);
    delegate.addAll(sublist);

    fireChange(new ColorPaletteColorMovedEvent(this, oldIndex, newIndex));
  }

  /**
   * Checks for equality between two color palettes. Does not take the name into account.
   * 
   * @param obj The palette.
   * @return true or false.
   */
  @Override public boolean equals(Object obj) {
    if(obj == null)
      return false;

    if(obj == this)
      return true;

    if(!(obj instanceof SimpleColorPalette))
      return false;

    SimpleColorPalette palette = (SimpleColorPalette) obj;

    if (size() != palette.size())
      return false;

    for (int i = 0; i < size(); i++) {
      if (!Objects.equals(get(i).toString(), palette.get(i).toString())) {
        return false;
      }
    }

    if(!Objects.equals(getName(), palette.getName()))
      return false;

    return true;
  }

  @Override
  public int hashCode() {
    return super.hashCode() + name.hashCode();
  }

  @Override
  public SimpleColorPalette clone() {
    SimpleColorPalette clone = new SimpleColorPalette();
    for (Color clr : this) {
      clone.add(clr);
    }
    clone.setName(getName());
    return clone;
  }

  @Override
  public String toString() {
    return getName() + " " + super.toString();
  }

  public void loadFromXML(Element xmlElement) {
    this.setName(xmlElement.getAttribute(NAME_ATTRIBUTE));
    String text = xmlElement.getTextContent();

    // not a single color in the palette
    if (text.length() < 10) {
      this.clear();
      this.addAll(
          ColorsFX.getSevenColorPalette(MZmineCore.getConfiguration().getColorVision(), true));
      return;
    }
    text = text.substring(1, text.length() - 1);
    text = text.replaceAll("\\s", "");
    String[] clrs = text.split(",");
    for (String clr : clrs) {
      delegate.add(Color.web(clr));
    }
  }

  public static SimpleColorPalette createFromXML(Element xmlElement) {
    SimpleColorPalette p = new SimpleColorPalette();
    p.loadFromXML(xmlElement);
    return p;
  }

  /**
   * Saves this color palette to an xml element.
   *
   * @param xmlElement The xml element this palette shall be saved into.
   */
  public void saveToXML(Element xmlElement) {
    xmlElement.setAttribute(NAME_ATTRIBUTE, name);
    xmlElement.setTextContent(delegate.toString());
    logger.info(xmlElement.toString());
  }

  // --- super type
  @Override
  public Color get(int index) {
    next = index + 1;
    return delegate.get(index);
  }

  @Override
  public int size() {
    return delegate.size();
  }

  @Override
  protected void doAdd(int index, Color element) {
    delegate.add(index, element);
  }

  @Override
  protected Color doSet(int index, Color element) {
    return delegate.set(index, element);
  }

  @Override
  protected Color doRemove(int index) {
    return delegate.remove(index);
  }

}
