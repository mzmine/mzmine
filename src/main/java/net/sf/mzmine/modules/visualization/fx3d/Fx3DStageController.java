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
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

import org.controlsfx.glyphfont.Glyph;

import com.google.common.collect.Range;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.Scene;
import javafx.scene.SubScene;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
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
import javafx.scene.shape.Line;
import javafx.scene.shape.MeshView;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;
import javafx.util.Duration;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.parametertypes.selectors.ScanSelection;
import net.sf.mzmine.taskcontrol.TaskPriority;
import net.sf.mzmine.util.components.ButtonCell;
import net.sf.mzmine.util.components.ColorTableCell;
import net.sf.mzmine.util.components.SliderCell;

public class Fx3DStageController {

    @FXML
    private Stage stage;
    @FXML
    private Scene scene;
    @FXML
    private HBox hBox;
    @FXML
    Region leftRegion;
    @FXML
    private Label label;
    @FXML
    Region rightRegion;
    @FXML
    private SubScene scene3D;
    @FXML
    private Menu addMenu;
    @FXML
    private Menu removeMenu;
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
    private TableView<Fx3DRawDataFileDataset> tableView;
    @FXML
    private TableColumn<Fx3DRawDataFileDataset, String> fileNameCol;
    @FXML
    private TableColumn<Fx3DRawDataFileDataset, Color> colorCol;
    @FXML
    private TableColumn<Fx3DRawDataFileDataset, Double> opacityCol;
    @FXML
    private TableColumn<Fx3DRawDataFileDataset, Boolean> visibilityCol;
    @FXML
    private Group finalNode;
    private Group plot = new Group();
    private Group meshViews = new Group();
    private static final int SIZE = 500;
    private final Rotate rotateX = new Rotate(30, Rotate.X_AXIS);
    private final Rotate rotateY = new Rotate(0, Rotate.Y_AXIS);
    private final Translate translateX = new Translate();
    private final Translate translateY = new Translate();
    private Logger LOG = Logger.getLogger(this.getClass().getName());
    private int rtResolution, mzResolution;
    private double mousePosX, mousePosY;
    private double mouseOldX, mouseOldY;

    final double MAX_SCALE = 10.0;
    final double MIN_SCALE = 0.7;
    final double DEFAULT_SCALE = 1.0;

    private double maxOfAllBinnedIntensity = Double.NEGATIVE_INFINITY;

    private ObservableList<Fx3DRawDataFileDataset> visualizedMeshPlots = FXCollections
            .observableArrayList();
    private ObservableList<Fx3DRawDataFileDataset> remainingMeshPlots = FXCollections
            .observableArrayList();
    private ObservableList<MeshView> meshViewList = FXCollections
            .observableArrayList();
    private PerspectiveCamera camera = new PerspectiveCamera();
    private ScanSelection scanSel;
    private List<RawDataFile> allDataFiles;
    private List<RawDataFile> visualizedFiles = new ArrayList<RawDataFile>();
    private Timeline rotateAnimationTimeline;
    boolean animationRunning = false;
    private Range<Double> rtRange;
    private Range<Double> mzRange;
    public Translate pivot = new Translate(250, 0, 250);
    public Rotate yRotate = new Rotate(0, Rotate.Y_AXIS);
    public Rotate yRotateDelta = new Rotate();
    double deltaAngle;
    private PointLight top;
    private PointLight bottom;
    private PointLight left;
    private PointLight right;
    private int axesPosition = 0;

    public void initialize() {
        rotateX.setPivotZ(SIZE / 2);
        rotateX.setPivotX(SIZE / 2);
        rotateY.setPivotZ(SIZE / 2);
        rotateY.setPivotX(SIZE / 2);
        plot.getTransforms().addAll(rotateX, rotateY);
        translateY.setY(250);
        translateX.setX(170);
        finalNode.getTransforms().addAll(translateX, translateY);
        finalNode.getChildren().add(plot);
        HBox.setHgrow(leftRegion, Priority.ALWAYS);
        HBox.setHgrow(rightRegion, Priority.ALWAYS);
        plot.getChildren().add(axes);
        colorCol.setCellFactory(
                column -> new ColorTableCell<Fx3DRawDataFileDataset>(column));
        double minValue = 0;
        double maxValue = 1;
        opacityCol.setCellFactory(
                column -> new SliderCell<Fx3DRawDataFileDataset>(column,
                        minValue, maxValue));

        visibilityCol.setCellFactory(
                column -> new ButtonCell<Fx3DRawDataFileDataset>(column,
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

        tableView.setItems(visualizedMeshPlots);
        plot.getChildren().add(meshViews);
        allDataFiles = Arrays.asList(MZmineCore.getProjectManager()
                .getCurrentProject().getDataFiles());

        scene3D.widthProperty().bind(root.widthProperty());
        scene3D.heightProperty().bind(root.heightProperty());
        scene3D.setCamera(camera);
        scene3D.setPickOnBounds(true);
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

    public synchronized void addDataset(Fx3DAbstractDataset dataset) {
        visualizedMeshPlots.add((Fx3DRawDataFileDataset) dataset);
        visualizedFiles.add(dataset.getDataFile());
        maxOfAllBinnedIntensity = Double.NEGATIVE_INFINITY;
        for (Fx3DRawDataFileDataset mesh : visualizedMeshPlots) {
            if (maxOfAllBinnedIntensity < ((Fx3DRawDataFileDataset) mesh)
                    .getMaxBinnedIntensity()) {
                maxOfAllBinnedIntensity = ((Fx3DRawDataFileDataset) mesh)
                        .getMaxBinnedIntensity();
            }
        }
        axes.updateAxisParameters(rtRange, mzRange, maxOfAllBinnedIntensity);
        meshViewList.clear();
        for (Fx3DRawDataFileDataset mesh : visualizedMeshPlots) {
            mesh.normalize(maxOfAllBinnedIntensity);
            meshViewList.add(mesh.getMeshView());
        }
        meshViews.getChildren().clear();
        meshViews.getChildren().addAll(meshViewList);
        addMenuItems();
        addColorListener((Fx3DRawDataFileDataset) dataset);
        addOpacityListener((Fx3DRawDataFileDataset) dataset);
        dataset.visibilityProperty()
                .bindBidirectional(((Fx3DRawDataFileDataset) dataset)
                        .getMeshView().visibleProperty());
        updateLabel();
    }

    private void updateGraph() {
        maxOfAllBinnedIntensity = Double.NEGATIVE_INFINITY;
        for (Fx3DRawDataFileDataset mesh : visualizedMeshPlots) {
            if (maxOfAllBinnedIntensity < ((Fx3DRawDataFileDataset) mesh)
                    .getMaxBinnedIntensity()) {
                maxOfAllBinnedIntensity = ((Fx3DRawDataFileDataset) mesh)
                        .getMaxBinnedIntensity();
            }
        }
        axes.updateAxisParameters(rtRange, mzRange, maxOfAllBinnedIntensity);
        meshViewList.clear();
        for (Fx3DRawDataFileDataset mesh : visualizedMeshPlots) {
            mesh.normalize(maxOfAllBinnedIntensity);
            meshViewList.add(mesh.getMeshView());
        }
        meshViews.getChildren().clear();
        meshViews.getChildren().addAll(meshViewList);
        FXCollections.sort(meshViews.getChildren(), new SortByOpacity());
        updateLabel();
    }

    private void addColorListener(Fx3DRawDataFileDataset dataset) {
        dataset.colorProperty().addListener((e, oldValue, newValue) -> {
            int red = (int) (newValue.getRed() * 255);
            int green = (int) (newValue.getGreen() * 255);
            int blue = (int) (newValue.getBlue() * 255);
            dataset.setPeakColor(Color.rgb(red, green, blue,
                    (double) dataset.opacityProperty().get()));
            dataset.getMeshView()
                    .setOpacity((double) dataset.opacityProperty().get());
            LOG.finest("Color is changed from " + oldValue + " to " + newValue
                    + " for the dataset " + dataset.getFileName());
        });
    }

    private void addOpacityListener(Fx3DRawDataFileDataset dataset) {
        dataset.opacityProperty().addListener((e, oldValue, newValue) -> {
            Color color = dataset.getColor();
            int red = (int) (color.getRed() * 255);
            int green = (int) (color.getGreen() * 255);
            int blue = (int) (color.getBlue() * 255);
            dataset.setOpacity((double) newValue);
            dataset.setColor(Color.rgb(red, green, blue, (double) newValue));
            FXCollections.sort(meshViews.getChildren(), new SortByOpacity());
        });

    }

    class SortByOpacity implements Comparator<Node> {
        @Override
        public int compare(Node a, Node b) {
            final Double aOpacity = a.getOpacity();
            final Double bOpacity = b.getOpacity();
            return bOpacity.compareTo(aOpacity);
        }
    }

    private void addMenuItems() {
        removeMenu.getItems().clear();
        for (Fx3DRawDataFileDataset dataset : visualizedMeshPlots) {
            MenuItem menuItem = new MenuItem(dataset.getFileName());
            removeMenu.getItems().add(menuItem);
            menuItem.setOnAction(new EventHandler<ActionEvent>() {
                public void handle(ActionEvent e) {
                    LOG.finest(
                            "Context menu invoked. Remove button clicked. Removing dataset "
                                    + dataset.getFileName()
                                    + " from the plot.");
                    remainingMeshPlots.add(dataset);
                    visualizedFiles.remove(dataset.getDataFile());
                    visualizedMeshPlots.remove(dataset);
                    updateGraph();
                    addMenuItems();
                }
            });
        }
        addMenu.getItems().clear();
        for (RawDataFile file : allDataFiles) {
            if (!visualizedFiles.contains(file)) {
                MenuItem menuItem = new MenuItem(file.getName());
                addMenu.getItems().add(menuItem);
                final Fx3DStageController controller = this;
                menuItem.setOnAction(new EventHandler<ActionEvent>() {

                    public void handle(ActionEvent e) {
                        LOG.finest(
                                "Context menu invoked. Add button clicked. Adding dataset "
                                        + file.getName() + " to the plot.");
                        MZmineCore.getTaskController().addTask(
                                new Fx3DSamplingTask(file, scanSel, mzRange,
                                        rtResolution, mzResolution, controller),
                                TaskPriority.HIGH);
                        addMenuItems();
                    }

                });
            }
        }
    }

    private void updateLabel() {
        String title = "";
        for (Fx3DRawDataFileDataset dataset : visualizedMeshPlots) {
            title = title + dataset.getFileName() + " ";
        }
        stage.setTitle(title);
        title = "3D plot of files [" + title + "], "
                + mzRange.lowerEndpoint().toString() + "-"
                + mzRange.upperEndpoint().toString() + " m/z, RT "
                + (float) (Math.round(rtRange.lowerEndpoint() * 100.0) / 100.0)
                + "-"
                + (float) (Math.round(rtRange.upperEndpoint() * 100.0) / 100.0)
                + " min";

        label.setText(title);
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
        }
        if (me.isSecondaryButtonDown()) {
            translateX.setX(translateX.getX() + (mousePosX - mouseOldX));
            translateY.setY(translateY.getY() + (mousePosY - mouseOldY));

        }
        mouseOldX = mousePosX;
        mouseOldY = mousePosY;
    }

    private void setMzAxis() {
        axes.getMzAxisLabels().getChildren().clear();
        axes.getMzAxisTicks().getChildren().clear();
        double mzDelta = (mzRange.upperEndpoint() - mzRange.lowerEndpoint())
                / 7;
        double mzScaleValue = mzRange.lowerEndpoint();
        Text mzLabel = new Text("m/z");
        mzLabel.setRotationAxis(Rotate.X_AXIS);
        mzLabel.setRotate(-45);
        mzLabel.setTranslateX(SIZE / 2);
        mzLabel.setTranslateZ(-5);
        mzLabel.setTranslateY(8);
        axes.getMzAxisLabels().getChildren().add(mzLabel);
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
            mzScaleValue += mzDelta;
            axes.getMzAxisTicks().getChildren().add(tickLineZ);
            axes.getMzAxisLabels().getChildren().add(text);
        }
        axes.getMzAxisLabels().setRotate(270);
        axes.getMzAxisLabels().setTranslateX(SIZE / 2 + SIZE / 30);
        axes.getMzAxisTicks().setTranslateX(SIZE / 2 + 10);
        axes.getMzAxisTicks().setTranslateY(-1);
        axes.getMzAxis().setTranslateX(SIZE);
    }

    private void setRtAxis() {
        axes.getRtAxis().getChildren().clear();
        double rtDelta = (rtRange.upperEndpoint() - rtRange.lowerEndpoint())
                / 7;
        double rtScaleValue = rtRange.upperEndpoint();
        Text rtLabel = new Text("Retention Time");
        rtLabel.setRotationAxis(Rotate.X_AXIS);
        rtLabel.setRotate(-45);
        rtLabel.setTranslateX(SIZE * 3 / 8);
        rtLabel.setTranslateZ(-25);
        rtLabel.setTranslateY(13);
        axes.getRtAxis().getChildren().add(rtLabel);
        for (int y = 0; y <= SIZE; y += SIZE / 7) {
            Line tickLineX = new Line(0, 0, 0, 9);
            tickLineX.setRotationAxis(Rotate.X_AXIS);
            tickLineX.setRotate(-90);
            tickLineX.setTranslateY(-5);
            tickLineX.setTranslateX(y);
            tickLineX.setTranslateZ(-3.5);
            float roundOff = (float) (Math.round(rtScaleValue * 10.0) / 10.0);
            Text text = new Text("" + (float) roundOff);
            text.setRotationAxis(Rotate.X_AXIS);
            text.setRotate(-45);
            text.setTranslateY(9);
            text.setTranslateX(y - 5);
            text.setTranslateZ(-15);
            rtScaleValue -= rtDelta;
            axes.getRtAxis().getChildren().addAll(text, tickLineX);
        }
        Line lineX = new Line(0, 0, SIZE, 0);
        axes.getRtAxis().getChildren().add(lineX);
        axes.getRtRotate().setAngle(180);
        axes.getRtTranslate().setZ(-SIZE);
        axes.getRtTranslate().setX(-SIZE);
    }

    private void setCameraAngle(int angle) {
        rotateX.setAngle(30);
        translateX.setX(root.getWidth() * 3 / 4 - root.getHeight() * 3 / 4);
        translateY.setY(root.getHeight() / 3);
        rotateY.setAngle(angle);
        plot.setScaleX(DEFAULT_SCALE);
        plot.setScaleY(DEFAULT_SCALE);
    }

    public void handleToggleAxes(Event event) {
        if (animationRunning) {
            animateBtn.setSelected(false);
            rotateAnimationTimeline.stop();
            deltaAngle = 0;
            animationRunning = false;
            plot.getTransforms().remove(yRotate);
        }
        if (axesPosition == 0) {
            axesPosition = 1;
            axes.getIntensityTranslate().setX(SIZE);
            setMzAxis();
            setCameraAngle(44);
        } else if (axesPosition == 1) {
            axesPosition = 2;
            setCameraAngle(90);
        } else if (axesPosition == 2) {
            axesPosition = 3;
            axes.getIntensityTranslate().setZ(-SIZE);
            axes.getIntensityRotate().setAngle(-91);
            setRtAxis();
            setCameraAngle(135);
        } else if (axesPosition == 3) {
            axesPosition = 4;
            setCameraAngle(180);
        } else if (axesPosition == 4) {
            axesPosition = 5;
            axes.updateAxisParameters(rtRange, mzRange,
                    maxOfAllBinnedIntensity);
            axes.getIntensityRotate().setAngle(181);
            axes.getIntensityTranslate().setZ(-SIZE);
            axes.getMzAxis().setTranslateX(0);
            setRtAxis();
            setCameraAngle(135 + 90);
        } else if (axesPosition == 5) {
            axesPosition = 6;
            setCameraAngle(270);
            axes.getIntensityRotate().setAngle(89);
            axes.getIntensityTranslate().setZ(0);
            axes.getIntensityTranslate().setX(-SIZE);
            setCameraAngle(270);
        } else if (axesPosition == 6) {
            axesPosition = 7;
            axes.updateAxisParameters(rtRange, mzRange,
                    maxOfAllBinnedIntensity);
            axes.getIntensityRotate().setAngle(89);
            setCameraAngle(135 + 180);
        } else if (axesPosition == 7) {
            axesPosition = 0;
            axesPosition = 6;
            axes.updateAxisParameters(rtRange, mzRange,
                    maxOfAllBinnedIntensity);
            setCameraAngle(0);
        }

    }

    public void handleAnimate() {
        if (!animationRunning) {
            yRotate.setAngle(rotateY.getAngle() + deltaAngle);
            plot.getTransforms().addAll(pivot, yRotate,
                    new Translate(-250, 0, -250));
            rotateAnimationTimeline.play();
            animationRunning = true;
            LOG.finest("ANIMATE button clicked.Starting animation.");
        } else {
            plot.getTransforms().remove(yRotate);
            rotateY.setAngle(rotateY.getAngle() + yRotate.getAngle());
            deltaAngle = yRotate.getAngle();
            rotateAnimationTimeline.stop();
            animationRunning = false;
            LOG.finest("ANIMATE button clicked.Stopping animation.");
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
                        translateY.yProperty(), root.getHeight() / 3)));
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
            LOG.finest("Axes ON/OFF button clicked.Setting axes to invisible.");
        } else {
            axes.setVisible(true);
            LOG.finest("Axes ON/OFF button clicked.Setting axes to visible.");
        }
    }

    public void handleLights() {
        if (lightsBtn.isSelected()) {
            left.setLightOn(true);
            right.setLightOn(true);
            LOG.finest("Lights ON/OFF button clicked.Switching lights ON.");
        } else {
            left.setLightOn(false);
            right.setLightOn(false);
            LOG.finest("Lights ON/OFF button clicked.Switching lights OFF.");
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

    public void setRtAndMzResolutions(int rtRes, int mzRes) {
        this.rtResolution = rtRes;
        this.mzResolution = mzRes;
    }

    public void setScanSelection(ScanSelection scanselectn) {
        this.scanSel = scanselectn;
    }

    public void setRtMzValues(Range<Double> rt, Range<Double> mz) {
        this.rtRange = rt;
        this.mzRange = mz;
    }

    public Fx3DAxes getAxes() {
        return axes;
    }
}
