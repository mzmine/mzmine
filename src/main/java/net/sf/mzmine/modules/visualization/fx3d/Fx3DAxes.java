package net.sf.mzmine.modules.visualization.fx3d;

import com.google.common.collect.Range;

import javafx.scene.Group;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import net.sf.mzmine.main.MZmineCore;

public class Fx3DAxes extends Group {

    private static final int SIZE = 500;
    private static float AMPLIFI = 130;

    public Fx3DAxes(Range<Double> rtRange, Range<Double> mzRange,
            double maxBinnedIntensity) {
        // rtAxis
        double rtDelta = (rtRange.upperEndpoint() - rtRange.lowerEndpoint())
                / 7;
        double rtscaleValue = rtRange.lowerEndpoint();
        Text rtLabel = new Text("Retention Time");
        rtLabel.setRotationAxis(Rotate.X_AXIS);
        rtLabel.setRotate(-45);
        rtLabel.setTranslateX(SIZE * 3 / 8);
        rtLabel.setTranslateZ(-25);
        rtLabel.setTranslateY(13);
        this.getChildren().add(rtLabel);
        for (int y = 0; y <= SIZE; y += SIZE / 7) {
            Line tickLineX = new Line(0, 0, 0, 9);
            tickLineX.setRotationAxis(Rotate.X_AXIS);
            tickLineX.setRotate(-90);
            tickLineX.setTranslateY(-4);
            tickLineX.setTranslateX(y);
            tickLineX.setTranslateZ(-3.5);
            Text text = new Text("" + (int) rtscaleValue);
            text.setRotationAxis(Rotate.X_AXIS);
            text.setRotate(-45);
            text.setTranslateY(9);
            text.setTranslateX(y - 5);
            text.setTranslateZ(-15);
            rtscaleValue += rtDelta;
            this.getChildren().addAll(text, tickLineX);
        }

        // mzAxis
        double mzDelta = (mzRange.upperEndpoint() - mzRange.lowerEndpoint())
                / 7;
        double mzScaleValue = mzRange.upperEndpoint();
        Group mzAxisTicks = new Group();
        Group mzAxisLabels = new Group();
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

        // intensityAxis

        int numScale = 5;
        double gapLen = (AMPLIFI / numScale);
        double transLen = 0;
        double intensityDelta = maxBinnedIntensity / numScale;
        double intensityValue = 0;

        Text intensityLabel = new Text("Intensity");
        intensityLabel.setTranslateX(-75);
        intensityLabel.setRotationAxis(Rotate.Y_AXIS);
        intensityLabel.setRotate(-45);
        intensityLabel.setRotationAxis(Rotate.Z_AXIS);
        intensityLabel.setRotate(90);
        intensityLabel.setTranslateZ(-40);
        intensityLabel.setTranslateY(-70);
        this.getChildren().add(intensityLabel);
        for (int y = 0; y <= numScale; y++) {
            Line tickLineY = new Line(0, 0, 7, 0);
            tickLineY.setRotationAxis(Rotate.Y_AXIS);
            tickLineY.setRotate(135);
            tickLineY.setTranslateX(-6);
            tickLineY.setTranslateZ(-3);
            tickLineY.setTranslateY(-transLen);
            this.getChildren().add(tickLineY);

            Text text = new Text("" + MZmineCore.getConfiguration()
                    .getIntensityFormat().format(intensityValue));
            intensityValue += intensityDelta;
            text.setRotationAxis(Rotate.Y_AXIS);
            text.setRotate(-45);
            text.setTranslateY(-transLen + 5);
            text.setTranslateX(-40);
            text.setTranslateZ(-26);
            this.getChildren().add(text);
            transLen += gapLen;
        }

        Line lineX = new Line(0, 0, SIZE, 0);
        this.getChildren().add(lineX);
        Line lineZ = new Line(0, 0, SIZE, 0);
        lineZ.setRotationAxis(Rotate.Y_AXIS);
        lineZ.setRotate(90);
        lineZ.setTranslateX(-SIZE / 2);
        lineZ.setTranslateZ(SIZE / 2);
        this.getChildren().add(lineZ);
        Line lineY = new Line(0, 0, AMPLIFI, 0);
        lineY.setRotate(90);
        lineY.setTranslateX(-AMPLIFI / 2);
        lineY.setTranslateY(-AMPLIFI / 2);
        this.getChildren().add(lineY);

    }
}
