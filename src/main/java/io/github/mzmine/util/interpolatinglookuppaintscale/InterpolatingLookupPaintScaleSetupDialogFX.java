/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.util.interpolatinglookuppaintscale;

import io.github.mzmine.util.ExitCode;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;



public class InterpolatingLookupPaintScaleSetupDialogFX extends Stage {

    private InterpolatingLookupPaintScaleSetupDialogController controller;

    public InterpolatingLookupPaintScaleSetupDialogFX(InterpolatingLookupPaintScale paintScale){

        try{

            FXMLLoader root = new FXMLLoader(getClass().getResource("InterpolatingLookupPaintScaleSetupDialogFX.fxml"));
            Parent rootPane = root.load();
            Scene scene = new Scene(rootPane);
            setScene(scene);
            setMinWidth(480);
            setMinHeight(330);

            controller = root.getController();
            controller.addPaintScaleToTableView(paintScale);

        }
        catch (IOException e)
        {
            e.printStackTrace();
        }


    }



    public ExitCode getExitCode() {
       return controller.getExitCode();
    }

    public InterpolatingLookupPaintScale getPaintScale() {

        return controller.getPaintScale();
    }

}
