package net.sf.mzmine.modules.visualization.fx3d;

import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;

public class Fx3DStageController {

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

    private double mousePosX, mousePosY;
    private double mouseOldX, mouseOldY;

    public void initialize() {
        plot.getTransforms().addAll(rotateX, rotateY);
        finalNode.getTransforms().addAll(translateX, translateY);
    }

    public void setDataset(Fx3DDataset dataset) {
        meshView.setDataset(dataset);
    }

    public void handleMousePressed(MouseEvent me) {
        mouseOldX = me.getSceneX();
        mouseOldY = me.getSceneY();
    }

    public void handleMouseDragged(MouseEvent me) {
        double rotateFactor = 0.08;
        mousePosX = me.getSceneX();
        mousePosY = me.getSceneY();
        if (me.isPrimaryButtonDown()) {
            rotateX.setAngle(rotateX.getAngle()
                    + rotateFactor * (mousePosY - mouseOldY));
            rotateY.setAngle(rotateY.getAngle()
                    - rotateFactor * (mousePosX - mouseOldX));
        }
        if (me.isSecondaryButtonDown()) {
            translateX.setX(translateX.getX() + (mousePosX - mouseOldX));
            translateY.setY(translateY.getY() + (mousePosY - mouseOldY));
        }
        mouseOldX = mousePosX;
        mouseOldY = mousePosY;
    }

    public Fx3DAxes getAxes() {
        return axes;
    }
}
