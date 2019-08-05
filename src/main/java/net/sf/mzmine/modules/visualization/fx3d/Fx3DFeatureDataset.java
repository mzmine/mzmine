package net.sf.mzmine.modules.visualization.fx3d;

import java.util.logging.Logger;

import com.google.common.collect.Range;

import javafx.scene.shape.Box;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import net.sf.mzmine.datamodel.Feature;

public class Fx3DFeatureDataset {

    private static final int SIZE = 500;
    private static float AMPLIFI = 130;
    private Feature feature;
    private Range<Double> featureRtRange;
    private Range<Double> featureMzRange;
    private Box featureBox;
    private Range<Double> plotRtRange;
    private Range<Double> plotMzRange;
    private int rtResolution;
    private int mzResolution;
    private float maxIntensityValue;
    private MeshView meshView = new MeshView();
    private TriangleMesh mesh;
    private static final Logger LOG = Logger
            .getLogger(Fx3DRawDataFileDataset.class.getName());

    Fx3DFeatureDataset(Feature feature, int rtResolution, int mzResolution,
            Range<Double> plotRtRange, Range<Double> plotMzRange) {
        this.feature = feature;
        this.featureRtRange = feature.getRawDataPointsRTRange();
        this.featureMzRange = feature.getRawDataPointsMZRange();
        this.plotRtRange = plotRtRange;
        this.plotMzRange = plotMzRange;
        this.rtResolution = rtResolution;
        this.mzResolution = mzResolution;
        this.maxIntensityValue = feature.getRawDataPointsIntensityRange()
                .upperEndpoint().floatValue();
        mesh = new TriangleMesh();

        float factorX = (float) SIZE / rtResolution;
        float factorZ = (float) SIZE / mzResolution;

        float rtSlope = (float) (plotRtRange.upperEndpoint()
                - plotRtRange.lowerEndpoint()) / 500;
        float mzSlope = (float) (plotMzRange.upperEndpoint()
                - plotMzRange.lowerEndpoint() / 500);

        int minFeatureRtPoint = (int) (featureRtRange.lowerEndpoint()
                * rtSlope);
        int maxFeatureRtPoint = (int) (featureRtRange.upperEndpoint()
                * rtSlope);
        int minFeatureMzPoint = (int) (featureMzRange.lowerEndpoint()
                * mzSlope);
        int maxFeatureMzPoint = (int) (featureMzRange.upperEndpoint()
                * mzSlope);

        int width = maxFeatureRtPoint - minFeatureRtPoint;
        int depth = maxFeatureMzPoint - minFeatureMzPoint;

        featureBox = new Box(width * factorX, maxIntensityValue * AMPLIFI,
                depth * factorZ);
        featureBox.setTranslateX(width * factorX / 2);
        featureBox.setTranslateY(-maxIntensityValue * AMPLIFI / 2);
        featureBox.setTranslateZ(depth * factorZ / 2);

        /*
         * mesh.getPoints().addAll((float) minFeatureRtPoint - 1 * factorX,
         * -0.1f, (float) minFeatureMzPoint - 1 * factorZ);
         * mesh.getPoints().addAll((float) minFeatureRtPoint - 1 * factorX,
         * -0.1f, (float) maxFeatureMzPoint - 1 * factorZ);
         * mesh.getPoints().addAll((float) maxFeatureRtPoint - 1 * factorX,
         * -0.1f, (float) minFeatureMzPoint * factorZ);
         * mesh.getPoints().addAll((float) maxFeatureRtPoint - 1 * factorX,
         * -0.1f, (float) maxFeatureMzPoint - 1 * factorZ); for (int x =
         * minFeatureRtPoint; x < maxFeatureRtPoint; x++) { for (int z =
         * minFeatureMzPoint; z < maxFeatureMzPoint; z++) {
         * mesh.getPoints().addAll((float) x * factorX, -maxIntensityValue *
         * AMPLIFI, (float) z * factorZ); } }
         * 
         * int rtLength = maxFeatureRtPoint - minFeatureRtPoint + 2; int
         * mzLength = maxFeatureMzPoint - minFeatureMzPoint + 2; float rtTotal =
         * rtLength; float mzTotal = mzLength;
         * 
         * for (float x = 0; x < rtLength - 1; x++) { for (float y = 0; y <
         * mzLength - 1; y++) {
         * 
         * float x0 = x / rtTotal; float y0 = y / mzTotal; float x1 = (x + 1) /
         * rtTotal; float y1 = (y + 1) / mzTotal;
         * 
         * mesh.getTexCoords().addAll( // x0, y0, // 0, top-left x0, y1, // 1,
         * bottom-left x1, y0, // 2, top-right x1, y1 // 3, bottom-right ); } }
         * 
         * // faces for (int x = 0; x < rtLength - 1; x++) { for (int z = 0; z <
         * mzLength - 1; z++) {
         * 
         * int tl = x * mzLength + z; // top-left int bl = x * mzLength + z + 1;
         * // bottom-left int tr = (x + 1) * mzLength + z; // top-right int br =
         * (x + 1) * mzLength + z + 1; // bottom-right
         * 
         * int offset = (x * (mzLength - 1) + z) * 8 / 2; // div 2 because // we
         * have u AND // v in the list
         * 
         * // working mesh.getFaces().addAll(bl, offset + 1, tl, offset + 0, tr,
         * offset + 2); mesh.getFaces().addAll(tr, offset + 2, br, offset + 3,
         * bl, offset + 1);
         * 
         * } }
         * 
         * setFeatureColor(rtLength, mzLength); meshView.setMesh(mesh);
         * meshView.setCullFace(CullFace.NONE);
         * meshView.setDrawMode(DrawMode.FILL);
         * meshView.setDepthTest(DepthTest.ENABLE);
         * LOG.finest("Feature has been added.");
         */

    }

    /*
     * public void setFeatureColor(int width, int height) { WritableImage wr =
     * new WritableImage(width, height); PixelWriter pw = wr.getPixelWriter();
     * double opacity = 0.3; for (int x = 0; x < width; x++) { for (int y = 0; y
     * < height; y++) { Color color = Color.rgb(165, 42, 42, opacity);
     * pw.setColor(x, y, color); } } Image diffuseMap = wr; PhongMaterial
     * material = new PhongMaterial(); material.setDiffuseMap(diffuseMap);
     * meshView.setMaterial(material); }
     * 
     * public MeshView getMeshView() { return meshView; }
     */

    public Box getFeatureBox() {
        return featureBox;
    }

    public Feature getFeature() {
        return feature;
    }

}
