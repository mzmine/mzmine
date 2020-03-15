package io.github.mzmine.util.dialogs;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.openscience.cdk.config.IsotopeFactory;
import org.openscience.cdk.config.Isotopes;
import org.openscience.cdk.event.ICDKChangeListener;
import org.openscience.cdk.interfaces.IIsotope;

import java.util.EventObject;

public class PeriodicTableDialog extends Stage implements ICDKChangeListener {

    private DialogController periodicTable;
    private IIsotope selectedIsotope;


    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("PeriodicTablePanel.fxml"));
        primaryStage.setScene(new Scene(root, 700, 400));
        primaryStage.show();


    }

    public void stateChanged(EventObject event) {

        if (event.getSource() == periodicTable) {
            try {
                IsotopeFactory isoFac = Isotopes.getInstance();
                selectedIsotope = isoFac.getMajorIsotope(periodicTable.getElementSymbol());
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.exit(0);
        }
    }

    public IIsotope getSelectedIsotope() {
        return selectedIsotope;
    }

}

