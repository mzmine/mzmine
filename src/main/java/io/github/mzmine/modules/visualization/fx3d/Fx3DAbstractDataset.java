/*
 * Copyright 2006-2020 The MZmine Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
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
 * @author akshaj Abstract class to represent a data that can be plotted in the
 *         visualizer.
 */
abstract class Fx3DAbstractDataset {

    private RawDataFile dataFile;
    private SimpleStringProperty fileName = new SimpleStringProperty("");
    private ObjectProperty<Color> color = new SimpleObjectProperty<>(this,
            "color");
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
     * @param maxOfAllBinnedIntensities
     *            Normalizes the dataset according to the max Intensity so that
     *            the graph remains always within the axes.
     */
    public abstract void normalize(double maxOfAllBinnedIntensities);

    public abstract void setNodeColor(Color nodeColor);

    public abstract double getMaxBinnedIntensity();

    public abstract Object getFile();
}
