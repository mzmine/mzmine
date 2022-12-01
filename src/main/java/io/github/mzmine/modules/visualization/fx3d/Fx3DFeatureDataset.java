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

import io.github.mzmine.datamodel.features.Feature;
import java.util.logging.Logger;
import com.google.common.collect.Range;
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
  private final Feature feature;
  private Range<Float> featureRtRange;
  private Range<Double> featureMzRange;
  private Box featureBox;
  private Range<Float> plotRtRange;
  private Range<Double> plotMzRange;
  private double maxIntensityValue;
  private static final Logger logger = Logger.getLogger(Fx3DFeatureDataset.class.getName());

  public Fx3DFeatureDataset(Feature feature, int rtResolution, int mzResolution,
      Range<Float> rtRange, Range<Double> mzRange, double maxOfAllBinnedIntensity,
      Color featureColor) {
    super(null, feature.toString(), featureColor);
    this.feature = feature;
    this.featureRtRange = feature.getRawDataPointsRTRange();
    this.featureMzRange = feature.getRawDataPointsMZRange();
    this.plotRtRange = rtRange;
    this.plotMzRange = mzRange;
    this.maxIntensityValue = feature.getRawDataPointsIntensityRange().upperEndpoint();

    float factorX = (float) SIZE / rtResolution;
    float factorZ = (float) SIZE / mzResolution;

    double rtSlope = SIZE / (plotRtRange.upperEndpoint() - plotRtRange.lowerEndpoint());
    double mzSlope = SIZE / (plotMzRange.upperEndpoint() - plotMzRange.lowerEndpoint());
    logger.finest("RtSlope is:" + rtSlope);
    logger.finest("MzSlope is:" + mzSlope);
    double minFeatureRtPoint =
        (featureRtRange.lowerEndpoint() - plotRtRange.lowerEndpoint()) * rtSlope;
    double maxFeatureRtPoint =
        (featureRtRange.upperEndpoint() - plotRtRange.lowerEndpoint()) * rtSlope;
    double minFeatureMzPoint =
        (featureMzRange.lowerEndpoint() - plotMzRange.lowerEndpoint()) * mzSlope;
    double maxFeatureMzPoint =
        (featureMzRange.upperEndpoint() - plotMzRange.lowerEndpoint()) * mzSlope;
    logger.finest("minRTPoint:" + minFeatureRtPoint + "  maxRTPoint:" + maxFeatureRtPoint);
    logger.finest("minMzPoint:" + minFeatureMzPoint + "  maxMzPoint:" + maxFeatureMzPoint);
    logger.finest("maxIntensityValue is:" + maxIntensityValue * AMPLIFI);
    logger.finest("maxOfAllBinnedIntensity value is:" + maxOfAllBinnedIntensity * AMPLIFI);
    double width = maxFeatureRtPoint - minFeatureRtPoint;
    double depth = maxFeatureMzPoint - minFeatureMzPoint;
    logger.finest("width is: " + width);
    logger.finest("depth is:" + depth);
    featureBox = new Box(width * factorX, maxIntensityValue * AMPLIFI, depth * factorZ);
    featureBox.setTranslateX((minFeatureRtPoint + width / 2) * factorX);
    featureBox.setTranslateY(-maxIntensityValue * AMPLIFI / 2);
    featureBox.setTranslateZ((minFeatureMzPoint + depth / 2) * factorZ);
    setNodeColor(featureColor);
  }

  public Feature getFeature() {
    return feature;
  }

  @Override
  public Node getNode() {
    return featureBox;
  }

  /*
   * Normalizes each feature when the maxIntensity of the 3D plot changes.
   *
   * @see io.github.mzmine.modules.visualization.fx3d.Fx3DAbstractDataset# normalize( double)
   */
  @Override
  public void normalize(double maxOfAllBinnedIntensities) {
    featureBox.setHeight((maxIntensityValue / maxOfAllBinnedIntensities) * AMPLIFI);
    logger.finest("Final height is:" + (maxIntensityValue / maxOfAllBinnedIntensities) * AMPLIFI);
    featureBox.setTranslateY(-(maxIntensityValue / maxOfAllBinnedIntensities) * AMPLIFI / 2);
  }

  /*
   * Sets the color of the containing box.
   *
   * @see io.github.mzmine.modules.visualization.fx3d.Fx3DAbstractDataset# setNodeColor
   * (javafx.scene.paint.Color)
   */
  @Override
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
    return feature;
  }

}
