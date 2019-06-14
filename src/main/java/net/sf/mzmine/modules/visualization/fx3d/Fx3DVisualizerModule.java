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
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import com.google.common.collect.Range;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.input.ScrollEvent;
import javafx.stage.Stage;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineRunnableModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.selectors.ScanSelection;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskPriority;
import net.sf.mzmine.util.ExitCode;
import net.sf.mzmine.util.scans.ScanUtils;

public class Fx3DVisualizerModule implements MZmineRunnableModule {

    private static final Logger LOG = Logger
            .getLogger(Fx3DVisualizerModule.class.getName());

    private static final String MODULE_NAME = "Fx 3D visualizer";
    private static final String MODULE_DESCRIPTION = "Fx 3D visualizer."; // TODO

    private Fx3DDataset dataset;

    @Override
    public @Nonnull String getName() {
        return MODULE_NAME;
    }

    @Override
    public @Nonnull String getDescription() {
        return MODULE_DESCRIPTION;
    }

    @SuppressWarnings("null")
    @Override
    @Nonnull
    public ExitCode runModule(@Nonnull MZmineProject project,
            @Nonnull ParameterSet parameters, @Nonnull Collection<Task> tasks) {

        final RawDataFile[] dataFiles = parameters
                .getParameter(Fx3DVisualizerParameters.dataFiles).getValue()
                .getMatchingRawDataFiles();
        final ScanSelection scanSel = parameters
                .getParameter(Fx3DVisualizerParameters.scanSelection)
                .getValue();
        int len = dataFiles.length;
        Scan scans[][] = new Scan[len][];
        for (int i = 0; i < dataFiles.length; i++) {
            scans[i] = scanSel.getMatchingScans(dataFiles[i]);
        }
        Range<Double> rtRange = ScanUtils.findRtRange(scans[0]);

        final Desktop desktop = MZmineCore.getDesktop();

        // Check scan numbers.
        if (scans.length == 0) {
            desktop.displayErrorMessage(MZmineCore.getDesktop().getMainWindow(),
                    "No scans found");
            return ExitCode.ERROR;

        }

        ParameterSet myParameters = MZmineCore.getConfiguration()
                .getModuleParameters(Fx3DVisualizerModule.class);
        Range<Double> mzRange = myParameters
                .getParameter(Fx3DVisualizerParameters.mzRange).getValue();
        int rtRes = myParameters
                .getParameter(Fx3DVisualizerParameters.rtResolution).getValue();
        int mzRes = myParameters
                .getParameter(Fx3DVisualizerParameters.mzResolution).getValue();
        try {

            Platform.setImplicitExit(false);
            Platform.runLater(() -> {
                FXMLLoader loader = new FXMLLoader(
                        (getClass().getResource("Fx3DStage.fxml")));
                Node nodeFromFXML = null;
                try {
                    nodeFromFXML = loader.load();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Fx3DStageController controller = loader.getController();
                MZmineCore.getTaskController()
                        .addTask(
                                new Fx3DSamplingTask(dataFiles, scans, rtRange,
                                        mzRange, rtRes, mzRes, controller),
                                TaskPriority.HIGH);
                Scene scene = new Scene((Parent) nodeFromFXML, 800, 600, true,
                        SceneAntialiasing.BALANCED);
                PerspectiveCamera camera = new PerspectiveCamera();
                scene.setCamera(camera);
                makeZoomable(scene);
                Stage newStage = new Stage();
                newStage.setTitle(dataFiles[0].toString());
                newStage.setScene(scene);
                newStage.show();
            });

        } catch (Throwable e) {
            e.printStackTrace();
            // Missing Java3D may cause UnsatisfiedLinkError or
            // NoClassDefFoundError.
            final String msg = "Error initializing Java3D. Please file an issue at https://github.com/mzmine/mzmine2/issues and include the complete output of your MZmine console.";
            LOG.log(Level.SEVERE, msg, e);
            desktop.displayErrorMessage(MZmineCore.getDesktop().getMainWindow(),
                    msg);
        }

        return ExitCode.OK;

    }

    public void makeZoomable(Scene scene) {

        final double MAX_SCALE = 20.0;
        final double MIN_SCALE = 0.1;

        scene.addEventFilter(ScrollEvent.ANY, event -> {

            double delta = 1.2;
            double scale = ((Node) scene.getRoot()).getScaleX();

            if (event.getDeltaY() < 0) {
                scale /= delta;
            } else {
                scale *= delta;
            }

            scale = clamp(scale, MIN_SCALE, MAX_SCALE);

            scene.getRoot().setScaleX(scale);
            scene.getRoot().setScaleY(scale);

            event.consume();

        });
    }

    public static double clamp(double value, double min, double max) {

        if (Double.compare(value, min) < 0)
            return min;

        if (Double.compare(value, max) > 0)
            return max;

        return value;
    }

    @Override
    public @Nonnull MZmineModuleCategory getModuleCategory() {
        return MZmineModuleCategory.VISUALIZATIONRAWDATA;
    }

    @Override
    public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
        return Fx3DVisualizerParameters.class;
    }

}
