/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.visualization.fx3d;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.util.components.ButtonCell;
import io.github.mzmine.util.components.ColorPickerTableCell;
import io.github.mzmine.util.components.SliderCell;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
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
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.util.Duration;
import org.controlsfx.glyphfont.Glyph;
import org.jetbrains.annotations.NotNull;

/**
 * @author akshaj The controller class of the Fx3DVisualizer which handles all user actions and
 *         shows the plot along with the table.
 */
public class Fx3DBorderPaneController {

  //@FXML
  //private Stage stage;
  //@FXML
  //private Scene scene;
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
  private Menu addDatafileMenu;
  @FXML
  private Menu addFeatureMenu;
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
  private TableView<Fx3DAbstractDataset> tableView;
  @FXML
  private TableColumn<Fx3DAbstractDataset, String> fileNameCol;
  @FXML
  private TableColumn<Fx3DAbstractDataset, Color> colorCol;
  @FXML
  private TableColumn<Fx3DAbstractDataset, Double> opacityCol;
  @FXML
  private TableColumn<Fx3DAbstractDataset, Boolean> visibilityCol;
  @FXML
  private Group finalNode;
  private Group plot = new Group();
  private Group lights = new Group();
  private Group meshViews = new Group();
  private static final int SIZE = 500;
  private final Rotate rotateX = new Rotate(30, Rotate.X_AXIS);
  private final Rotate rotateY = new Rotate(0, Rotate.Y_AXIS);
  private final Translate translateX = new Translate();
  private final Translate translateY = new Translate();
  private Logger logger = Logger.getLogger(this.getClass().getName());
  private int rtResolution, mzResolution;
  private double mousePosX, mousePosY;
  private double mouseOldX, mouseOldY;

  final double MAX_SCALE = 10.0;
  final double MIN_SCALE = 0.7;
  final double DEFAULT_SCALE = 1.0;

  private double maxOfAllBinnedIntensity = Double.NEGATIVE_INFINITY;

  private ObservableList<Fx3DAbstractDataset> visualizedMeshPlots =
      FXCollections.observableArrayList();
  private List<Object> visualizedFiles = new ArrayList<Object>();
  private ObservableList<Node> meshViewList = FXCollections.observableArrayList();
  private FeatureList[] allFeatureLists;
  private PerspectiveCamera camera = new PerspectiveCamera();
  private ScanSelection scanSel;
  private List<RawDataFile> allDataFiles;
  private List<Feature> featureSelections;
  private Timeline rotateAnimationTimeline;
  boolean animationRunning = false;
  private Range<Float> rtRange;
  private Range<Double> mzRange;
  public Translate pivot = new Translate(250, 0, 250);
  public Rotate yRotate = new Rotate(0, Rotate.Y_AXIS);
  public Rotate yRotateDelta = new Rotate();
  double deltaAngle;
  private PointLight top;
  private PointLight bottom;
  private PointLight left;
  private PointLight right;
  private PointLight front;
  private PointLight back;
  private int axesPosition = 0;

  @FXML
  public void initialize() {
    // Use main CSS
    //scene.getStylesheets()
    //    .addAll(MZmineCore.getDesktop().getMainWindow().getScene().getStylesheets());

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
    colorCol.setCellFactory(column -> new ColorPickerTableCell<Fx3DAbstractDataset>(column));
    double minValue = 0;
    double maxValue = 1;
    opacityCol
        .setCellFactory(column -> new SliderCell<Fx3DAbstractDataset>(column, minValue, maxValue));

    visibilityCol.setCellFactory(column -> new ButtonCell<Fx3DAbstractDataset>(column,
        new Glyph("FontAwesome", "EYE"), new Glyph("FontAwesome", "EYE_SLASH")));
    axesBtn.setSelected(true);
    lightsBtn.setSelected(true);
    addLights();
    rotateAnimationTimeline = new Timeline(
        new KeyFrame(Duration.seconds(0), new KeyValue(yRotate.angleProperty(), 360)),
        new KeyFrame(Duration.seconds(50), new KeyValue(yRotate.angleProperty(), 0)));
    rotateAnimationTimeline.setCycleCount(Timeline.INDEFINITE);

    tableView.setItems(visualizedMeshPlots);
    plot.getChildren().add(meshViews);
    plot.getChildren().add(lights);
    allDataFiles = Arrays.asList(MZmineCore.getProjectManager().getCurrentProject().getDataFiles());
    allFeatureLists = MZmineCore.getProjectManager().getCurrentProject().getCurrentFeatureLists()
        .toArray(new FeatureList[0]);
    scene3D.widthProperty().bind(root.widthProperty());
    scene3D.heightProperty().bind(root.heightProperty());
    scene3D.setCamera(camera);
    scene3D.setPickOnBounds(true);

    // Add the Windows menu
    //WindowsMenu.addWindowsMenu(scene);
  }

  private void addLights() {
    top = new PointLight(Color.WHITE);
    top.setTranslateX(SIZE / 2);
    top.setTranslateZ(SIZE / 2);
    top.setTranslateY(-1000);

    bottom = new PointLight(Color.WHITE);
    bottom.setTranslateX(SIZE / 2);
    bottom.setTranslateZ(SIZE / 2);
    bottom.setTranslateY(1000);

    left = new PointLight(Color.WHITE);
    left.setTranslateX(-1000);
    left.setTranslateZ(-SIZE);
    left.setTranslateY(-500);

    right = new PointLight(Color.WHITE);
    right.setTranslateX(1500);
    right.setTranslateZ(-SIZE);
    right.setTranslateY(-500);

    front = new PointLight(Color.WHITE);
    front.setTranslateX(SIZE / 2);
    front.setTranslateZ(-1000);
    front.setTranslateY(-500);

    back = new PointLight(Color.WHITE);
    back.setTranslateX(SIZE / 2);
    back.setTranslateZ(1000);
    back.setTranslateY(-500);

    lights.getChildren().add(front);
    lights.getChildren().add(back);
    lights.getChildren().add(top);
    lights.getChildren().add(bottom);
  }

  /**
   * @param dataset This method adds the dataset to the 3D plot.
   */
  public synchronized void addDataset(Fx3DAbstractDataset dataset) {
    visualizedMeshPlots.add(dataset);
    visualizedFiles.add(dataset.getFile());
    maxOfAllBinnedIntensity = Double.NEGATIVE_INFINITY;
    for (Fx3DAbstractDataset mesh : visualizedMeshPlots) {
      if (maxOfAllBinnedIntensity < mesh.getMaxBinnedIntensity()) {
        maxOfAllBinnedIntensity = mesh.getMaxBinnedIntensity();
      }
    }
    axes.updateAxisParameters(rtRange, mzRange, maxOfAllBinnedIntensity);
    meshViewList.clear();
    for (Fx3DAbstractDataset mesh : visualizedMeshPlots) {
      mesh.normalize(maxOfAllBinnedIntensity);
      meshViewList.add(mesh.getNode());
    }
    meshViews.getChildren().clear();
    meshViews.getChildren().addAll(meshViewList);
    addMenuItems();
    addColorListener(dataset);
    addOpacityListener(dataset);
    dataset.visibilityProperty().bindBidirectional(dataset.getNode().visibleProperty());
    updateLabel();
  }

  private void updateGraph() {
    maxOfAllBinnedIntensity = Double.NEGATIVE_INFINITY;
    for (Fx3DAbstractDataset mesh : visualizedMeshPlots) {
      if (maxOfAllBinnedIntensity < mesh.getMaxBinnedIntensity()) {
        maxOfAllBinnedIntensity = mesh.getMaxBinnedIntensity();
      }
    }
    axes.updateAxisParameters(rtRange, mzRange, maxOfAllBinnedIntensity);
    meshViewList.clear();
    for (Fx3DAbstractDataset mesh : visualizedMeshPlots) {
      mesh.normalize(maxOfAllBinnedIntensity);
      meshViewList.add(mesh.getNode());
    }
    meshViews.getChildren().clear();
    meshViews.getChildren().addAll(meshViewList);
    FXCollections.sort(meshViews.getChildren(), new SortByOpacity());
    updateLabel();
  }

  private void addColorListener(Fx3DAbstractDataset dataset) {
    dataset.colorProperty().addListener((e, oldValue, newValue) -> {
      int red = (int) (newValue.getRed() * 255);
      int green = (int) (newValue.getGreen() * 255);
      int blue = (int) (newValue.getBlue() * 255);
      dataset.setNodeColor(Color.rgb(red, green, blue, dataset.opacityProperty().get()));
      dataset.getNode().setOpacity(dataset.opacityProperty().get());
      logger.finest("Color is changed from " + oldValue + " to " + newValue + " for the dataset "
          + dataset.getFileName());
    });
  }

  private void addOpacityListener(Fx3DAbstractDataset dataset) {
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

  @NotNull
  public List<Object> getVisualizedFiles() {
    return visualizedFiles;
  }

  public void updateVisualizedFiles(Collection<? extends RawDataFile> rawDataFiles) {
    if(rawDataFiles == null || rawDataFiles.isEmpty()) {
      return;
    }

    for(RawDataFile file : allDataFiles) {
      if(rawDataFiles.contains(file)) {
        if (!visualizedFiles.contains(file)) {
          MZmineCore.getTaskController().addTask(new Fx3DSamplingTask(file, scanSel, mzRange,
              rtResolution, mzResolution, this), TaskPriority.HIGH);
        }
      } else {
        visualizedFiles.remove(file);
        visualizedMeshPlots.removeIf(dataset -> file.equals(dataset.getDataFile()));
      }
    }

    updateGraph();
    addMenuItems();
  }

  class SortByOpacity implements Comparator<Node> {
    @Override
    public int compare(Node a, Node b) {
      final Double aOpacity = a.getOpacity();
      final Double bOpacity = b.getOpacity();
      return bOpacity.compareTo(aOpacity);
    }
  }

  /**
   * @param selections Adds the list of FeatureSelection from the module class.
   */
  public void addFeatureSelections(List<Feature> selections) {
    this.featureSelections = selections;
    addFeatures();
  }

  private void addFeatures() {
    for (Feature featureSelection : featureSelections) {
      Fx3DFeatureDataset featureDataset = new Fx3DFeatureDataset(featureSelection, rtResolution,
          mzResolution, rtRange, mzRange, maxOfAllBinnedIntensity, Color.rgb(255, 255, 0, 0.35));
      addDataset(featureDataset);
    }
  }

  private void addMenuItems() {
    removeMenu.getItems().clear();
    for (Fx3DAbstractDataset dataset : visualizedMeshPlots) {
      MenuItem menuItem = new MenuItem(dataset.getFileName());
      removeMenu.getItems().add(menuItem);
      menuItem.setOnAction(new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent e) {
          logger.finest("Context menu invoked. Remove Data file button clicked. Removing dataset "
              + dataset.getFileName() + " from the plot.");
          visualizedFiles.remove(dataset.getFile());
          visualizedMeshPlots.remove(dataset);
          updateGraph();
          addMenuItems();
        }
      });
    }
    addDatafileMenu.getItems().clear();
    for (RawDataFile file : allDataFiles) {
      if (!visualizedFiles.contains(file)) {
        MenuItem menuItem = new MenuItem(file.getName());
        addDatafileMenu.getItems().add(menuItem);
        final Fx3DBorderPaneController controller = this;
        menuItem.setOnAction(new EventHandler<ActionEvent>() {
          @Override
          public void handle(ActionEvent e) {
            logger.finest("Context menu invoked. Add Data file button clicked. Adding dataset "
                + file.getName() + " to the plot.");
            MZmineCore.getTaskController().addTask(new Fx3DSamplingTask(file, scanSel, mzRange,
                rtResolution, mzResolution, controller), TaskPriority.HIGH);
            addMenuItems();
          }
        });
      }
    }

    addFeatureMenu.getItems().clear();
    for (FeatureList featureList : allFeatureLists) {
      Menu peakListMenu = new Menu(featureList.getName());
      addFeatureMenu.getItems().add(peakListMenu);
      RawDataFile[] dataFiles = featureList.getRawDataFiles().toArray(RawDataFile[]::new);
      for (RawDataFile dataFile : dataFiles) {
        Menu dataFileMenu = new Menu(dataFile.getName());
        peakListMenu.getItems().add(dataFileMenu);
        Feature[] features = featureList.getFeatures(dataFile).toArray(Feature[]::new);
        for (Feature feature : features) {
          if (feature.getRawDataPointsRTRange().lowerEndpoint() >= rtRange.lowerEndpoint()
              && feature.getRawDataPointsRTRange().upperEndpoint() <= mzRange.upperEndpoint()
              && feature.getRawDataPointsMZRange().lowerEndpoint() >= mzRange.lowerEndpoint()
              && feature.getRawDataPointsMZRange().upperEndpoint() <= mzRange.upperEndpoint()) {
            if (!visualizedFiles.contains(feature)) {
              MenuItem menuItem = new MenuItem(feature.toString());
              dataFileMenu.getItems().add(menuItem);
              menuItem.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent e) {
                  logger.finest("Context menu invoked. Add Feature button clicked. Adding dataset "
                      + feature.toString() + " to the plot.");
                  Fx3DFeatureDataset featureDataset =
                      new Fx3DFeatureDataset(feature, rtResolution, mzResolution, rtRange, mzRange,
                          maxOfAllBinnedIntensity, Color.rgb(165, 42, 42, 0.9));
                  addDataset(featureDataset);
                  addMenuItems();
                }
              });
            }
          }
        }

      }

    }
  }

  private void updateLabel() {
    String title = "";
    for (Fx3DAbstractDataset dataset : visualizedMeshPlots) {
      title = title + dataset.getFileName() + " ";
    }
    //stage.setTitle(title);
    title = "3D plot of files [" + title + "], " + mzRange.lowerEndpoint().toString() + "-"
        + mzRange.upperEndpoint().toString() + " m/z, RT "
        + (float) (Math.round(rtRange.lowerEndpoint() * 100.0) / 100.0) + "-"
        + (float) (Math.round(rtRange.upperEndpoint() * 100.0) / 100.0) + " min";

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
      rotateX.setAngle(rotateX.getAngle() + rotateFactor * (mousePosY - mouseOldY));
      rotateY.setAngle(rotateY.getAngle() - rotateFactor * (mousePosX - mouseOldX));
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
    double mzDelta = (mzRange.upperEndpoint() - mzRange.lowerEndpoint()) / 7;
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
      Text text = new Text("" + roundOff);
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
    double rtDelta = (rtRange.upperEndpoint() - rtRange.lowerEndpoint()) / 7;
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
      Text text = new Text("" + roundOff);
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

  /**
   * @param event Rotates the plot to show all the axes in the direction of the camera.
   */
  public void handleRotateAxes(Event event) {
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
      axes.getIntensityRotate().setAngle(-91);
      axes.getIntensityTranslate().setZ(-SIZE);
      axes.getIntensityTranslate().setX(-5);
      setCameraAngle(90);
    } else if (axesPosition == 2) {
      axesPosition = 3;
      axes.getIntensityTranslate().setZ(-SIZE - 9);
      axes.getIntensityTranslate().setX(SIZE - 9);
      axes.getIntensityRotate().setAngle(-91);
      setRtAxis();
      setCameraAngle(135);
    } else if (axesPosition == 3) {
      axesPosition = 4;
      axes.getIntensityRotate().setAngle(181);
      axes.getIntensityTranslate().setZ(-SIZE - 6);
      axes.getIntensityTranslate().setX(-SIZE + 9);
      setCameraAngle(180);
    } else if (axesPosition == 4) {
      axesPosition = 5;
      axes.updateAxisParameters(rtRange, mzRange, maxOfAllBinnedIntensity);
      axes.getIntensityRotate().setAngle(181);
      axes.getIntensityTranslate().setZ(-SIZE + 10);
      axes.getMzAxis().setTranslateX(0);
      setRtAxis();
      setCameraAngle(135 + 90);
    } else if (axesPosition == 5) {
      axesPosition = 6;
      setCameraAngle(270);
      axes.getIntensityRotate().setAngle(89);
      axes.getIntensityTranslate().setZ(10);
      axes.getIntensityTranslate().setX(-SIZE + 2);
      setCameraAngle(270);
    } else if (axesPosition == 6) {
      axesPosition = 7;
      axes.updateAxisParameters(rtRange, mzRange, maxOfAllBinnedIntensity);
      axes.getIntensityRotate().setAngle(89);
      setCameraAngle(135 + 180);
    } else if (axesPosition == 7) {
      axesPosition = 0;
      axes.updateAxisParameters(rtRange, mzRange, maxOfAllBinnedIntensity);
      setCameraAngle(0);
    }

  }

  /**
   * Starts the rotation of the plot.
   */
  public void handleAnimate() {
    if (!animationRunning) {
      yRotate.setAngle(rotateY.getAngle() + deltaAngle);
      plot.getTransforms().addAll(pivot, yRotate, new Translate(-250, 0, -250));
      rotateAnimationTimeline.play();
      animationRunning = true;
      logger.finest("ANIMATE button clicked.Starting animation.");
    } else {
      plot.getTransforms().remove(yRotate);
      rotateY.setAngle(rotateY.getAngle() + yRotate.getAngle());
      deltaAngle = yRotate.getAngle();
      rotateAnimationTimeline.stop();
      animationRunning = false;
      logger.finest("ANIMATE button clicked.Stopping animation.");
    }
  }

  public void handleAxis(Event event) {
    if (axes.isVisible()) {
      axes.setVisible(false);
      logger.finest("Axes ON/OFF button clicked.Setting axes to invisible.");
    } else {
      axes.setVisible(true);
      logger.finest("Axes ON/OFF button clicked.Setting axes to visible.");
    }
  }

  public void handleLights() {
    if (lightsBtn.isSelected()) {
      top.setLightOn(true);
      logger.finest("Lights ON/OFF button clicked.Switching lights ON.");
    } else {
      top.setLightOn(false);
      logger.finest("Lights ON/OFF button clicked.Switching lights OFF.");
    }
  }

  /**
   * @param event Zooms in and out when the mouse is scrolled.
   */
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

  public void setRtAndMzValues(Range<Float> rt, Range<Double> mz) {
    this.rtRange = rt;
    this.mzRange = mz;
  }

  public Fx3DAxes getAxes() {
    return axes;
  }
}
