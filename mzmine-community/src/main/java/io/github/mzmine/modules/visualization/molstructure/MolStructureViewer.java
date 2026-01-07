/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.visualization.molstructure;


import io.github.mzmine.util.InetUtils;
import io.github.mzmine.util.exceptions.ExceptionUtils;
import io.github.mzmine.util.javafx.WindowsMenu;
import java.net.URL;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import org.openscience.cdk.interfaces.IAtomContainer;

public class MolStructureViewer extends Stage {

  private final Font labelNameFont = Font.font("Arial", FontWeight.BOLD, 18.0);
  private final BorderPane mainPanel = new BorderPane();
  private final Scene mainScene = new Scene(mainPanel);

  private final Label loading2Dlabel = new Label("Loading 2D structure...");
  private final Label loading3Dlabel = new Label("Loading 3D structure...");
  private final Pane pane2D = new StackPane(loading2Dlabel);
  private final Pane pane3D = new StackPane(loading3Dlabel);

  /**
   * Constructor of MolStructureViewer, loads 2d and 3d structures into JPanel specified by urls
   *
   * @param name
   * @param structure2DAddress
   * @param structure3DAddress
   */
  public MolStructureViewer(String name, final URL structure2DAddress,
      final URL structure3DAddress) {

    setTitle("Structure of " + name);
    setupViewer(name, structure3DAddress!=null);

    if (structure2DAddress != null) {
      Thread loading2DThread = new Thread(() -> {
        load2DStructure(structure2DAddress);
      }, "Structure loading thread");
      loading2DThread.start();
    } else {
      loading2Dlabel.setText("2D structure not available");
    }

    if (structure3DAddress != null) {
      Thread loading3DThread = new Thread(() -> {
        load3DStructure(structure3DAddress);
      }, "Structure loading thread");
      loading3DThread.start();
    } else {
      loading3Dlabel.setText("3D structure not available");
    }

  }

  /**
   * Constructor for MolStructureViewer from AtomContainer and only for 2D object The 3D view will
   * be unavailable
   *
   * @param name
   * @param structure2D AtomContainer
   */
  public MolStructureViewer(String name, final IAtomContainer structure2D) {
    setTitle("Structure of " + name);
    setupViewer(name, false);

    if (structure2D != null) {
      Thread loading2DThread = new Thread(() -> {
        load2DStructure(structure2D);
      }, "Structure loading thread");
      loading2DThread.start();
    } else {
      loading2Dlabel.setText("2D structure not available");
    }

    loading3Dlabel.setText("3D structure not available");
  }

  /**
   * Load initial parameters for JPanel
   *
   * @param name
   */
  private void setupViewer(String name, boolean add3d) {
    // setDefaultCloseOperation(DISPOSE_ON_CLOSE);

    // Main panel - contains a title (compound name) in the top, 2D
    // structure on the left, 3D structure on the right
    Label labelName = new Label(name);
    labelName.setAlignment(Pos.CENTER);
    labelName.setTextFill(Color.BLUE);
    labelName.setPadding(new Insets(10.0));
    labelName.setFont(labelNameFont);
    if (name != null && !name.isBlank()) {
      mainPanel.setTop(labelName);
    }

    loading2Dlabel.setAlignment(Pos.CENTER);
    loading3Dlabel.setAlignment(Pos.CENTER);

    if (add3d) {
      SplitPane splitPane = new SplitPane();
      splitPane.getItems().addAll(pane2D, pane3D);
      // splitPane.setResizeWeight(0.5);
      splitPane.setOrientation(Orientation.HORIZONTAL);
      mainPanel.setCenter(splitPane);
      splitPane.setDividerPosition(0, 0.5);
    } else {
      mainPanel.setCenter(pane2D);
    }

    setMinWidth(600.0);
    setMinHeight(400.0);

    // Add the Windows menu
    WindowsMenu.addWindowsMenu(mainScene);
    setScene(mainScene);
  }

  /**
   * Load the structure passed as parameter in JChemViewer
   */
  private void load2DStructure(URL url) {

    Node newComponent;
    try {
      String structure2D = InetUtils.retrieveData(url);
      if (structure2D.length() < 10) {
        loading2Dlabel.setText("2D structure not available");
        return;
      }
      newComponent = Structure2DComponent.create(structure2D);
    } catch (Exception e) {
      String errorMessage =
          "Could not load 2D structure\n" + "Exception: " + ExceptionUtils.exceptionToString(e);
      newComponent = new Label(errorMessage);
    }
    final Node newComponentFinal = newComponent;
    Platform.runLater(() -> {
      pane2D.getChildren().clear();
      pane2D.getChildren().add(newComponentFinal);
    });
  }

  /**
   * Load the AtomContainer passed as parameter in JChemViewer
   *
   * @param container
   */
  private void load2DStructure(IAtomContainer container) {
    Node newComponent;
    try {
      newComponent = new Structure2DComponent(container);
    } catch (Exception e) {
      e.printStackTrace();
      String errorMessage =
          "Could not load 2D structure\n" + "Exception: " + ExceptionUtils.exceptionToString(e);
      newComponent = new Label(errorMessage);
    }
    final Node newComponentFinal = newComponent;
    Platform.runLater(() -> {
      pane2D.getChildren().clear();
      pane2D.getChildren().add(newComponentFinal);
    });

  }

  /**
   * Load the structure passed as parameter in JmolViewer
   */
  private void load3DStructure(URL url) {

    try {

      String structure3D = InetUtils.retrieveData(url);

      // If the returned structure is empty or too short, just return
      if (structure3D.length() < 10) {
        loading3Dlabel.setText("3D structure not available");
        return;
      }

      // Check for html tag, to recognize PubChem error message
      if (structure3D.contains("<html>")) {
        loading3Dlabel.setText("3D structure not available");
        return;
      }

      Structure3DComponent new3DComponent = new Structure3DComponent();
      final AnchorPane newComponentFinal = new AnchorPane();
      newComponentFinal.getChildren().add(new3DComponent);
      Platform.runLater(() -> {
        pane3D.getChildren().clear();
        pane3D.getChildren().add(newComponentFinal);
      });

      // loadStructure must be called after the component is added,
      // otherwise Jmol will freeze waiting for repaint (IMHO this is a
      // Jmol bug introduced in 11.8)
      new3DComponent.loadStructure(structure3D);

    } catch (Exception e) {
      e.printStackTrace();
      String errorMessage =
          "Could not load 3D structure\n" + "Exception: " + ExceptionUtils.exceptionToString(e);
      Label label = new Label(errorMessage);
      pane3D.getChildren().clear();
      pane3D.getChildren().add(label);
    }

  }
}
