package io.github.mzmine.modules.dataprocessing.id_formulaprediction;

import io.github.mzmine.datamodel.PeakListRow;
import io.github.mzmine.taskcontrol.Task;
import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.IOException;


public class ResultWindowFX extends Stage {

    private ResultWindowController controller;

    public ResultWindowFX(String title, PeakListRow peakListRow, double searchedMass, int charge,
                          Task searchTask){

        try{

            FXMLLoader root = new FXMLLoader(getClass().getResource("ResultWindowFX.fxml"));
            Parent rootPane = root.load();
            Scene scene = new Scene(rootPane, 800, 800,Color.WHITE);
            setScene(scene);
            controller = root.getController();
            controller.initValues(title, peakListRow, searchedMass, charge, searchTask);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

    }

}