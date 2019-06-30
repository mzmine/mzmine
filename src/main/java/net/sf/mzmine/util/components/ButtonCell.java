package net.sf.mzmine.util.components;

import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;

public class ButtonCell<T> extends TableCell<T, Boolean> {
    ToggleButton button;

    public ButtonCell(TableColumn<T, Boolean> column) {
        button = new ToggleButton("ON");

        button.setOnMouseClicked(event -> {
            final TableView<T> tableView = getTableView();
            tableView.getSelectionModel().select(getTableRow().getIndex());
            tableView.edit(tableView.getSelectionModel().getSelectedIndex(),
                    column);
            if (button.isSelected()) {
                button.setText("OFF");
                commitEdit(false);
            } else {
                button.setText("ON");
                commitEdit(true);
            }
        });

    }

    @Override
    protected void updateItem(Boolean item, boolean empty) {
        super.updateItem(item, empty);
        if (empty) {
            setGraphic(null);
        } else {
            setGraphic(button);
        }
    }

}
