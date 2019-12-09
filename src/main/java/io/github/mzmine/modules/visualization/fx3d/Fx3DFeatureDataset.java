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

import java.util.logging.Logger;

import com.google.common.collect.Range;

import io.github.mzmine.datamodel.Feature;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureSelection;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;

/**
 * @author akshaj This class represents the dataset of a Feature.
 */
public class Fx3DFeatureDataset extends Fx3DAbstractDataset {

    private static final int SIZE = 500;
    private static float AMPLIFI = 130;
    private FeatureSelection featureSelection;
    private Range<Double> featureRtRange;
    private Range<Double> featureMzRange;
    private Box featureBox;
    private Range<Double> plotRtRange;
    private Range<Double> plotMzRange;
    private double maxIntensityValue;
    private static final Logger LOG = Logger
            .getLogger(Fx3DFeatureDataset.class.getName());

    public Fx3DFeatureDataset(FeatureSelection featureSel, int rtResolution,
            int mzResolution, Range<Double> rtRange, Range<Double> mzRange,
            double maxOfAllBinnedIntensity, Color featureColor) {
        super(featureSel.getRawDataFile(), featureSel.getFeature().toString(),
                featureColor);
        this.featureSelection = featureSel;
        this.featureRtRange = featureSel.getFeature().getRawDataPointsRTRange();
        this.featureMzRange = featureSel.getFeature().getRawDataPointsMZRange();
        this.plotRtRange = rtRange;
        this.plotMzRange = mzRange;
        this.maxIntensityValue = featureSel.getFeature()
                .getRawDataPointsIntensityRange().upperEndpoint();

        float factorX = (float) SIZE / rtResolution;
        float factorZ = (float) SIZE / mzResolution;

        double rtSlope = (double) ((double) SIZE
                / (plotRtRange.upperEndpoint() - plotRtRange.lowerEndpoint()));
        double mzSlope = (double) ((double) SIZE
                / (plotMzRange.upperEndpoint() - plotMzRange.lowerEndpoint()));
        LOG.finest("RtSlope is:" + rtSlope);
        LOG.finest("MzSlope is:" + mzSlope);
        double minFeatureRtPoint = (double) ((featureRtRange.lowerEndpoint()
                - plotRtRange.lowerEndpoint()) * rtSlope);
        double maxFeatureRtPoint = (double) ((featureRtRange.upperEndpoint()
                - plotRtRange.lowerEndpoint()) * rtSlope);
        double minFeatureMzPoint = (double) ((featureMzRange.lowerEndpoint()
                - plotMzRange.lowerEndpoint()) * mzSlope);
        double maxFeatureMzPoint = (double) ((featureMzRange.upperEndpoint()
                - plotMzRange.lowerEndpoint()) * mzSlope);
        LOG.finest("minRTPoint:" + minFeatureRtPoint + "  maxRTPoint:"
                + maxFeatureRtPoint);
        LOG.finest("minMzPoint:" + minFeatureMzPoint + "  maxMzPoint:"
                + maxFeatureMzPoint);
        LOG.finest("maxIntensityValue is:" + maxIntensityValue * AMPLIFI);
        LOG.finest("maxOfAllBinnedIntensity value is:"
                + maxOfAllBinnedIntensity * AMPLIFI);
        double width = maxFeatureRtPoint - minFeatureRtPoint;
        double depth = maxFeatureMzPoint - minFeatureMzPoint;
        LOG.finest("width is: " + width);
        LOG.finest("depth is:" + depth);
        featureBox = new Box(width * factorX, maxIntensityValue * AMPLIFI,
                depth * factorZ);
        featureBox.setTranslateX((minFeatureRtPoint + width / 2) * factorX);
        featureBox.setTranslateY(-maxIntensityValue * AMPLIFI / 2);
        featureBox.setTranslateZ((minFeatureMzPoint + depth / 2) * factorZ);
        setNodeColor(featureColor);
    }

    public FeatureSelection getFeatureSelection() {
        return featureSelection;
    }

    @Override
    public Node getNode() {
        return featureBox;
    }

    /*
     * Normalizes each feature when the maxIntensity of the 3D plot changes.
     * 
     * @see io.github.mzmine.modules.visualization.fx3d.Fx3DAbstractDataset#
     * normalize( double)
     */
    public void normalize(double maxOfAllBinnedIntensities) {
        featureBox.setHeight(
                (maxIntensityValue / maxOfAllBinnedIntensities) * AMPLIFI);
        LOG.finest("Final height is:"
                + (maxIntensityValue / maxOfAllBinnedIntensities) * AMPLIFI);
        featureBox.setTranslateY(
                -(maxIntensityValue / maxOfAllBinnedIntensities) * AMPLIFI / 2);
    }

    /*
     * Sets the color of the containing box.
     * 
     * @see io.github.mzmine.modules.visualization.fx3d.Fx3DAbstractDataset#
     * setNodeColor (javafx.scene.paint.Color)
     */
    public void setNodeColor(Color featureColor) {
        PhongMaterial material = new PhongMaterial();
        material.setDiffuseColor(featureColor);
        featureBox.setMaterial(material);
    }

    @Override
    public double getMaxBinnedIntensity() {
        return 0;
    }

    @Override
    public Feature getFile() {
        return this.featureSelection.getFeature();
    }

}
