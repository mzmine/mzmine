package io.github.mzmine.modules.visualization.acquisitionmetadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import io.github.mzmine.datamodel.AcquisitionMetadata;
import io.github.mzmine.datamodel.AcquisitionMetadata.Field;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import io.github.mzmine.project.impl.RawDataFileImpl;
import java.util.List;
import java.util.Map;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import testutils.MZmineTestUtil;

class AcquisitionMetadataTabTest {

  @BeforeAll
  static void initMzmine() {
    MZmineTestUtil.startMzmineCore();
    FxThread.initJavaFx();
    Platform.setImplicitExit(false);
  }

  @AfterEach
  void resetProject() {
    ProjectService.getProjectManager().setCurrentProject(new MZmineProjectImpl());
  }

  @Test
  void showsOneRowPerFileWithMetadataColumnsAndEditableCells() throws Exception {
    fx(() -> {
      final MZmineProjectImpl project = new MZmineProjectImpl();
      ProjectService.getProjectManager().setCurrentProject(project);
      final RawDataFileImpl first = raw("a.mzML", "method-A");
      final RawDataFileImpl second = raw("b.mzML", "method-B");
      project.addFile(second);
      project.addFile(first);
      final long sampleVersion = project.getProjectMetadata().getVersion();

      final AcquisitionMetadataTab tab = new AcquisitionMetadataTab();
      final TableView<RawDataFile> table = table(tab);
      assertEquals(List.of(first, second), table.getItems());
      assertEquals("filename", table.getColumns().getFirst().getText());
      assertFalse(table.getColumns().getFirst().isEditable());
      assertTrue(table.isEditable());
      assertEquals("method-A", column(table, "MethodName").getCellData(first));
      assertEquals("Q Exactive", column(table, "Instrument model").getCellData(first));
      assertEquals("1, 2", column(table, "MS levels").getCellData(first));
      assertEquals("MS:1001911", first.getAcquisitionMetadata().terms().getFirst().accession());
      assertTrue(table.getColumns().stream().noneMatch(column -> column.getText().contains(": ")));
      assertEquals("Acquisition: instrument model", column(table, "Instrument model").getUserData());
      assertEquals("", column(table, "Analyzer").getCellData(first));
      assertFalse(table.getColumns().stream().anyMatch(column -> column.getText().equals("Field")));

      edit(table, "MethodName", "corrected method");
      edit(table, "Instrument model", "");
      edit(table, "Scan count", "42");
      assertEquals("corrected method", column(table, "MethodName").getCellData(first));
      assertEquals("", column(table, "Instrument model").getCellData(first));
      assertEquals("42", column(table, "Scan count").getCellData(first));
      assertEquals("method-B", column(table, "MethodName").getCellData(second));
      assertEquals(0, first.getNumOfScans());
      assertEquals("method-A", first.getAcquisitionMetadata().localFields().get("MethodName"));
      assertEquals(sampleVersion, project.getProjectMetadata().getVersion());

      column(table, "MethodName").setPrefWidth(275);
      final ButtonBar buttons = (ButtonBar) ((BorderPane) tab.getContent()).getBottom();
      ((Button) buttons.getButtons().getFirst()).fire();
      assertEquals("corrected method", column(table, "MethodName").getCellData(first));
      assertEquals(275, column(table, "MethodName").getPrefWidth());
      final AcquisitionMetadataTab reopened = new AcquisitionMetadataTab();
      assertEquals("", column(table(reopened), "Instrument model").getCellData(first));
      assertEquals("42", column(table(reopened), "Scan count").getCellData(first));
    });
  }

  private static RawDataFileImpl raw(final String name, final String method) {
    final RawDataFileImpl file = spy(new RawDataFileImpl(name, "/data/" + name, null));
    doReturn(new int[]{1, 2}).when(file).getMSLevels();
    file.setAcquisitionMetadata(new AcquisitionMetadata(AcquisitionMetadata.resolveLabel(
        Field.INSTRUMENT_MODEL, "Q Exactive"), Map.of("MethodName", method)));
    return file;
  }

  @SuppressWarnings("unchecked")
  private static TableView<RawDataFile> table(final AcquisitionMetadataTab tab) {
    return (TableView<RawDataFile>) ((BorderPane) tab.getContent()).getCenter();
  }

  @SuppressWarnings("unchecked")
  private static TableColumn<RawDataFile, String> column(final TableView<RawDataFile> table,
      final String name) {
    return (TableColumn<RawDataFile, String>) table.getColumns().stream()
        .filter(column -> column.getText().equals(name)).findFirst().orElseThrow();
  }

  private static void edit(final TableView<RawDataFile> table, final String name,
      final String value) {
    final TableColumn<RawDataFile, String> column = column(table, name);
    column.getOnEditCommit().handle(new TableColumn.CellEditEvent<>(table,
        new TablePosition<>(table, 0, column), TableColumn.editCommitEvent(), value));
  }

  private static void fx(final Runnable test) throws Exception {
    final FutureTask<Void> task = new FutureTask<>(test, null);
    Platform.runLater(task);
    task.get(15, TimeUnit.SECONDS);
  }
}
