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
package io.github.mzmine.modules.visualization.fx3d;

import io.github.mzmine.datamodel.RawDataFile;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Node;
import javafx.scene.paint.Color;

/**
 * @author akshaj Abstract class to represent a data that can be plotted in the visualizer.
 */
abstract class Fx3DAbstractDataset {

  private RawDataFile dataFile;
  private SimpleStringProperty fileName = new SimpleStringProperty("");
  private ObjectProperty<Color> color = new SimpleObjectProperty<>(this, "color");
  private SimpleDoubleProperty opacity = new SimpleDoubleProperty();
  private SimpleBooleanProperty visibility = new SimpleBooleanProperty();

  Fx3DAbstractDataset(RawDataFile dataFile, String fileName, Color color) {
    this.dataFile = dataFile;
    this.fileName.set(fileName);
    this.color.set(color);
    this.opacity.set(1.0);
    this.visibility.set(true);
  }

  public RawDataFile getDataFile() {
    return this.dataFile;
  }

  public String getFileName() {
    return fileName.get();
  }

  public Color getColor() {
    return color.get();
  }

  public void setColor(Color newColor) {
    color.set(newColor);
  }

  public ObjectProperty<Color> colorProperty() {
    return color;
  }

  public Double getOpacity() {
    return opacity.get();
  }

  public void setOpacity(double value) {
    opacity.set(value);
  }

  public SimpleDoubleProperty opacityProperty() {
    return opacity;
  }

  public boolean getVisibility() {
    return visibility.get();
  }

  public void setVisibility(boolean value) {
    visibility.set(value);
  }

  public SimpleBooleanProperty visibilityProperty() {
    return visibility;
  }

  public abstract Node getNode();

  /**
   * @param maxOfAllBinnedIntensities Normalizes the dataset according to the max Intensity so that
   *        the graph remains always within the axes.
   */
  public abstract void normalize(double maxOfAllBinnedIntensities);

  public abstract void setNodeColor(Color nodeColor);

  public abstract double getMaxBinnedIntensity();

  public abstract Object getFile();
}
