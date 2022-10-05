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

package io.github.mzmine.parameters.parametertypes;

import java.util.Collection;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import io.github.mzmine.parameters.Parameter;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class WindowSettingsParameter implements Parameter<Object> {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private static final String POSX_ELEMENT = "posX";
  private static final String POSY_ELEMENT = "posY";
  private static final String WIDTH_ELEMENT = "width";
  private static final String HEIGHT_ELEMENT = "height";
  private static final String MAXIMIZED_ELEMENT = "maximized";

  private @Nullable Double posX, posY, width, height;
  private @Nullable Integer screenIndex;
  private @Nullable Boolean isMaximized;

  @Override
  public String getName() {
    return "Window state";
  }

  @Override
  public WindowSettingsParameter cloneParameter() {
    return this;
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    return true;
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {

    // Window position
    NodeList posXElement = xmlElement.getElementsByTagName(POSX_ELEMENT);
    if (posXElement.getLength() == 1) {
      String posXString = posXElement.item(0).getTextContent();
      posX = Double.parseDouble(posXString);
    }
    NodeList posYElement = xmlElement.getElementsByTagName(POSY_ELEMENT);
    if (posYElement.getLength() == 1) {
      String posYString = posYElement.item(0).getTextContent();
      posY = Double.parseDouble(posYString);
    }

    // Window size
    NodeList widthElement = xmlElement.getElementsByTagName(WIDTH_ELEMENT);
    if (widthElement.getLength() == 1) {
      String widthString = widthElement.item(0).getTextContent();
      width = Double.parseDouble(widthString);
    }

    NodeList heightElement = xmlElement.getElementsByTagName(HEIGHT_ELEMENT);
    if (heightElement.getLength() == 1) {
      String heightString = heightElement.item(0).getTextContent();
      height = Double.parseDouble(heightString);
    }

    // Window maximized
    NodeList maximizedElement = xmlElement.getElementsByTagName(MAXIMIZED_ELEMENT);
    if (maximizedElement.getLength() == 1) {
      String maximizedString = maximizedElement.item(0).getTextContent();
      isMaximized = Boolean.valueOf(maximizedString);
    }

  }

  @Override
  public void saveValueToXML(Element xmlElement) {

    // Add elements
    Document doc = xmlElement.getOwnerDocument();

    if (posX != null) {
      Element posXElement = doc.createElement(POSX_ELEMENT);
      xmlElement.appendChild(posXElement);
      posXElement.setTextContent(posX.toString());
    }

    if (posY != null) {
      Element posYElement = doc.createElement(POSY_ELEMENT);
      xmlElement.appendChild(posYElement);
      posYElement.setTextContent(posY.toString());
    }

    if (width != null) {
      Element widthElement = doc.createElement(WIDTH_ELEMENT);
      xmlElement.appendChild(widthElement);
      widthElement.setTextContent(width.toString());
    }

    if (height != null) {
      Element heightElement = doc.createElement(HEIGHT_ELEMENT);
      xmlElement.appendChild(heightElement);
      heightElement.setTextContent(height.toString());
    }

    if (isMaximized != null) {
      Element maximizedElement = doc.createElement(MAXIMIZED_ELEMENT);
      xmlElement.appendChild(maximizedElement);
      maximizedElement.setTextContent(isMaximized.toString());
    }
  }

  @Override
  public Object getValue() {
    return null;
  }

  @Override
  public void setValue(Object newValue) {
    // ignore
  }

  /**
   * Set window size and position according to the values in this instance
   */
  public void applySettingsToWindow(@NotNull final Stage stage) {

    logger.finest("Setting window " + stage.getTitle() + " position " + posX + ":" + posY
        + " and size " + width + "x" + height);
    if (posX != null)
      stage.setX(posX);
    if (posY != null)
      stage.setX(posY);
    if (width != null)
      stage.setWidth(width);
    if (height != null)
      stage.setHeight(height);

    if ((isMaximized != null) && isMaximized) {
      logger.finest("Setting window " + stage.getTitle() + " to maximized");
      stage.setMaximized(true);
    }

    // when still outside of screen
    // e.g. changing from 2 screens to one
    if (stage.isShowing() && !isOnScreen(stage)) {
      // Maximize on screen 1
      logger.finest(
          "Window " + stage.getTitle() + " is not on screen, setting to maximized on screen 1");
      stage.setX(0.0);
      stage.setY(0.0);
      stage.sizeToScene();
      stage.setMaximized(true);
    }

    ChangeListener<Number> stageListener = (observable, oldValue, newValue) -> {
      posX = stage.getX();
      posY = stage.getY();
      width = stage.getWidth();
      height = stage.getHeight();
    };
    stage.widthProperty().addListener(stageListener);
    stage.heightProperty().addListener(stageListener);
    stage.xProperty().addListener(stageListener);
    stage.yProperty().addListener(stageListener);

  }

  private boolean isOnScreen(Stage stage) {
    Rectangle2D stagePosition =
        new Rectangle2D(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
    var screens = Screen.getScreensForRectangle(stagePosition);
    return !screens.isEmpty();
  }

}
