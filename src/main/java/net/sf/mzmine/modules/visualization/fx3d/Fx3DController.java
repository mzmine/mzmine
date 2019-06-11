package net.sf.mzmine.modules.visualization.fx3d;

import com.google.common.collect.Range;

import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.PointLight;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;

public class Fx3DController extends StackPane {

    private Fx3DDataset dataset;

    private Group plot = new Group();
    private Group finalNode = new Group();

    private final Rotate rotateX = new Rotate(45, Rotate.X_AXIS);
    private final Rotate rotateY = new Rotate(0, Rotate.Y_AXIS);
    private final Translate translateX = new Translate();
    private final Translate translateY = new Translate();

    public void initialize() {

        plot.getTransforms().addAll(rotateX, rotateY);
        finalNode.getTransforms().addAll(translateX, translateY);
        finalNode.getChildren().add(plot);
        getChildren().add(finalNode);

        Fx3DPlotMesh meshView = new Fx3DPlotMesh(dataset);

        AmbientLight ambient = new AmbientLight();
        ambient.setColor(Color.WHITE);
        PointLight light = new PointLight();
        light.setColor(Color.WHITE);
        light.setLayoutX(250 - light.getLayoutBounds().getMinX());
        light.setLayoutY(250 - light.getLayoutBounds().getMinY());
        light.getScope().add(meshView);

        Range<Double> rtRange = dataset.getRtRange();
        Range<Double> mzRange = dataset.getMzRange();
        double maxBinnedIntensity = dataset.getMaxBinnedIntensity();
        plot.getChildren().addAll(meshView, light, ambient);
        plot.getChildren()
                .addAll(new Fx3DAxes(rtRange, mzRange, maxBinnedIntensity));

    }

    public void setDataset(Fx3DDataset dataset) {
        this.dataset = dataset;
    }

    public StackPane getRoot() {
        return this;
    }
}
