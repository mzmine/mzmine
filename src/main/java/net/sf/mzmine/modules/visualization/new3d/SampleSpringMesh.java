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

package net.sf.mzmine.modules.visualization.new3d;

import org.fxyz3d.shapes.primitives.SpringMesh;
import org.fxyz3d.utils.CameraTransformer;

import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.paint.Color;
import javafx.scene.shape.CullFace;
import javafx.stage.Stage;

public class SampleSpringMesh extends Stage {

    final PerspectiveCamera camera = new PerspectiveCamera(true);
    
    public SampleSpringMesh(String title) {
	    camera.setNearClip(0.1);
	    camera.setFarClip(10000.0);
	    camera.setTranslateX(10);
	    camera.setTranslateZ(-100);
	    camera.setFieldOfView(20);
	    
	    CameraTransformer cameraTransform = new CameraTransformer();
        cameraTransform.getChildren().add(camera);
        cameraTransform.ry.setAngle(-30.0);
        cameraTransform.rx.setAngle(-15.0);
        
        SpringMesh spring = new SpringMesh(10, 2, 2, 8 * 2 * Math.PI, 200, 100, 0, 0);
        spring.setCullFace(CullFace.NONE);
        spring.setTextureModeVertices3D(1530, p -> p.f);
	    
        Group group = new Group(cameraTransform, spring);
        

        Scene scene = new Scene(group, 600, 400, true, SceneAntialiasing.BALANCED);
        scene.setFill(Color.BISQUE);
        scene.setCamera(camera);
        
        this.setScene(scene);	    
        this.setTitle(title);
	}
	
}
