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

import java.util.logging.Logger;

import com.google.common.collect.Range;

import javafx.event.EventHandler;
import javafx.scene.DepthTest;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.*;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;
import net.sf.mzmine.main.MZmineCore;

public class New3DJavafxStage extends Stage{
	
	final Group plot = new Group();
	final Group finalNode = new Group();
	private static final Logger LOG = Logger.getLogger(New3DJavafxStage.class.getName());
	private static final int SIZE = 500;
	private static float AMPLIFI = 130;
	private double mousePosX, mousePosY;
    private double mouseOldX, mouseOldY;
    
    private final Rotate rotateX = new Rotate(45, Rotate.X_AXIS);
    private final Rotate rotateY = new Rotate(0, Rotate.Y_AXIS);
    private final Translate translateX = new Translate();
    private final Translate translateY = new Translate();
    
    
	public New3DJavafxStage(float[][] intensityValues,int rtResolution,int mzResolution,double maxBinnedIntensity,Range<Double> rtRange,Range<Double> mzRange){
        plot.getTransforms().addAll(rotateX, rotateY);
        finalNode.getTransforms().addAll(translateX,translateY);
        finalNode.getChildren().add(plot);
        StackPane root = new StackPane();
        root.getChildren().add(finalNode);
        
        
        
        TriangleMesh mesh = new TriangleMesh();
    
        int[][] peakListIndices = new int[rtResolution][mzResolution];
        
        
        float factorX = (float)SIZE/rtResolution;
        float factorZ = (float)SIZE/mzResolution;
        
        float maxIntensityValue = Float.NEGATIVE_INFINITY;
        for(int i=0;i<rtResolution;i++){
        	for(int j=0;j<mzResolution;j++) {
        		if(maxIntensityValue<intensityValues[i][j]) {
        			maxIntensityValue = intensityValues[i][j];
        		}
        	}
        }
        
        for (int x = 0; x < rtResolution; x++) {
            for (int z = 0; z < mzResolution; z++) {
                mesh.getPoints().addAll((float)x*factorX, -intensityValues[x][z]* AMPLIFI,(float)z*factorZ);
                if(intensityValues[x][z]>0.022*maxIntensityValue){
                	peakListIndices[x][z]=1;
                }
            }
        }
        
        int rtLength = rtResolution;
        int mzLength = mzResolution;
        float rtTotal = rtLength;
        float mzTotal = mzResolution;
        
        for (float x = 0; x < rtLength - 1; x++) {
            for (float y = 0; y < mzLength - 1; y++) {

                float x0 = x / rtTotal;
                float y0 = y / mzTotal;
                float x1 = (x + 1) / rtTotal;
                float y1 = (y + 1) / mzTotal;

                mesh.getTexCoords().addAll( //
                        x0, y0, // 0, top-left
                        x0, y1, // 1, bottom-left
                        x1, y0, // 2, top-right
                        x1, y1 // 3, bottom-right
                );
            }
        }

        // faces
        for (int x = 0; x < rtLength - 1; x++) {
            for (int z = 0; z < mzLength - 1; z++) {

                int tl = x * mzLength + z; // top-left
                int bl = x * mzLength + z + 1; // bottom-left
                int tr = (x + 1) * mzLength + z; // top-right
                int br = (x + 1) * mzLength + z + 1; // bottom-right

                int offset = (x * (mzLength - 1) + z ) * 8 / 2; // div 2 because we have u AND v in the list

                // working
                mesh.getFaces().addAll(bl, offset + 1, tl, offset + 0, tr, offset + 2);
                mesh.getFaces().addAll(tr, offset + 2, br, offset + 3, bl, offset + 1);

            }
        }
        int width = rtLength;
        int height = mzLength;
        
        WritableImage wr = new WritableImage(width, height);
        PixelWriter pw = wr.getPixelWriter();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {

                if(peakListIndices[x][y]==1) {
                	 Color color = Color.BLUE;
                	 pw.setColor(x, y, color);
                	 if(x-1>=0&&y-1>=0) {
                		 pw.setColor(x-1, y-1, color);
                		 pw.setColor(x, y-1, color);
                		 pw.setColor(x-1, y, color);
                	 }
                }
                else {
                	Color color = Color.SILVER;
                	pw.setColor(x,y,color);
                }
            }
        }
        Image diffuseMap = wr;
       
        PhongMaterial material = new PhongMaterial();
        material.setDiffuseMap(diffuseMap);
       
        MeshView meshView = new MeshView(mesh);
        meshView.setMaterial(material);
        meshView.setCullFace(CullFace.NONE);
        meshView.setDrawMode(DrawMode.FILL);
        meshView.setDepthTest(DepthTest.ENABLE);

        plot.getChildren().addAll(meshView);
        
        buildAxes(rtRange,mzRange,maxBinnedIntensity);
        
        
        Scene scene = new Scene(root, 800, 600, true, SceneAntialiasing.BALANCED);
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
        	if(me.isPrimaryButtonDown()) {
		        rotateX.setAngle(rotateX.getAngle() + rotateFactor*(mousePosY - mouseOldY));
		        rotateY.setAngle(rotateY.getAngle() - rotateFactor*(mousePosX - mouseOldX));     
        	}
            if(me.isSecondaryButtonDown()) {
            	translateX.setX(translateX.getX() + (mousePosX - mouseOldX) );
            	translateY.setY(translateY.getY() + (mousePosY - mouseOldY) );
        	}
            mouseOldX = mousePosX;
	        mouseOldY = mousePosY;
        });
        
        makeZoomable(root);
        this.setScene(scene);
        
	}
	
	public void buildAxes(Range<Double> rtRange,Range<Double> mzRange,double maxBinnedIntensity) {
		
		//rtAxis
		double rtDelta = (rtRange.upperEndpoint() - rtRange.lowerEndpoint())/7;
        double rtscaleValue = rtRange.lowerEndpoint();
        Text rtLabel = new Text( "Retention Time");
        rtLabel.setRotationAxis(Rotate.X_AXIS);
        rtLabel.setRotate(-45);
        rtLabel.setTranslateX(SIZE*3/8);
        rtLabel.setTranslateZ(-25);
        rtLabel.setTranslateY(13);
        plot.getChildren().add(rtLabel);
        for( int y=0; y <= SIZE; y+=SIZE/7) {
            Line tickLineX = new Line(0,0,0,9);
            tickLineX.setRotationAxis(Rotate.X_AXIS);
            tickLineX.setRotate(-90);
            tickLineX.setTranslateY(-2);
            tickLineX.setTranslateX(y);
            tickLineX.setTranslateZ(-3.5);
            Text text = new Text( ""+(int)rtscaleValue);
            text.setRotationAxis(Rotate.X_AXIS);
            text.setRotate(-45);
            text.setTranslateY(8);
            text.setTranslateX(y-5);
            text.setTranslateZ(-13);
            rtscaleValue += rtDelta; 
            plot.getChildren().addAll(text,tickLineX);
        }
        
        //mzAxis
        double mzDelta = (mzRange.upperEndpoint() - mzRange.lowerEndpoint())/7;
        double mzScaleValue = mzRange.upperEndpoint();
        Group mzAxisTicks = new Group();
        Group mzAxisLabels = new Group();
        Text mzLabel = new Text( "m/z");
        mzLabel.setRotationAxis(Rotate.X_AXIS);
        mzLabel.setRotate(-45);
        mzLabel.setTranslateX(SIZE/2);
        mzLabel.setTranslateZ(-22);
        mzLabel.setTranslateY(8);
        mzAxisLabels.getChildren().add(mzLabel);
        for( int y=0; y <= SIZE; y+=SIZE/7) {
        	Line tickLineZ = new Line(0,0,0,9);
            tickLineZ.setRotationAxis(Rotate.X_AXIS);
            tickLineZ.setRotate(-90);
            tickLineZ.setTranslateY(-2);
            tickLineZ.setTranslateX(y);
            float roundOff = (float) (Math.round(mzScaleValue * 100.0) / 100.0);
            Text text = new Text( ""+(float)roundOff);
            text.setRotationAxis(Rotate.X_AXIS);
            text.setRotate(-45);
            text.setTranslateY(8);
            text.setTranslateX(y-5);
            text.setTranslateZ(-7);
            mzScaleValue -= mzDelta; 
            mzAxisTicks.getChildren().add(tickLineZ);
            mzAxisLabels.getChildren().add(text);
        }
        mzAxisTicks.setRotationAxis(Rotate.Y_AXIS);
        mzAxisTicks.setRotate(90);
        mzAxisTicks.setTranslateX(-SIZE/2);
        mzAxisTicks.setTranslateZ(SIZE/2);
        mzAxisLabels.setRotationAxis(Rotate.Y_AXIS);
        mzAxisLabels.setRotate(90);
        mzAxisLabels.setTranslateX(-SIZE/2-SIZE/14);
        mzAxisLabels.setTranslateZ(SIZE/2);
        plot.getChildren().addAll(mzAxisTicks,mzAxisLabels);
        
        //intensityAxis
        
        int numScale =5;
        double gapLen = (AMPLIFI/numScale);
        double transLen = 0;
        double intensityDelta = maxBinnedIntensity/numScale;
        double intensityValue = 0;
        
        Text intensityLabel = new Text( "Intensity");
        intensityLabel.setTranslateX(-75);
        intensityLabel.setRotationAxis(Rotate.Y_AXIS);
        intensityLabel.setRotate(-45);
        intensityLabel.setRotationAxis(Rotate.Z_AXIS);
        intensityLabel.setRotate(90);
        intensityLabel.setTranslateZ(-40);
        intensityLabel.setTranslateY(-70);
        plot.getChildren().add(intensityLabel);
        for(int y=0;y<=numScale;y++){ 
        	Line tickLineY = new Line(0,0,7,0);
        	tickLineY.setRotationAxis(Rotate.Y_AXIS);
        	tickLineY.setRotate(135);
        	tickLineY.setTranslateX(-7);
        	tickLineY.setTranslateZ(-7);
        	tickLineY.setTranslateY(-transLen);
        	LOG.info("ADebugTag" + "Value: " + Double.toString(transLen));
        	plot.getChildren().add(tickLineY);
        	
        	Text text = new Text( ""+MZmineCore.getConfiguration().getIntensityFormat().format(intensityValue));
   		  	intensityValue += intensityDelta;
   		   	text.setRotationAxis(Rotate.Y_AXIS);
   		   	text.setRotate(-45); 
   		   	text.setTranslateY(-transLen+5);
   		   	text.setTranslateX(-40);
   		   	text.setTranslateZ(-30);
   		   	plot.getChildren().add(text);
   		   	transLen += gapLen;
        }
        
        
        Line lineX = new Line(0,0,SIZE,0);
        plot.getChildren().add(lineX);
        Line lineZ = new Line(0,0,SIZE,0);
        lineZ.setRotationAxis(Rotate.Y_AXIS);
        lineZ.setRotate(90);
        lineZ.setTranslateX(-SIZE/2);
        lineZ.setTranslateZ(SIZE/2);
        plot.getChildren().add(lineZ);
        Line lineY = new Line(0,0,AMPLIFI,0);
        lineY.setRotate(90);
        lineY.setTranslateX(-AMPLIFI/2);
        lineY.setTranslateY(-AMPLIFI/2);
        plot.getChildren().add(lineY);
        

        LOG.info("maxbinnedintensity" + "Value: " + Double.toString(maxBinnedIntensity));
        
	}
	
	
	public void makeZoomable(StackPane control) {

        final double MAX_SCALE = 20.0;
        final double MIN_SCALE = 0.1;

        control.addEventFilter(ScrollEvent.ANY, new EventHandler<ScrollEvent>() {

            @Override
            public void handle(ScrollEvent event) {

                double delta = 1.2;
                double scale = control.getScaleX();

                if (event.getDeltaY() < 0) {
                    scale /= delta;
                } else {
                    scale *= delta;
                }

                scale = clamp(scale, MIN_SCALE, MAX_SCALE);

                control.setScaleX(scale);
                control.setScaleY(scale);

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
