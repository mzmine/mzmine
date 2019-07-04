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

import java.util.logging.Logger;

import org.controlsfx.glyphfont.Glyph;

import com.google.common.collect.Range;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.MeshView;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.util.Duration;
import net.sf.mzmine.util.components.ButtonCell;
import net.sf.mzmine.util.components.ColorTableCell;
import net.sf.mzmine.util.components.SliderCell;

public class Fx3DStageController {

    @FXML
    private HBox hBox;
    @FXML
    Region leftRegion;
    @FXML
    private Label label;
    @FXML
    Region rightRegion;
    @FXML
    private BorderPane root;
    @FXML
    private Group subSceneRootNode;
    private Fx3DAxes axes = new Fx3DAxes();
    @FXML
    private ToggleButton animateBtn;
    @FXML
    private ToggleButton axesBtn;
    @FXML
    private ToggleButton lightsBtn;
    @FXML
    private TableView<Fx3DPlotMesh> tableView;
    @FXML
    private TableColumn<Fx3DPlotMesh, String> fileNameCol;
    @FXML
    private TableColumn<Fx3DPlotMesh, Color> colorCol;
    @FXML
    private TableColumn<Fx3DPlotMesh, Double> opacityCol;
    @FXML
    private TableColumn<Fx3DPlotMesh, Boolean> visibilityCol;

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

    private double maxOfAllBinnedIntensity = Double.NEGATIVE_INFINITY;

    private ObservableList<Fx3DPlotMesh> meshPlots = FXCollections
            .observableArrayList();
    private ObservableList<MeshView> meshList = FXCollections
            .observableArrayList();
    private PerspectiveCamera camera = new PerspectiveCamera();

    private Timeline rotateAnimationTimeline;
    boolean animationRunning = false;
    private Range<Double> rtRange;
    private Range<Double> mzRange;
    public Translate pivot = new Translate(250, 0, 250);
    public Rotate yRotate = new Rotate(0, Rotate.Y_AXIS);
    public Rotate yRotateDelta = new Rotate();
    double deltaAngle;
    private int totalFiles;
    private int fileCount = 0;
    private PointLight top;
    private PointLight bottom;
    private PointLight left;
    private PointLight right;

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

        colorCol.setCellFactory(
                column -> new ColorTableCell<Fx3DPlotMesh>(column));
        double minValue = 0;
        double maxValue = 1;
        opacityCol.setCellFactory(column -> new SliderCell<Fx3DPlotMesh>(column,
                minValue, maxValue));

        visibilityCol
                .setCellFactory(column -> new ButtonCell<Fx3DPlotMesh>(column,
                        new Glyph("FontAwesome", "EYE"),
                        new Glyph("FontAwesome", "EYE_SLASH")));
        axesBtn.setSelected(true);
        lightsBtn.setSelected(true);
        addLights();
        rotateAnimationTimeline = new Timeline(
                new KeyFrame(Duration.seconds(0),
                        new KeyValue(yRotate.angleProperty(), 360)),
                new KeyFrame(Duration.seconds(50),
                        new KeyValue(yRotate.angleProperty(), 0)));
        rotateAnimationTimeline.setCycleCount(Timeline.INDEFINITE);

        tableView.setItems(meshPlots);
        SubScene scene3D = new SubScene(finalNode, 800, 600, true,
                SceneAntialiasing.BALANCED);
        scene3D.widthProperty().bind(root.widthProperty());
        scene3D.heightProperty().bind(root.heightProperty());
        scene3D.setCamera(camera);
        scene3D.setPickOnBounds(true);
        subSceneRootNode.getChildren().add(scene3D);
    }

    private void addLights() {
        top = new PointLight(Color.WHITE);
        top.setTranslateX(SIZE / 2);
        top.setTranslateZ(SIZE / 2);
        top.setTranslateY(-1000);

        left = new PointLight(Color.WHITE);
        left.setTranslateX(-1000);
        left.setTranslateZ(SIZE / 2);
        left.setTranslateY(10);

        right = new PointLight(Color.WHITE);
        right.setTranslateX(1500);
        right.setTranslateZ(SIZE / 2);
        right.setTranslateY(-10);

        bottom = new PointLight(Color.WHITE);
        bottom.setTranslateX(SIZE / 2);
        bottom.setTranslateZ(SIZE / 2);
        bottom.setTranslateY(1000);

        plot.getChildren().add(left);
        plot.getChildren().add(right);
        plot.getChildren().add(top);
        plot.getChildren().add(bottom);
    }

    public synchronized void addPlotMesh(Fx3DPlotMesh plotMesh) {
        fileCount++;
        meshPlots.add(plotMesh);
        if (maxOfAllBinnedIntensity < plotMesh.getMaxBinnedIntensity()) {
            maxOfAllBinnedIntensity = plotMesh.getMaxBinnedIntensity();
        }
        if (fileCount == totalFiles) {
            addColorListener();
            addOpacityListener();
            for (Fx3DPlotMesh mesh : meshPlots) {
                mesh.normalize(maxOfAllBinnedIntensity);
                meshList.add(mesh.getMeshView());
            }
            axes.setValues(rtRange, mzRange, maxOfAllBinnedIntensity);
            plot.getChildren().addAll(meshList);
            LOG.finest("Number of plot meshes:" + meshList.size());
            LOG.finest("Number of datasets sampled:" + meshPlots.size());
        }
        LOG.finest("Dataset no. " + plotMesh.getIndex()
                + " has been added to the datasets list.");
    }

    private void addColorListener() {
        for (Fx3DPlotMesh mesh : meshPlots) {
            colorCol.getCellObservableValue(mesh)
                    .addListener((e, oldValue, newValue) -> {
                        int red = (int) (newValue.getRed() * 255);
                        int green = (int) (newValue.getGreen() * 255);
                        int blue = (int) (newValue.getBlue() * 255);
                        mesh.setColor(Color.rgb(red, green, blue,
                                (double) mesh.opacityProperty().get()));
                        mesh.setOpacity((double) mesh.opacityProperty().get());
                    });
        }
    }

    private void addOpacityListener() {
        for (Fx3DPlotMesh mesh : meshPlots) {
            opacityCol.getCellObservableValue(mesh)
                    .addListener((e, oldValue, newValue) -> {
                        Color color = mesh.getColor();
                        int red = (int) (color.getRed() * 255);
                        int green = (int) (color.getGreen() * 255);
                        int blue = (int) (color.getBlue() * 255);
                        mesh.setOpacity((double) newValue);
                        mesh.setColor(
                                Color.rgb(red, green, blue, (double) newValue));
                    });
        }
    }

    public void setRtMzValues(Range<Double> rt, Range<Double> mz) {
        this.rtRange = rt;
        this.mzRange = mz;
    }

    public void setLabel(String labelText) {
        HBox.setHgrow(leftRegion, Priority.ALWAYS);
        HBox.setHgrow(rightRegion, Priority.ALWAYS);
        label.setText(labelText);
        label.setTextFill(Color.WHITE);
    }

    public void handleMousePressed(MouseEvent me) {
        mouseOldX = me.getSceneX();
        mouseOldY = me.getSceneY();
    }

    public void handleMouseDragged(MouseEvent me) {
        double rotateFactor = 0.12;
        mousePosX = me.getSceneX();
        mousePosY = me.getSceneY();
        if (me.isPrimaryButtonDown()) {
            rotateX.setAngle(rotateX.getAngle()
                    + rotateFactor * (mousePosY - mouseOldY));
            rotateY.setAngle(rotateY.getAngle()
                    - rotateFactor * (mousePosX - mouseOldX));
            axes.getRotateY().setAngle(axes.getRotateY().getAngle()
                    + rotateFactor * (mousePosX - mouseOldX));
        }
        if (me.isSecondaryButtonDown()) {
            translateX.setX(translateX.getX() + (mousePosX - mouseOldX));
            translateY.setY(translateY.getY() + (mousePosY - mouseOldY));
        }
        mouseOldX = mousePosX;
        mouseOldY = mousePosY;
    }

    public void handleAnimate() {
        if (!animationRunning) {
            yRotate.setAngle(rotateY.getAngle() + deltaAngle);
            plot.getTransforms().addAll(pivot, yRotate,
                    new Translate(-250, 0, -250));
            rotateAnimationTimeline.play();
            animationRunning = true;
        } else {
            plot.getTransforms().remove(yRotate);
            rotateY.setAngle(rotateY.getAngle() + yRotate.getAngle());
            deltaAngle = yRotate.getAngle();
            rotateAnimationTimeline.stop();
            animationRunning = false;
        }
    }

    public void handleZoomOut(Event event) {
        if (animationRunning) {
            rotateY.setAngle(rotateY.getAngle() + yRotate.getAngle());
        }
        animateBtn.setSelected(false);
        rotateAnimationTimeline.stop();
        deltaAngle = 0;
        animationRunning = false;
        plot.getTransforms().clear();
        plot.getTransforms().addAll(rotateX, rotateY);

        Timeline resetTranslateXTimeline = new Timeline(
                new KeyFrame(Duration.seconds(0),
                        new KeyValue(translateX.xProperty(),
                                translateX.getX())),
                new KeyFrame(Duration.seconds(1.5), new KeyValue(
                        translateX.xProperty(),
                        (root.getWidth() * 3 / 4) - root.getHeight() * 3 / 4)));
        resetTranslateXTimeline.play();

        Timeline resetTranslateYTimeline = new Timeline(
                new KeyFrame(Duration.seconds(0),
                        new KeyValue(translateY.yProperty(),
                                translateY.getY())),
                new KeyFrame(Duration.seconds(1.5), new KeyValue(
                        translateY.yProperty(), root.getHeight() / 2)));
        resetTranslateYTimeline.play();

        Timeline resetRotateXTimeline = new Timeline(
                new KeyFrame(Duration.seconds(0),
                        new KeyValue(rotateX.angleProperty(),
                                rotateX.getAngle())),
                new KeyFrame(Duration.seconds(1.5),
                        new KeyValue(rotateX.angleProperty(), 30)));
        resetRotateXTimeline.play();

        double angle = rotateY.getAngle();
        if ((angle > 180 && angle < 360)) {
            angle = -(360 - (rotateY.getAngle() % 360));
        } else if ((angle > -360 && angle < -180)) {
            angle = Math.abs(angle);
        } else {
            angle = rotateY.getAngle() % 360;
        }
        LOG.finest("Zoom out button clicked. Current rotation angles X="
                + rotateX.getAngle() + " Y=" + rotateY.getAngle()
                + "Starting 1.5s animation to angle X=0 Y=0");
        Timeline resetRotateYTimeline = new Timeline(
                new KeyFrame(Duration.seconds(0),
                        new KeyValue(rotateY.angleProperty(), angle)),
                new KeyFrame(Duration.seconds(1.5),
                        new KeyValue(rotateY.angleProperty(), 0)));
        resetRotateYTimeline.play();

        Timeline resetScaleXTimeline = new Timeline(
                new KeyFrame(Duration.seconds(0),
                        new KeyValue(plot.scaleXProperty(), plot.getScaleX())),
                new KeyFrame(Duration.seconds(1.5),
                        new KeyValue(plot.scaleXProperty(), DEFAULT_SCALE)));
        resetScaleXTimeline.play();

        Timeline resetScaleYTimeline = new Timeline(
                new KeyFrame(Duration.seconds(0),
                        new KeyValue(plot.scaleYProperty(), plot.getScaleY())),
                new KeyFrame(Duration.seconds(1.5),
                        new KeyValue(plot.scaleYProperty(), DEFAULT_SCALE)));
        resetScaleYTimeline.play();
    }

    public void handleAxis(Event event) {
        if (axes.isVisible()) {
            axes.setVisible(false);
        } else {
            axes.setVisible(true);
        }
    }

    public void handleLights() {
        if (lightsBtn.isSelected()) {
            top.setLightOn(true);
            bottom.setLightOn(true);
            left.setLightOn(true);
            right.setLightOn(true);
        } else {
            top.setLightOn(false);
            bottom.setLightOn(false);
            left.setLightOn(false);
            right.setLightOn(false);
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

    private static double clamp(double value, double min, double max) {

        if (Double.compare(value, min) < 0)
            return min;

        if (Double.compare(value, max) > 0)
            return max;

        return value;
    }

    public void setTotalFiles(int noOfFiles) {
        totalFiles = noOfFiles;
    }

    public Fx3DAxes getAxes() {
        return axes;
    }
}
