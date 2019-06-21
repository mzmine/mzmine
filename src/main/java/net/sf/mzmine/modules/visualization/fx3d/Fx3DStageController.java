/*
 * Copyright 2006-2018 The MZmine 2 Development Team
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
package net.sf.mzmine.modules.visualization.fx3d;

import java.util.ArrayList;
import java.util.logging.Logger;

import org.fxyz3d.utils.CameraTransformer;

import com.google.common.collect.Range;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.RotateTransition;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.util.Duration;

public class Fx3DStageController {

    @FXML
    private Label label;
    @FXML
    private BorderPane root;
    @FXML
    private Group subSceneRootNode;
    private Fx3DAxes axes = new Fx3DAxes();
    @FXML
    private TableView<Fx3DDataset> tableView = new TableView<Fx3DDataset>();
    @FXML
    private TableColumn<Fx3DDataset, String> fileNameCol = new TableColumn<Fx3DDataset, String>();
    @FXML
    private TableColumn<Fx3DDataset, String> colorCol = new TableColumn<Fx3DDataset, String>();

    private Group finalNode = new Group();
    private Group plot = new Group();
    private static final int SIZE = 500;
    private final Rotate rotateX = new Rotate(30, Rotate.X_AXIS);
    private final Rotate rotateY = new Rotate(0, Rotate.Y_AXIS);
    private final Translate translateX = new Translate();
    private final Translate translateY = new Translate();
    private Logger LOG = Logger.getLogger(this.getClass().getName());

    private double mousePosX, mousePosY;
    private double mouseOldX, mouseOldY;

    final double MAX_SCALE = 10.0;
    final double MIN_SCALE = 0.7;
    final double DEFAULT_SCALE = 1.0;

    public double maxOfAllBinnedIntensity = Double.NEGATIVE_INFINITY;

    public ObservableList<Fx3DDataset> datasets = FXCollections
            .observableArrayList();
    public ArrayList<Color> colors = new ArrayList<Color>();

    public CameraTransformer cameraTransform = new CameraTransformer();

    public RotateTransition rt = new RotateTransition(Duration.millis(3000),
            plot);

    public PerspectiveCamera camera = new PerspectiveCamera();

    public Timeline timeline;
    int animationFlag = 0;

    public Translate pivot = new Translate(250, 0, 250);
    public Rotate yRotate = new Rotate(0, Rotate.Y_AXIS);
    public Rotate yRotateDelta = new Rotate();
    double deltaAngle;
    int axisFlag = 0;

    public void initialize() {
        rotateX.setPivotZ(SIZE / 2);
        rotateX.setPivotX(SIZE / 2);
        rotateY.setPivotZ(SIZE / 2);
        rotateY.setPivotX(SIZE / 2);
        plot.getTransforms().addAll(rotateX, rotateY);
        translateY.setY(350);
        translateX.setX(170);
        finalNode.getTransforms().addAll(translateX, translateY);
        finalNode.getChildren().add(plot);

        plot.getChildren().add(axes);

        colors.add(Color.BLUE);
        colors.add(Color.GREEN);
        colors.add(Color.RED);
        colors.add(Color.YELLOW);
        colors.add(Color.DARKORANGE);
        colors.add(Color.CYAN);
        colors.add(Color.FUCHSIA);
        colors.add(Color.GOLD);

        fileNameCol.setCellValueFactory(
                new PropertyValueFactory<Fx3DDataset, String>("fileName"));
        colorCol.setCellValueFactory(
                new PropertyValueFactory<Fx3DDataset, String>("color"));

        PointLight light1 = new PointLight(Color.WHITE);
        light1.setTranslateX(SIZE / 2);
        light1.setTranslateZ(SIZE / 2);
        light1.setTranslateY(-1000);

        PointLight light2 = new PointLight(Color.WHITE);
        light2.setTranslateX(SIZE / 2);
        light2.setTranslateZ(SIZE / 2);
        light2.setTranslateY(1000);

        plot.getChildren().addAll(light1, light2);

        timeline = new Timeline(
                new KeyFrame(Duration.seconds(0),
                        new KeyValue(yRotate.angleProperty(), 360)),
                new KeyFrame(Duration.seconds(50),
                        new KeyValue(yRotate.angleProperty(), 0)));
        timeline.setCycleCount(Timeline.INDEFINITE);

        SubScene scene3D = new SubScene(finalNode, 800, 600, true,
                SceneAntialiasing.BALANCED);
        scene3D.widthProperty().bind(root.widthProperty());
        scene3D.heightProperty().bind(root.heightProperty());
        scene3D.setCamera(camera);
        scene3D.setPickOnBounds(true);
        subSceneRootNode.getChildren().add(scene3D);
    }

    public synchronized void setDataset(Fx3DDataset dataset,
            double maxBinnedIntensity, int index, int length) {

        dataset.setColor(getColorName(index));

        datasets.add(dataset);
        if (maxOfAllBinnedIntensity < maxBinnedIntensity) {
            maxOfAllBinnedIntensity = maxBinnedIntensity;
        }
        if (index == length - 1) {
            int i = 0;
            for (Fx3DDataset data : datasets) {
                Fx3DPlotMesh meshView = new Fx3DPlotMesh();
                meshView.setDataset(data, maxOfAllBinnedIntensity,
                        colors.get(i));
                plot.getChildren().add(meshView);
                i = (i + 1) % 8;
            }
            Range<Double> rtRange = dataset.getRtRange();
            Range<Double> mzRange = dataset.getMzRange();
            axes.setValues(rtRange, mzRange, maxOfAllBinnedIntensity);
            tableView.setItems(datasets);
        }
    }

    public void setLabel(String labelText) {
        this.label.setText(labelText);
        label.minWidth(root.getWidth());
        label.setAlignment(Pos.CENTER);
        label.setBackground(new Background(new BackgroundFill(Color.ALICEBLUE,
                CornerRadii.EMPTY, Insets.EMPTY)));
    }

    public String getColorName(int index) {
        String name = "UNKNOWN";

        switch (index) {
        case 0:
            name = "BLUE";
            break;
        case 1:
            name = "GREEN";
            break;
        case 2:
            name = "RED";
            break;
        case 3:
            name = "YELLOW";
            break;
        case 4:
            name = "DARKORANGE";
            break;
        case 5:
            name = "CYAN";
            break;
        case 6:
            name = "FUCHSIA";
            break;
        case 7:
            name = "GOLD";
            break;
        }
        return name;
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

    public void handleAnimate() {

        if (animationFlag == 0) {
            yRotate.setAngle(rotateY.getAngle() + deltaAngle);
            plot.getTransforms().addAll(pivot, yRotate,
                    new Translate(-250, 0, -250));
            timeline.play();
            animationFlag = 1;
        } else {
            plot.getTransforms().remove(yRotate);
            rotateY.setAngle(rotateY.getAngle() + yRotate.getAngle());
            deltaAngle = yRotate.getAngle();
            timeline.stop();
            animationFlag = 0;
        }
    }

    public void handleZoomOut(Event event) {
        if (animationFlag != 0) {
            rotateY.setAngle(rotateY.getAngle() + yRotate.getAngle());
        }
        timeline.stop();
        deltaAngle = 0;
        animationFlag = 0;
        plot.getTransforms().clear();
        plot.getTransforms().addAll(rotateX, rotateY);

        Timeline timeline1 = new Timeline(
                new KeyFrame(Duration.seconds(0),
                        new KeyValue(translateX.xProperty(),
                                translateX.getX())),
                new KeyFrame(Duration.seconds(1.5),
                        new KeyValue(translateX.xProperty(), 170)));
        timeline1.play();

        Timeline timeline2 = new Timeline(
                new KeyFrame(Duration.seconds(0),
                        new KeyValue(translateY.yProperty(),
                                translateY.getY())),
                new KeyFrame(Duration.seconds(1.5),
                        new KeyValue(translateY.yProperty(), 350)));
        timeline2.play();

        Timeline timeline3 = new Timeline(
                new KeyFrame(Duration.seconds(0),
                        new KeyValue(rotateX.angleProperty(),
                                rotateX.getAngle())),
                new KeyFrame(Duration.seconds(1.5),
                        new KeyValue(rotateX.angleProperty(), 30)));
        timeline3.play();

        double angle = rotateY.getAngle();
        if (angle > 180 && angle < 360) {
            angle = -(360 - (rotateY.getAngle() % 360));
        } else {
            angle = rotateY.getAngle() % 360;
        }
        LOG.finest("Rotate Angle:" + rotateY.getAngle());
        Timeline timeline4 = new Timeline(
                new KeyFrame(Duration.seconds(0),
                        new KeyValue(rotateY.angleProperty(), angle)),
                new KeyFrame(Duration.seconds(1.5),
                        new KeyValue(rotateY.angleProperty(), 0)));
        timeline4.play();

        Timeline timeline5 = new Timeline(
                new KeyFrame(Duration.seconds(0),
                        new KeyValue(plot.scaleXProperty(), plot.getScaleX())),
                new KeyFrame(Duration.seconds(1.5),
                        new KeyValue(plot.scaleXProperty(), DEFAULT_SCALE)));
        timeline5.play();

        Timeline timeline6 = new Timeline(
                new KeyFrame(Duration.seconds(0),
                        new KeyValue(plot.scaleYProperty(), plot.getScaleY())),
                new KeyFrame(Duration.seconds(1.5),
                        new KeyValue(plot.scaleYProperty(), DEFAULT_SCALE)));
        timeline6.play();
    }

    public void handleAxis(Event event) {
        if (axisFlag == 0) {
            plot.getChildren().remove(axes);
            axisFlag = 1;
        } else if (axisFlag == 1) {
            plot.getChildren().add(axes);
            axisFlag = 0;
        }
    }

    public void onScrollHandler(ScrollEvent event) {
        double delta = 1.2;
        double scale = (plot.getScaleX());

        if (event.getDeltaY() < 0) {
            scale /= delta;
        } else {
            scale *= delta;
        }

        scale = clamp(scale, MIN_SCALE, MAX_SCALE);

        plot.setScaleX(scale);
        plot.setScaleY(scale);

        event.consume();
    }

    public static double clamp(double value, double min, double max) {

        if (Double.compare(value, min) < 0)
            return min;

        if (Double.compare(value, max) > 0)
            return max;

        return value;
    }

    public Fx3DAxes getAxes() {
        return axes;
    }
}
