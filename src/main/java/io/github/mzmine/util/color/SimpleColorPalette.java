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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.w3c.dom.Element;
import com.google.common.collect.ImmutableMap;
import io.github.mzmine.gui.chartbasics.chartthemes.EStandardChartTheme;
import io.github.mzmine.util.javafx.FxColorUtil;
import javafx.collections.ModifiableObservableListBase;
import javafx.scene.paint.Color;

/**
 * Implementation of a color palette. It's an observable list to allow addition of listeners.
 *
 * @author SteffenHeu steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 */
public class SimpleColorPalette extends ModifiableObservableListBase<Color> implements Cloneable {

  protected static final SimpleColorPalette DEFAULT_NORMAL =
      new SimpleColorPalette(ColorsFX.getSevenColorPalette(Vision.NORMAL_VISION, true), "Normal",
          ColorsFX.getPositiveColor(Vision.NORMAL_VISION), ColorsFX.getNeutralColor(),
          ColorsFX.getNegativeColor(Vision.NORMAL_VISION));

  protected static final SimpleColorPalette DEFAULT_DEUTERANOPIA =
      new SimpleColorPalette(ColorsFX.getSevenColorPalette(Vision.DEUTERANOPIA, true),
          "Deuteranopia", ColorsFX.getPositiveColor(Vision.DEUTERANOPIA),
          ColorsFX.getNeutralColor(), ColorsFX.getNegativeColor(Vision.DEUTERANOPIA));

  protected static final SimpleColorPalette DEFAULT_PROTANOPIA =
      new SimpleColorPalette(ColorsFX.getSevenColorPalette(Vision.PROTANOPIA, true), "Protanopia",
          ColorsFX.getPositiveColor(Vision.PROTANOPIA), ColorsFX.getNeutralColor(),
          ColorsFX.getNegativeColor(Vision.PROTANOPIA));

  protected static final SimpleColorPalette DEFAULT_TRITANOPIA =
      new SimpleColorPalette(ColorsFX.getSevenColorPalette(Vision.TRITANOPIA, true), "Tritanopia",
          ColorsFX.getPositiveColor(Vision.TRITANOPIA), ColorsFX.getNeutralColor(),
          ColorsFX.getNegativeColor(Vision.TRITANOPIA));

  /**
   * Access via {@link Vision}
   */
  public static final ImmutableMap<Vision, SimpleColorPalette> DEFAULT = ImmutableMap.of(
      Vision.NORMAL_VISION, DEFAULT_NORMAL, Vision.DEUTERANOPIA, DEFAULT_DEUTERANOPIA,
      Vision.PROTANOPIA, DEFAULT_PROTANOPIA, Vision.TRITANOPIA, DEFAULT_TRITANOPIA);

  private static final String NAME_ATTRIBUTE = "name";
  private static final String POS_ATTRIBUTE = "positive_color";
  private static final String NEG_ATTRIBUTE = "negative_color";
  private static final String NEU_ATTRIBUTE = "neutral_color";

  private static final Color defclr = Color.BLACK;
  private static final Logger logger = Logger.getLogger(SimpleColorPalette.class.getName());
  private static final int MAIN_COLOR = 0;

  private final List<Color> delegate;

  protected String name;
  protected int next;

  protected Color positiveColor;
  protected Color negativeColor;

  protected Color neutralColor;

  /**
   *
   */
  private static final long serialVersionUID = 1L;

  public SimpleColorPalette() {
    super();
    delegate = new ArrayList<>();
    next = 0;
    name = "";
    positiveColor = ColorsFX.getPositiveColor(Vision.DEUTERANOPIA);
    negativeColor = ColorsFX.getNegativeColor(Vision.DEUTERANOPIA);
    neutralColor = ColorsFX.getNeutralColor();
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

  public SimpleColorPalette(@Nonnull Color[] clrs, @Nonnull String name, Color positiveColor,
      Color neutralColor, Color negativeColor) {
    this(clrs, name);

    setPositiveColor(positiveColor);
    setNeutralColor(neutralColor);
    setNegativeColor(negativeColor);
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

  public java.awt.Color getNextColorAWT(int index) {
    return FxColorUtil.fxColorToAWT(getNextColor());
  }

  @Nonnull
  public Color getMainColor() {
    if (isValid()) {
      return get(MAIN_COLOR);
    }
    return Color.BLACK;
  }

  public java.awt.Color getMainColorAWT() {
    return FxColorUtil.fxColorToAWT(getMainColor());
  }

  public boolean isValid() {
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

  @Nonnull
  public String getName() {
    return name;
  }

  public void setName(@Nonnull String name) {
    this.name = name;
  }

  /**
   * @param color
   * @param newIndex
   * @return The new index of the moved color. -1 if called with invalid color.
   */
  public int moveColor(Color color, int newIndex) {
    return moveColor(indexOf(color), newIndex);
  }

  /**
   * @param oldIndex
   * @param newIndex
   * @return The new index of the moved color. -1 if called with invalid index.
   */
  public int moveColor(int oldIndex, int newIndex) {
    if (oldIndex < 0 || newIndex < 0 || oldIndex >= size() || newIndex >= size()) {
      logger.info("move called with invalid parameters " + oldIndex + " to " + newIndex);
      return -1;
    }

    Color clr = delegate.get(oldIndex);

    // if the color shall be moved to the end, we have to increment by 1, otherwise the last color
    // will just move one to the right.
    if (newIndex == size() - 1) {
      newIndex++;
    }

    delegate.add(newIndex, clr);

    if (oldIndex > newIndex) {
      remove(oldIndex + 1);
    } else {
      remove(oldIndex);
    }

    fireChange(new ColorPaletteChangedEvent(this, oldIndex, newIndex));

    return indexOf(clr);
  }

  /**
   * Checks for equality between two color palettes. Does not take the name into account.
   *
   * @param obj The palette.
   * @return true or false.
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }

    if (obj == this) {
      return true;
    }

    if (!(obj instanceof SimpleColorPalette)) {
      return false;
    }

    SimpleColorPalette palette = (SimpleColorPalette) obj;

    if (size() != palette.size()) {
      return false;
    }

    for (int i = 0; i < size(); i++) {
      if (!Objects.equals(get(i).toString(), palette.get(i).toString())) {
        return false;
      }
    }

    if (!Objects.equals(getName(), palette.getName())) {
      return false;
    }

    if (!Objects.equals(getPositiveColor().toString(), palette.getPositiveColor().toString())) {
      return false;
    }
    if (!Objects.equals(getNegativeColor().toString(), palette.getNegativeColor().toString())) {
      return false;
    }
    if (!Objects.equals(getNeutralColor().toString(), palette.getNeutralColor().toString())) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return super.hashCode() + name.hashCode() + getPositiveColor().hashCode()
        + getNeutralColor().hashCode() + getNegativeColor().hashCode();
  }

  @Override
  public SimpleColorPalette clone() {
    SimpleColorPalette clone = new SimpleColorPalette();
    for (Color clr : this) {
      clone.add(clr);
    }
    clone.setName(getName());
    clone.setNegativeColor(getNegativeColor());
    clone.setPositiveColor(getPositiveColor());
    return clone;
  }

  @Override
  public String toString() {
    return getName() + " " + super.toString() + " pos " + getPositiveColor().toString() + " neg "
        + getNegativeColor();
  }

  public void loadFromXML(Element xmlElement) {
    this.setName(xmlElement.getAttribute(NAME_ATTRIBUTE));
    String text = xmlElement.getTextContent();

    String pos = xmlElement.getAttribute(POS_ATTRIBUTE);
    String neg = xmlElement.getAttribute(NEG_ATTRIBUTE);
    String neu = xmlElement.getAttribute(NEU_ATTRIBUTE);

    Color clrPos, clrNeg, clrNeu;

    try {
      text = text.substring(1, text.length() - 1);
      text = text.replaceAll("\\s", "");
      String[] clrs = text.split(",");
      for (String clr : clrs) {
        delegate.add(Color.web(clr));
      }
    } catch (Exception e) {
      logger.log(Level.WARNING,
          "Could not load color palette " + name + ". Setting default colors.", e);
      this.addAll(ColorsFX.getSevenColorPalette(Vision.DEUTERANOPIA, true));
    }

    try {
      clrPos = Color.web(pos);
      clrNeg = Color.web(neg);
      clrNeu = Color.web(neu);
    } catch (Exception e) {
      logger.log(Level.WARNING,
          "Could not load positive/negative colors of " + name + ". Setting default colors.", e);
      clrPos = ColorsFX.getPositiveColor(Vision.DEUTERANOPIA);
      clrNeg = ColorsFX.getNegativeColor(Vision.DEUTERANOPIA);
      clrNeu = ColorsFX.getNeutralColor();
    }

    setPositiveColor(clrPos);
    setNegativeColor(clrNeg);
    setNeutralColor(clrNeu);
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
    xmlElement.setAttribute(POS_ATTRIBUTE, getPositiveColor().toString());
    xmlElement.setAttribute(NEG_ATTRIBUTE, getNegativeColor().toString());
    xmlElement.setAttribute(NEU_ATTRIBUTE, getNeutralColor().toString());
  }

  public Color getPositiveColor() {
    return positiveColor;
  }

  public java.awt.Color getPositiveColorAWT() {
    return FxColorUtil.fxColorToAWT(getPositiveColor());
  }

  public void setPositiveColor(Color positiveColor) {
    this.positiveColor = positiveColor;
    fireChange(new ColorPaletteChangedEvent(this,
        ColorPaletteChangeEventType.POSITIVE_MARKER_UPDATED, positiveColor));
  }

  public Color getNegativeColor() {
    return negativeColor;
  }

  public java.awt.Color getNegativeColorAWT() {
    return FxColorUtil.fxColorToAWT(getNegativeColor());
  }

  public void setNegativeColor(Color negativeColor) {
    this.negativeColor = negativeColor;
    fireChange(new ColorPaletteChangedEvent(this,
        ColorPaletteChangeEventType.NEGATIVE_MARKER_UPDATED, negativeColor));
  }

  public Color getNeutralColor() {
    return neutralColor;
  }

  public java.awt.Color getNeutralColorAWT() {
    return FxColorUtil.fxColorToAWT(getNeutralColor());
  }

  public void setNeutralColor(Color neutralColor) {
    this.neutralColor = neutralColor;
    fireChange(new ColorPaletteChangedEvent(this,
        ColorPaletteChangeEventType.NEUTRAL_MARKER_UPDATED, neutralColor));
  }

  // --- super type
  @Override
  public Color get(int index) {
    next = index + 1;
    return delegate.get(index);
  }

  public java.awt.Color getAWT(int index) {
    return FxColorUtil.fxColorToAWT(get(index));
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
