package net.sf.mzmine.modules.visualization.fx3d;

import java.util.logging.Logger;

import javafx.scene.DepthTest;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

public class Fx3DPlotMesh extends MeshView {

    private static final int SIZE = 500;
    private static float AMPLIFI = 130;
    private int rtResolution;
    private int mzResolution;
    private static final Logger LOG = Logger
            .getLogger(Fx3DPlotMesh.class.getName());

    public Fx3DPlotMesh(Fx3DDataset dataset) {
        rtResolution = dataset.getRtResolution();
        mzResolution = dataset.getMzResolution();

        TriangleMesh mesh = new TriangleMesh();

        int[][] peakListIndices = new int[rtResolution][mzResolution];
        float factorX = (float) SIZE / rtResolution;
        float factorZ = (float) SIZE / mzResolution;

        float[][] intensityValues = dataset.getIntensityValues();

        float maxIntensityValue = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < rtResolution; i++) {
            for (int j = 0; j < mzResolution; j++) {
                if (maxIntensityValue < intensityValues[i][j]) {
                    maxIntensityValue = intensityValues[i][j];
                }
            }
        }

        for (int x = 0; x < rtResolution; x++) {
            for (int z = 0; z < mzResolution; z++) {
                mesh.getPoints().addAll((float) x * factorX,
                        -intensityValues[x][z] * AMPLIFI, (float) z * factorZ);
                if (intensityValues[x][z] > 0.022 * maxIntensityValue) {
                    peakListIndices[x][z] = 1;
                }
            }
        }

        int rtLength = rtResolution;
        int mzLength = mzResolution;
        float rtTotal = rtLength;
        float mzTotal = mzResolution;

        for (float x = 0; x < rtLength - 1; x++) {
            for (float y = 0; y < mzLength - 1; y++) {

                float x0 = x / rtTotal;
                float y0 = y / mzTotal;
                float x1 = (x + 1) / rtTotal;
                float y1 = (y + 1) / mzTotal;

                mesh.getTexCoords().addAll( //
                        x0, y0, // 0, top-left
                        x0, y1, // 1, bottom-left
                        x1, y0, // 2, top-right
                        x1, y1 // 3, bottom-right
                );
            }
        }

        // faces
        for (int x = 0; x < rtLength - 1; x++) {
            for (int z = 0; z < mzLength - 1; z++) {

                int tl = x * mzLength + z; // top-left
                int bl = x * mzLength + z + 1; // bottom-left
                int tr = (x + 1) * mzLength + z; // top-right
                int br = (x + 1) * mzLength + z + 1; // bottom-right

                int offset = (x * (mzLength - 1) + z) * 8 / 2; // div 2 because
                                                               // we have u AND
                                                               // v in the list

                // working
                mesh.getFaces().addAll(bl, offset + 1, tl, offset + 0, tr,
                        offset + 2);
                mesh.getFaces().addAll(tr, offset + 2, br, offset + 3, bl,
                        offset + 1);

            }
        }
        int width = rtLength;
        int height = mzLength;

        WritableImage wr = new WritableImage(width, height);
        PixelWriter pw = wr.getPixelWriter();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {

                if (peakListIndices[x][y] == 1) {
                    Color color = Color.BLUE;
                    pw.setColor(x, y, color);
                    if (x - 1 >= 0 && y - 1 >= 0) {
                        pw.setColor(x - 1, y - 1, color);
                        pw.setColor(x, y - 1, color);
                        pw.setColor(x - 1, y, color);
                    }
                } else {
                    Color color = Color.DARKGREY;
                    pw.setColor(x, y, color);
                }
            }
        }
        Image diffuseMap = wr;
        PhongMaterial material = new PhongMaterial();
        material.setDiffuseMap(diffuseMap);

        LOG.info("Plot mesh is ready.");
        setMesh(mesh);
        setMaterial(material);
        setCullFace(CullFace.NONE);
        setDrawMode(DrawMode.FILL);
        setDepthTest(DepthTest.ENABLE);

    }
}
