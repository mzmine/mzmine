package io.github.mzmine.datamodel.fx;

import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.data.RowData;
import io.github.mzmine.datamodel.data.types.AreaType;
import io.github.mzmine.datamodel.data.types.DataType;
import io.github.mzmine.datamodel.data.types.DetectionType;
import io.github.mzmine.datamodel.data.types.GraphicalCellData;
import io.github.mzmine.datamodel.data.types.HeightType;
import io.github.mzmine.datamodel.data.types.MZType;
import io.github.mzmine.datamodel.data.types.RTType;
import javafx.application.Application;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTablePosition;
import javafx.scene.control.TreeTableView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;
// import javafx.graphics;

public class FXTableWindow extends Application {
  private int i = 0;


  @Override
  public void start(Stage stage) {
    TreeTableView<RowData> table = new TreeTableView<>();

    // example row to create all columns
    RowData data = createRow();
    addColumns(table, data);


    // OLD TODO remove
    // TreeTableColumn<RowData, Double> col1 = new TreeTableColumn<>("MZ");
    // TreeTableColumn<RowData, Float> col2 = new TreeTableColumn<>("RT");
    //
    // col1.setCellValueFactory(r -> {
    // return new ReadOnlyObjectWrapper<>(r.getValue().getValue().getMZ());
    // });
    // col2.setCellValueFactory((r) -> {
    // return new ReadOnlyObjectWrapper<>(r.getValue().getValue().getRT());
    // });
    //
    // // make this column hold the entire Data object so we can access all fields
    // TreeTableColumn<RowData, FeatureStatus> col3 = new TreeTableColumn<>("Status");
    // col3.setPrefWidth(40);
    // col3.setCellValueFactory((r) -> {
    // return new ReadOnlyObjectWrapper<>(r.getValue().getValue().getDetectionType());
    // });
    // col3.setCellFactory(param -> new TreeTableCell<RowData, FeatureStatus>() {
    // @Override
    // protected void updateItem(FeatureStatus item, boolean empty) {
    // super.updateItem(item, empty);
    // if (empty)
    // setGraphic(null);
    // else {
    // Rectangle r1 = new Rectangle();
    // // the param is the column, bind so rects resize with column
    // r1.widthProperty().bind(param.widthProperty());
    // r1.heightProperty().bind(param.widthProperty());
    // // TODO change color
    // r1.setStyle("-fx-fill:#f3622d;");
    //
    // HBox hbox = new HBox(r1);
    // hbox.setAlignment(Pos.CENTER_LEFT);
    // setGraphic(hbox);
    // setText(null);
    // }
    // }
    // });
    // table.getColumns().addAll(col1, col2, col3);

    // Table tree root
    final TreeItem<RowData> root = new TreeItem<>();
    root.setExpanded(true);


    table.setRoot(root);
    table.setShowRoot(false);
    table.setTableMenuButtonVisible(true);
    table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    table.getSelectionModel().setCellSelectionEnabled(true);

    // enable copy on selection
    final KeyCodeCombination keyCodeCopy =
        new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_ANY);
    table.setOnKeyPressed(event -> {
      if (keyCodeCopy.match(event)) {
        copySelectionToClipboard(table, true);
      }
    });

    addData(root);

    Scene scene = new Scene(table);

    stage.setScene(scene);
    stage.show();
  }

  private void addColumns(TreeTableView<RowData> table, RowData data) {
    // for all data columns available in "data"
    data.stream().forEach(c -> {
      // value binding
      TreeTableColumn<RowData, ? extends DataType> col = new TreeTableColumn<>(c.getHeaderString());
      col.setCellValueFactory(r -> {
        Optional<? extends DataType> o = r.getValue().getValue().get(c.getClass());
        if (o.isPresent())
          return new ReadOnlyObjectWrapper<>(o.get());
        else
          return null;
      });

      // value representation
      col.setCellFactory(param -> new TreeTableCell<RowData, DataType<?>>() {
        @Override
        protected void updateItem(DataType<?> item, boolean empty) {
          super.updateItem(item, empty);
          if (empty) {
            setGraphic(null);
            setText(null);
          } else {
            if (item instanceof GraphicalCellData) {
              Node node = ((GraphicalCellData) item).getCellNode(this, param);
              setGraphic(node);
              setText(null);
            } else {
              setText(item.getFormattedString());
              setGraphic(null);
            }
          }
        }
      });

      // add to table
      table.getColumns().add(col);
    });
  }

  private void addData(TreeItem<RowData> root) {
    for (int i = 0; i < 10; i++)
      root.getChildren().add(new TreeItem<>(createRow()));

    // add one to the second item
    root.getChildren().get(1).getChildren().add(new TreeItem<>(createRow()));
  }



  @SuppressWarnings("rawtypes")
  public void copySelectionToClipboard(final TreeTableView<RowData> table, boolean addHeader) {
    final Set<Integer> rows = new TreeSet<>();
    for (final TreeTablePosition tablePosition : table.getSelectionModel().getSelectedCells()) {
      rows.add(tablePosition.getRow());
    }
    final StringBuilder strb = new StringBuilder();
    boolean firstRow = true;
    for (final Integer row : rows) {
      if (!firstRow) {
        strb.append('\n');
      } else if (addHeader) {
        for (final TreeTableColumn<RowData, ?> column : table.getColumns()) {
          strb.append(column.getText());
        }
      }
      boolean firstCol = true;
      for (final TreeTableColumn<RowData, ?> column : table.getColumns()) {
        if (!firstCol) {
          strb.append('\t');
        }
        firstCol = false;
        final Object cellData = column.getCellData(row);
        if (cellData == null)
          strb.append("");
        else if (cellData instanceof DataType<?>)
          strb.append(((DataType<?>) cellData).getFormattedString());
        else
          strb.append(cellData.toString());
      }
      firstRow = false;
    }
    final ClipboardContent clipboardContent = new ClipboardContent();
    clipboardContent.putString(strb.toString());
    Clipboard.getSystemClipboard().setContent(clipboardContent);
  }


  public static void startThisApp(String[] args) {
    launch(args);
  }


  public RowData createRow() {
    RowData data = new RowData();
    data.set(MZType.class, new MZType(50d * i));
    data.set(RTType.class, new RTType(1f * i));
    data.set(AreaType.class, new AreaType(1E4f * i));
    data.set(HeightType.class, new HeightType(2E4f * i));
    data.set(DetectionType.class, new DetectionType(FeatureStatus.DETECTED));
    i++;
    return data;
  }
}


