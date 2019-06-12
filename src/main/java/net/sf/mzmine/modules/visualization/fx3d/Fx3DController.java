package net.sf.mzmine.modules.visualization.fx3d;

import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.layout.StackPane;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;

public class Fx3DController {

    @FXML
    private StackPane root = new StackPane();
    @FXML
    private Fx3DPlotMesh meshView = new Fx3DPlotMesh();
    @FXML
    private Group plot = new Group();
    @FXML
    private Group finalNode = new Group();
    @FXML
    private Fx3DAxes axes = new Fx3DAxes();

    private final Rotate rotateX = new Rotate(45, Rotate.X_AXIS);
    private final Rotate rotateY = new Rotate(0, Rotate.Y_AXIS);
    private final Translate translateX = new Translate();
    private final Translate translateY = new Translate();

    public void initialize() {
        plot.getTransforms().addAll(rotateX, rotateY);
        finalNode.getTransforms().addAll(translateX, translateY);
    }

    public void setDataset(Fx3DDataset dataset) {
        meshView.setDataset(dataset);
        axes.setDataset(dataset);
    }

    public StackPane getRoot() {
        return root;
    }
}
