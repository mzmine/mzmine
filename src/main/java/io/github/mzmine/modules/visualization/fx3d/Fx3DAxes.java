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

import com.google.common.collect.Range;

import io.github.mzmine.main.MZmineCore;
import javafx.scene.Group;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;

/**
 * @author akshaj This class represents the axes of the 3D plot.
 */
public class Fx3DAxes extends Group {

  private static final int SIZE = 500;
  private static float AMPLIFI = 130;

  private Group rtAxis = new Group();
  private Group mzAxis = new Group();
  private Group intensityAxis = new Group();

  private Group mzAxisTicks = new Group();
  private Group mzAxisLabels = new Group();

  public Rotate rtRotate = new Rotate(0, Rotate.Y_AXIS);
  public Translate rtTranslate = new Translate();

  public Rotate mzRotate = new Rotate(0, Rotate.Z_AXIS);
  public Translate mzTranslate = new Translate();

  public Rotate intensityRotate = new Rotate(0, Rotate.Y_AXIS);
  public Translate intensityTranslate = new Translate();

  public Fx3DAxes() {
    rtAxis.getTransforms().addAll(rtRotate, rtTranslate);
    mzAxis.getTransforms().addAll(mzRotate, mzTranslate);
    intensityAxis.getTransforms().addAll(intensityRotate, intensityTranslate);
  }

  /**
   * @param rtRange
   * @param mzRange
   * @param maxBinnedIntensity Sets the values to the axes according to the range of RT, MZ and
   *        maxIntensity.
   */
  public void setValues(Range<Float> rtRange, Range<Double> mzRange, double maxBinnedIntensity) {
    // rtAxis
    double rtDelta = (rtRange.upperEndpoint() - rtRange.lowerEndpoint()) / 7;
    double rtScaleValue = rtRange.lowerEndpoint();
    Text rtLabel = new Text("Retention Time");
    rtLabel.setRotationAxis(Rotate.X_AXIS);
    rtLabel.setRotate(-45);
    rtLabel.setTranslateX(SIZE * 3 / 8);
    rtLabel.setTranslateZ(-25);
    rtLabel.setTranslateY(13);
    rtAxis.getChildren().add(rtLabel);
    for (int y = 0; y <= SIZE; y += SIZE / 7) {
      Line tickLineX = new Line(0, 0, 0, 9);
      tickLineX.setRotationAxis(Rotate.X_AXIS);
      tickLineX.setRotate(-90);
      tickLineX.setTranslateY(-4);
      tickLineX.setTranslateX(y);
      tickLineX.setTranslateZ(-3.5);
      float roundOff = (float) (Math.round(rtScaleValue * 10.0) / 10.0);
      Text text = new Text("" + (float) roundOff);
      text.setRotationAxis(Rotate.X_AXIS);
      text.setRotate(-45);
      text.setTranslateY(9);
      text.setTranslateX(y - 5);
      text.setTranslateZ(-15);
      rtScaleValue += rtDelta;
      rtAxis.getChildren().addAll(text, tickLineX);
    }
    this.getChildren().add(rtAxis);
    // mzAxis
    double mzDelta = (mzRange.upperEndpoint() - mzRange.lowerEndpoint()) / 7;
    double mzScaleValue = mzRange.upperEndpoint();

    Text mzLabel = new Text("m/z");
    mzLabel.setRotationAxis(Rotate.X_AXIS);
    mzLabel.setRotate(-45);
    mzLabel.setTranslateX(SIZE / 2);
    mzLabel.setTranslateZ(-5);
    mzLabel.setTranslateY(8);
    mzAxisLabels.getChildren().add(mzLabel);
    for (int y = 0; y <= SIZE; y += SIZE / 7) {
      Line tickLineZ = new Line(0, 0, 0, 9);
      tickLineZ.setRotationAxis(Rotate.X_AXIS);
      tickLineZ.setRotate(-90);
      tickLineZ.setTranslateY(-4);
      tickLineZ.setTranslateX(y - 2);
      float roundOff = (float) (Math.round(mzScaleValue * 100.0) / 100.0);
      Text text = new Text("" + (float) roundOff);
      text.setRotationAxis(Rotate.X_AXIS);
      text.setRotate(-45);
      text.setTranslateY(8);
      text.setTranslateX(y - 10);
      text.setTranslateZ(20);
      mzScaleValue -= mzDelta;
      mzAxisTicks.getChildren().add(tickLineZ);
      mzAxisLabels.getChildren().add(text);
    }
    mzAxisTicks.setRotationAxis(Rotate.Y_AXIS);
    mzAxisTicks.setRotate(90);
    mzAxisTicks.setTranslateX(-SIZE / 2);
    mzAxisTicks.setTranslateZ(SIZE / 2);
    mzAxisLabels.setRotationAxis(Rotate.Y_AXIS);
    mzAxisLabels.setRotate(90);
    mzAxisLabels.setTranslateX(-SIZE / 2 - SIZE / 14);
    mzAxisLabels.setTranslateZ(SIZE / 2);
    this.getChildren().addAll(mzAxisTicks, mzAxisLabels);
    this.getChildren().add(mzAxis);
    // intensityAxis

    int numScale = 5;
    double gapLen = (AMPLIFI / numScale);
    double transLen = 0;
    double intensityDelta = maxBinnedIntensity / numScale;
    double intensityValue = 0;

    Text intensityLabel = new Text("Intensity");
    intensityLabel.setTranslateX(-75);
    intensityLabel.setRotationAxis(Rotate.Z_AXIS);
    intensityLabel.setRotate(-90);
    intensityLabel.setTranslateZ(-40);
    intensityLabel.setTranslateY(-70);
    intensityAxis.getChildren().add(intensityLabel);
    for (int y = 0; y <= numScale; y++) {
      Line tickLineY = new Line(0, 0, 7, 0);
      tickLineY.setRotationAxis(Rotate.Y_AXIS);
      tickLineY.setRotate(135);
      tickLineY.setTranslateX(-6);
      tickLineY.setTranslateZ(-3);
      tickLineY.setTranslateY(-transLen);
      intensityAxis.getChildren().add(tickLineY);

      Text text =
          new Text("" + MZmineCore.getConfiguration().getIntensityFormat().format(intensityValue));
      intensityValue += intensityDelta;
      text.setRotationAxis(Rotate.Y_AXIS);
      text.setRotate(-45);
      text.setTranslateY(-transLen + 5);
      text.setTranslateX(-40);
      text.setTranslateZ(-26);
      intensityAxis.getChildren().add(text);
      transLen += gapLen;
    }
    this.getChildren().add(intensityAxis);
    Line lineX = new Line(0, 0, SIZE, 0);
    rtAxis.getChildren().add(lineX);
    Line lineZ = new Line(0, 0, SIZE, 0);
    lineZ.setRotationAxis(Rotate.Y_AXIS);
    lineZ.setRotate(90);
    lineZ.setTranslateX(-SIZE / 2);
    lineZ.setTranslateZ(SIZE / 2);
    mzAxis.getChildren().add(lineZ);
    Line lineY = new Line(0, 0, AMPLIFI, 0);
    lineY.setRotate(90);
    lineY.setTranslateX(-AMPLIFI / 2);
    lineY.setTranslateY(-AMPLIFI / 2);
    intensityAxis.getChildren().add(lineY);
  }

  public Rotate getRtRotate() {
    return rtRotate;
  }

  public Rotate getMzRotate() {
    return mzRotate;
  }

  public Rotate getIntensityRotate() {
    return intensityRotate;
  }

  public Translate getRtTranslate() {
    return rtTranslate;
  }

  public Translate getIntensityTranslate() {
    return intensityTranslate;
  }

  public Group getRtAxis() {
    return rtAxis;
  }

  public Group getMzAxis() {
    return mzAxis;
  }

  public Group getMzAxisLabels() {
    return mzAxisLabels;
  }

  public Group getMzAxisTicks() {
    return mzAxisTicks;
  }

  /**
   * @param rtRange
   * @param mzRange
   * @param maxBinnedIntensity Updates the axes values according to the new ranges of RT,MZ and
   *        maximum intensity.
   */
  public void updateAxisParameters(Range<Float> rtRange, Range<Double> mzRange,
      double maxBinnedIntensity) {
    this.getChildren().clear();
    rtRotate.setAngle(0);
    mzRotate.setAngle(0);
    intensityRotate.setAngle(0);
    rtTranslate.setX(0);
    rtTranslate.setZ(0);
    intensityTranslate.setX(0);
    intensityTranslate.setZ(0);
    rtAxis.getChildren().clear();
    mzAxis.getChildren().clear();
    intensityAxis.getChildren().clear();
    mzAxisLabels.getChildren().clear();
    mzAxisTicks.getChildren().clear();
    setValues(rtRange, mzRange, maxBinnedIntensity);
  }

}
