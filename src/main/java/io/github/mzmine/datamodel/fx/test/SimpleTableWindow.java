package io.github.mzmine.datamodel.fx.test;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableColumn.CellDataFeatures;
import javafx.scene.control.TreeTableView;
import javafx.stage.Stage;

public class SimpleTableWindow extends Application {
  Logger logger = Logger.getLogger(this.getClass().getName());


  @Override
  public void start(Stage stage) {
    logger.info("Init small test");
    TreeTableView<Data> table = new TreeTableView<>();
    List<Data> data = new ArrayList<>();

    // add column
    TreeTableColumn<Data, String> col = new TreeTableColumn<>("Name");
    // col.setCellValueFactory(new TreeItemPropertyValueFactory<>("myName"));
    col.setCellValueFactory((CellDataFeatures<Data, String> cell) -> {
      if (cell.getValue() != null && cell.getValue().getValue() != null) {
        logger.info("CELL VALUE FACTORY: BINDING");
        return cell.getValue().getValue().myNameProperty();
      } else
        logger.info("CELL VALUE FACTORY: BINDING NULLVALUENULL");
      return null;
    });

    col.setCellFactory((TreeTableColumn<Data, String> param) -> {
      return new TreeTableCell<Data, String>() {
        @Override
        protected void updateItem(String item, boolean empty) {
          if (item == null || empty) {
            setText("");
            logger.info("CELL FAC: updateItem EMPTY: " + item + " " + empty);
          } else {
            logger.info("CELL FAC: updateItem " + item);
            setText(item);
          }
        }
      };
    });
    table.getColumns().add(col);


    Scene scene = new Scene(table);
    stage.setScene(scene);
    stage.setMaximized(true);
    stage.show();

    // add data
    logger.info("add data");
    data.add(new Data("H"));
    data.add(new Data("KK"));

    TreeItem<Data> root = new TreeItem<>(new Data("ROOT"));
    table.setRoot(root);
    root.setExpanded(true);
    table.setShowRoot(false);
    root.getChildren()
        .addAll(data.stream().map(d -> new TreeItem<Data>(d)).toArray(TreeItem[]::new));

    logger.info("init done");
  }

  public static void startThisApp(String[] args) {
    launch(args);
  }

}


