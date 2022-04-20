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

package io.github.mzmine.modules.visualization.spectra.spectralmatchresults;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.spectraldb.entry.SpectralDBAnnotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

/**
 * Window to show all spectral libraries matches from selected scan or feature list match
 *
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de) & SteffenHeu (s_heuc03@uni-muenster.de)
 */
public class SpectraIdentificationResultsWindowFX extends Stage {

  private final Logger logger = Logger.getLogger(this.getClass().getName());

  private final Font headerFont = new Font("Dialog Bold", 16);
  private final GridPane pnGrid;
  private final javafx.scene.control.ScrollPane scrollPane;
  private final List<SpectralDBAnnotation> totalMatches;
  private final Map<SpectralDBAnnotation, SpectralMatchPanelFX> matchPanels;
  // couple y zoom (if one is changed - change the other in a mirror plot)
  private boolean isCouplingZoomY;

  private final Label noMatchesFound;

  private final BorderPane pnMain;

  public SpectraIdentificationResultsWindowFX() {
    super();

    pnMain = new BorderPane();
    this.setScene(new Scene(pnMain));
    getScene().getStylesheets()
        .addAll(MZmineCore.getDesktop().getMainWindow().getScene().getStylesheets());

    pnMain.setPrefSize(1000, 600);
    pnMain.setMinSize(700, 500);
    setMinWidth(700);
    setMinHeight(500);

    setTitle("Processing...");

    pnGrid = new GridPane();
    // any number of rows

    noMatchesFound = new Label("I'm working on it");
    noMatchesFound.setFont(headerFont);
    // yellow
    noMatchesFound.setTextFill(Color.web("0xFFCC00"));
    pnGrid.add(noMatchesFound, 0, 0);
    pnGrid.setVgap(5);

    // Add the Windows menu
    MenuBar menuBar = new MenuBar();
    // menuBar.add(new WindowsMenu());

    Menu menu = new Menu("Menu");

    // set font size of chart
    MenuItem btnSetup = new MenuItem("Setup dialog");
    btnSetup.setOnAction(e -> {
      Platform.runLater(() -> {
        if (MZmineCore.getConfiguration()
            .getModuleParameters(SpectraIdentificationResultsModule.class)
            .showSetupDialog(true) == ExitCode.OK) {
          showExportButtonsChanged();
        }
      });
    });

    menu.getItems().add(btnSetup);

    CheckMenuItem cbCoupleZoomY = new CheckMenuItem("Couple y-zoom");
    cbCoupleZoomY.setSelected(true);
    cbCoupleZoomY.setOnAction(e -> setCoupleZoomY(cbCoupleZoomY.isSelected()));
    menu.getItems().add(cbCoupleZoomY);

    menuBar.getMenus().add(menu);
    pnMain.setTop(menuBar);

    scrollPane = new ScrollPane(pnGrid);
    pnMain.setCenter(scrollPane);
    scrollPane.setHbarPolicy(ScrollBarPolicy.AS_NEEDED);
    scrollPane.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);

    totalMatches = new ArrayList<>();
    matchPanels = new HashMap<>();
    setCoupleZoomY(true);

    show();
  }

  public void setCoupleZoomY(boolean selected) {
    isCouplingZoomY = selected;

    synchronized (matchPanels) {
      matchPanels.values().stream().filter(Objects::nonNull)
          .forEach(pn -> pn.setCoupleZoomY(selected));
    }
  }

  /**
   * Add a new match and sort the view. Call from {@link Platform#runLater}.
   *
   * @param match
   */
  public synchronized void addMatches(SpectralDBAnnotation match) {
    if (!totalMatches.contains(match)) {
      // add
      totalMatches.add(match);
      SpectralMatchPanelFX pn = new SpectralMatchPanelFX(match);
      pn.setCoupleZoomY(isCouplingZoomY);
      pn.prefWidthProperty().bind(this.widthProperty());
      matchPanels.put(match, pn);

      //pnGrid.add(pn, 0, matchPanels.size() - 1);

      // sort and show
      sortTotalMatches();
    }
  }

  /**
   * add all matches and sort the view.
   * Call from {@link Platform#runLater}.
   *
   * @param matches
   */
  public synchronized void addMatches(List<SpectralDBAnnotation> matches) {
    if (matches.isEmpty()) {
      return;
    }
    // add all
    for (SpectralDBAnnotation match : matches) {
      if (!totalMatches.contains(match)) {
        // add
        totalMatches.add(match);
        SpectralMatchPanelFX pn = new SpectralMatchPanelFX(match);
        pn.setCoupleZoomY(isCouplingZoomY);
        pn.prefWidthProperty().bind(this.widthProperty());
        matchPanels.put(match, pn);
      }
    }
    // sort and show
    sortTotalMatches();
  }

  /**
   * Sort all matches and renew panels
   */
  public void sortTotalMatches() {
    if (totalMatches.isEmpty()) {
      return;
    }

    // reversed sorting (highest cosine first
    synchronized (totalMatches) {
      totalMatches.sort((SpectralDBAnnotation a, SpectralDBAnnotation b) -> Double
          .compare(b.getSimilarity().getScore(), a.getSimilarity().getScore()));
    }
    // renew layout and show
    renewLayout();
  }

  public void setMatchingFinished() {
    if (totalMatches.isEmpty()) {
      noMatchesFound.setText("Sorry no matches found");
      noMatchesFound.setTextFill(Color.RED);
    }
  }

  /**
   * Removes panels and puts them in order.
   */
  private void renewLayout() {
//    Platform.runLater(() -> {
    // add all panel in order
    synchronized (totalMatches) {
      pnGrid.getChildren().clear();
      int row = 0;
      for (SpectralDBAnnotation match : totalMatches) {
        Pane pn = matchPanels.get(match);
        if (pn != null) {
          pnGrid.add(pn, 0, row);
          row++;
        }
      }
      }
//    });
  }

  private void showExportButtonsChanged() {
    if (matchPanels == null) {
      return;
    }
    matchPanels.values().stream().forEach(pn -> {
      pn.applySettings(MZmineCore.getConfiguration()
          .getModuleParameters(SpectraIdentificationResultsModule.class));
    });
  }


  protected void removeMatch(SpectralMatchPanelFX pn) {
    pn.prefWidthProperty().unbind();
    pnGrid.getChildren().remove(pn);

    totalMatches.remove(pn.getHit());
    matchPanels.remove(pn.getHit());
  }
}
