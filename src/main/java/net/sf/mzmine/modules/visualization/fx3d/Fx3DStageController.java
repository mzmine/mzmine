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

import org.fxyz3d.utils.CameraTransformer;

import com.google.common.collect.Range;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;

public class Fx3DStageController {

    @FXML
    private BorderPane root;
    @FXML
    private Group subSceneRootNode;
    private Group finalNode = new Group();

    private Group plot = new Group();

    private Fx3DAxes axes = new Fx3DAxes();
    @FXML
    private TableView<Fx3DDataset> tableView = new TableView<Fx3DDataset>();
    @FXML
    private TableColumn<Fx3DDataset, String> fileNameCol = new TableColumn<Fx3DDataset, String>();
    @FXML
    private TableColumn<Fx3DDataset, String> colorCol = new TableColumn<Fx3DDataset, String>();

    private static final int SIZE = 500;
    private final Rotate rotateX = new Rotate(45, Rotate.X_AXIS);
    private final Rotate rotateY = new Rotate(0, Rotate.Y_AXIS);
    private final Translate translateX = new Translate();
    private final Translate translateY = new Translate();

    private double mousePosX, mousePosY;
    private double mouseOldX, mouseOldY;

    final double MAX_SCALE = 20.0;
    final double MIN_SCALE = 0.1;

    public double maxOfAllBinnedIntensity = Double.NEGATIVE_INFINITY;

    public ObservableList<Fx3DDataset> datasets = FXCollections
            .observableArrayList();
    public ArrayList<Color> colors = new ArrayList<Color>();

    public CameraTransformer cameraTransform = new CameraTransformer();

    public void initialize() {
        rotateX.setPivotZ(SIZE / 2);
        rotateX.setPivotX(SIZE / 2);
        rotateY.setPivotZ(SIZE / 2);
        rotateY.setPivotX(SIZE / 2);
        plot.getTransforms().addAll(rotateX, rotateY);
        translateY.setY(400);
        translateX.setX(200);
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
        fileNameCol.setCellValueFactory(
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

        PerspectiveCamera camera = new PerspectiveCamera();
        cameraTransform.getChildren().add(camera);
        PointLight cameraLight = new PointLight(Color.WHITE);
        cameraTransform.getChildren().add(cameraLight);
        cameraLight.setTranslateX(camera.getTranslateX());
        cameraLight.setTranslateY(camera.getTranslateY());
        cameraLight.setTranslateZ(camera.getTranslateZ());

        SubScene scene3d = new SubScene(finalNode, 800, 600, true,
                SceneAntialiasing.BALANCED);

        scene3d.setCamera(camera);
        scene3d.setPickOnBounds(true);
        subSceneRootNode.getChildren().add(scene3d);
    }

    public synchronized void setDataset(Fx3DDataset dataset,
            double maxBinnedIntensity, int index, int length) {
        dataset.setColor(colors.get(index).toString());
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

    public void onScrollHandler(ScrollEvent event) {
        double delta = 1.2;
        double scale = (finalNode.getScaleX());

        if (event.getDeltaY() < 0) {
            scale /= delta;
        } else {
            scale *= delta;
        }

        scale = clamp(scale, MIN_SCALE, MAX_SCALE);

        finalNode.setScaleX(scale);
        finalNode.setScaleY(scale);

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
