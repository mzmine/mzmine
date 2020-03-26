package io.github.mzmine.modules.dataprocessing.id_sirius;

import io.github.msdk.datamodel.IonAnnotation;
import io.github.mzmine.datamodel.PeakListRow;
import io.github.mzmine.modules.dataprocessing.id_sirius.table.SiriusCompound;
import io.github.mzmine.taskcontrol.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;

public class ResultWindowFX extends Stage {
    private ResultWindowController controller;

    public ResultWindowFX(PeakListRow peakListRow, Task searchTask)
    {
        try
        {
            FXMLLoader root = new FXMLLoader(getClass().getResource("ResultWindow.fxml"));
            Parent rootPane = root.load();
            Scene scene = new Scene(rootPane);
            setScene(scene);
            controller = root.getController();
            controller.initValues(peakListRow, searchTask);

        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    public void addNewListItem(final SiriusCompound compound)
    {
        controller.addNewListItem(compound);
    }
    public void addListofItems(final List<IonAnnotation> annotations)
    {
        controller.addListofItems(annotations);
    }
    public void dispose(){
        controller.dispose();
    }
}
