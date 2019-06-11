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

import java.io.IOException;

import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;

public class Fx3DStage extends Stage {

    private double mousePosX, mousePosY;
    private double mouseOldX, mouseOldY;

    private final Rotate rotateX = new Rotate(45, Rotate.X_AXIS);
    private final Rotate rotateY = new Rotate(0, Rotate.Y_AXIS);
    private final Translate translateX = new Translate();
    private final Translate translateY = new Translate();

    public Fx3DStage(FXMLLoader loader) throws IOException {

        StackPane root = (StackPane) loader.load();
        Scene scene = new Scene(root, 800, 600, true,
                SceneAntialiasing.BALANCED);
        PerspectiveCamera camera = new PerspectiveCamera();
        scene.setCamera(camera);

        scene.setOnMousePressed(me -> {
            mouseOldX = me.getSceneX();
            mouseOldY = me.getSceneY();
        });
        double rotateFactor = 0.08;
        scene.setOnMouseDragged(me -> {
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
        });

        root.setOnMousePressed(me -> {

        });

        makeZoomable(root);
        this.setScene(scene);

    }

    public void makeZoomable(Parent root) {

        final double MAX_SCALE = 20.0;
        final double MIN_SCALE = 0.1;

        root.addEventFilter(ScrollEvent.ANY, new EventHandler<ScrollEvent>() {

            @Override
            public void handle(ScrollEvent event) {

                double delta = 1.2;
                double scale = root.getScaleX();

                if (event.getDeltaY() < 0) {
                    scale /= delta;
                } else {
                    scale *= delta;
                }

                scale = clamp(scale, MIN_SCALE, MAX_SCALE);

                root.setScaleX(scale);
                root.setScaleY(scale);

                event.consume();

            }

        });

    }

    public static double clamp(double value, double min, double max) {

        if (Double.compare(value, min) < 0)
            return min;

        if (Double.compare(value, max) > 0)
            return max;

        return value;
    }

}
