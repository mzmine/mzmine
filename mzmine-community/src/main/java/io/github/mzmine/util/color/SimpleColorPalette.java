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

package io.github.mzmine.util.color;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Range;
import io.github.mzmine.gui.chartbasics.chartthemes.EStandardChartTheme;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScale;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScaleFactory;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScaleTransform;
import io.github.mzmine.javafx.util.FxColorUtil;
import io.github.mzmine.javafx.util.color.ColorsFX;
import io.github.mzmine.javafx.util.color.Vision;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.ModifiableObservableListBase;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.w3c.dom.Element;

/**
 * Implementation of a color palette. It's an observable list to allow addition of listeners.
 *
 * @author SteffenHeu steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 */
public class SimpleColorPalette extends ModifiableObservableListBase<Color> implements Cloneable {

  public static final SimpleColorPalette RAINBOW = new SimpleColorPalette(
      new Color[]{new Color(0.3d, 0d, 0.4d, 1d), new Color(0d, 0d, 1d, 1d),
          new Color(0d, 1d, 0d, 1d), new Color(1d, 1d, 0d, 1d), new Color(1d, .5d, 0, 1d),
          new Color(1d, 0, 0, 1d)}, "Rainbow");

  public static final SimpleColorPalette BLUE_RED_WHITE = new SimpleColorPalette(
      new Color[]{new Color(0d, 0d, 1d, 1d), new Color(1d, 0, 0, 1d), new Color(1d, 1d, 1d, 1d)},
      "Blue-Red-White");

  public static final SimpleColorPalette GREEN_YELLOW = new SimpleColorPalette(
      new Color[]{new Color(0.f, 0.620f, 0.451f, 1f), new Color(0.941f, 0.894f, 0.259f, 1f)},
      "Green-Yellow (Color blind friendly)");

  public static final SimpleColorPalette BLUE_YELLOW = new SimpleColorPalette(
      new Color[]{Color.web("#2bb2ff", 1.0f), new Color(0.941f, 0.894f, 0.259f, 1f)},
      "Blue-Yellow (Color blind friendly)");

  public static final SimpleColorPalette BLUE_ORANGE_WHITE = new SimpleColorPalette(
      new Color[]{Color.BLACK, Color.web("#1A3399"), Color.web("#D55E00"), Color.WHITE},
      "Blue-Orange-White");

  public static final List<SimpleColorPalette> DEFAULT_PAINT_SCALES = List.of(BLUE_YELLOW,
      GREEN_YELLOW, BLUE_RED_WHITE, RAINBOW, BLUE_ORANGE_WHITE);
  protected static final SimpleColorPalette DEFAULT_NORMAL = new SimpleColorPalette(
      ColorsFX.getSevenColorPalette(Vision.NORMAL_VISION, true), "Normal",
      ColorsFX.getPositiveColor(Vision.NORMAL_VISION), ColorsFX.getNeutralColor(),
      ColorsFX.getNegativeColor(Vision.NORMAL_VISION));
  protected static final SimpleColorPalette DEFAULT_DEUTERANOPIA = new SimpleColorPalette(
      ColorsFX.getSevenColorPalette(Vision.DEUTERANOPIA, true), "Deuteranopia",
      ColorsFX.getPositiveColor(Vision.DEUTERANOPIA), ColorsFX.getNeutralColor(),
      ColorsFX.getNegativeColor(Vision.DEUTERANOPIA));
  protected static final SimpleColorPalette DEFAULT_PROTANOPIA = new SimpleColorPalette(
      ColorsFX.getSevenColorPalette(Vision.PROTANOPIA, true), "Protanopia",
      ColorsFX.getPositiveColor(Vision.PROTANOPIA), ColorsFX.getNeutralColor(),
      ColorsFX.getNegativeColor(Vision.PROTANOPIA));
  protected static final SimpleColorPalette DEFAULT_TRITANOPIA = new SimpleColorPalette(
      ColorsFX.getSevenColorPalette(Vision.TRITANOPIA, true), "Tritanopia",
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

  public SimpleColorPalette() {
    super();
    delegate = new ArrayList<>();
    next = 0;
    name = "";
    positiveColor = ColorsFX.getPositiveColor(Vision.DEUTERANOPIA);
    negativeColor = ColorsFX.getNegativeColor(Vision.DEUTERANOPIA);
    neutralColor = ColorsFX.getNeutralColor();
  }

  public SimpleColorPalette(@NotNull Color... clrs) {
    this();
    for (Color clr : clrs) {
      add(clr);
    }
  }

  public SimpleColorPalette(@NotNull Color[] clrs, @NotNull String name) {
    this(clrs);
    setName(name);
  }

  public SimpleColorPalette(@NotNull Color[] clrs, @NotNull String name, Color positiveColor,
      Color neutralColor, Color negativeColor) {
    this(clrs, name);

    setPositiveColor(positiveColor);
    setNeutralColor(neutralColor);
    setNegativeColor(negativeColor);
  }

  public static SimpleColorPalette createFromXML(Element xmlElement) {
    SimpleColorPalette p = new SimpleColorPalette();
    p.loadFromXML(xmlElement);
    return p;
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
  public synchronized Color getNextColor() {

    if (this.isEmpty()) {
      logger.fine("Color palette empty, returning default color.");
      return defclr;
    }

    if (next >= this.size()) {
      next = 0;
    }

    Color clr = get(next);
    next++;

    return clr;
  }

  /**
   * @param exclusion A color to be visually different from.
   * @return A visually different color.
   */
  public synchronized Color getNextColor(@NotNull final Color exclusion) {
    final int startNext = next;

    do {
      var clr = getNextColor();
      if (ColorUtils.getColorDifference(clr, exclusion) > ColorUtils.MIN_REDMEAN_COLOR_DIFF) {
        return clr; // this color is different
      }
    } while (startNext != next);
    return getNextColor(); // use the color we should use originally
  }

  /**
   * @param exclusion A color to be visually different from.
   * @return A visually different color.
   */
  public synchronized java.awt.Color getNextColorAWT(@NotNull final java.awt.Color exclusion) {
    return FxColorUtil.fxColorToAWT(getNextColor(FxColorUtil.awtColorToFX(exclusion)));
  }

  public java.awt.Color getNextColorAWT() {
    return FxColorUtil.fxColorToAWT(getNextColor());
  }

  @NotNull
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
    if (this.isEmpty()) {
      return false;
    }
    if (positiveColor == null || negativeColor == null || neutralColor == null) {
      return false;
    }
    return true;
  }

  @NotNull
  public String getName() {
    return name;
  }

  public void setName(@NotNull String name) {
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

  public PaintScale toPaintScale(PaintScaleTransform transform, Range<Double> valueRange) {
    var paintScale = new PaintScale(valueRange);
    PaintScaleFactory psf = new PaintScaleFactory();
    return psf.createColorsForCustomPaintScaleFX(paintScale, transform, this);
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
    return clone(false);
  }

  public SimpleColorPalette clone(boolean resetIndex) {
    SimpleColorPalette clone = new SimpleColorPalette();
    for (Color clr : this) {
      clone.add(clr);
    }
    clone.setName(getName());
    clone.setNegativeColor(getNegativeColor());
    clone.setPositiveColor(getPositiveColor());
    clone.setColorCounter(resetIndex ? 0 : next);
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

  public void setPositiveColor(Color positiveColor) {
    this.positiveColor = positiveColor;
    fireChange(
        new ColorPaletteChangedEvent(this, ColorPaletteChangeEventType.POSITIVE_MARKER_UPDATED,
            positiveColor));
  }

  public java.awt.Color getPositiveColorAWT() {
    return FxColorUtil.fxColorToAWT(getPositiveColor());
  }

  public Color getNegativeColor() {
    return negativeColor;
  }

  public void setNegativeColor(Color negativeColor) {
    this.negativeColor = negativeColor;
    fireChange(
        new ColorPaletteChangedEvent(this, ColorPaletteChangeEventType.NEGATIVE_MARKER_UPDATED,
            negativeColor));
  }

  public java.awt.Color getNegativeColorAWT() {
    return FxColorUtil.fxColorToAWT(getNegativeColor());
  }

  public Color getNeutralColor() {
    return neutralColor;
  }

  public void setNeutralColor(Color neutralColor) {
    this.neutralColor = neutralColor;
    fireChange(
        new ColorPaletteChangedEvent(this, ColorPaletteChangeEventType.NEUTRAL_MARKER_UPDATED,
            neutralColor));
  }

  public java.awt.Color getNeutralColorAWT() {
    return FxColorUtil.fxColorToAWT(getNeutralColor());
  }

  // --- super type
  @Override
  public Color get(int index) {
    if (index < delegate.size()) {
      return delegate.get(index);
    }
    return delegate.get(index % delegate.size());
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

  /**
   * @param nextIndex this will be the next color index
   */
  public void setColorCounter(int nextIndex) {
    next = nextIndex;
  }

  public void resetColorCounter() {
    setColorCounter(0);
  }
}
