package net.sf.mzmine.modules.visualization.fx3d;

import java.util.logging.Logger;

import com.google.common.collect.Range;

import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import net.sf.mzmine.parameters.parametertypes.selectors.FeatureSelection;

public class Fx3DFeatureDataset {

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

    Fx3DFeatureDataset(FeatureSelection featureSel, int rtResolution,
            int mzResolution, Range<Double> rtRange, Range<Double> mzRange,
            double maxOfAllBinnedIntensity) {
        this.featureSelection = featureSel;
        this.featureRtRange = featureSel.getFeature().getRawDataPointsRTRange();
        this.featureMzRange = featureSel.getFeature().getRawDataPointsMZRange();
        this.plotRtRange = rtRange;
        this.plotMzRange = mzRange;
        this.maxIntensityValue = featureSel.getFeature()
                .getRawDataPointsIntensityRange().upperEndpoint();
        // mesh = new TriangleMesh();

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
        featureBox = new Box(width * factorX, 20, depth * factorZ);
        featureBox.setTranslateX((minFeatureRtPoint + width / 2) * factorX);
        // featureBox.setTranslateY(-maxIntensityValue * AMPLIFI / 2);
        featureBox.setTranslateZ((minFeatureMzPoint + depth / 2) * factorZ);
        PhongMaterial material = new PhongMaterial();
        material.setDiffuseColor(Color.rgb(165, 42, 42, 0.5));

        featureBox.setMaterial(material);
    }

    public Box getFeatureBox() {
        return featureBox;
    }

    public void normalizeHeight(double maxOfAllBinnedIntensity) {
        featureBox.setHeight(
                (maxIntensityValue / maxOfAllBinnedIntensity) * AMPLIFI);
        LOG.finest("Final height is:"
                + (maxIntensityValue / maxOfAllBinnedIntensity) * AMPLIFI);
        featureBox.setTranslateY(
                -(maxIntensityValue / maxOfAllBinnedIntensity) * AMPLIFI / 2);
    }

    public FeatureSelection getFeatureSelection() {
        return featureSelection;
    }

}
