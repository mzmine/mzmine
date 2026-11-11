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

package io.github.mzmine.gui.mainwindow;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.gui.MZmineGUI;
import io.github.mzmine.gui.MZmineWindow;
import io.github.mzmine.gui.preferences.MZminePreferences;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.util.StringUtils;
import java.util.Collection;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tooltip;
import org.jetbrains.annotations.NotNull;


/**
 * This is a wrapper class to wrap any visualisation component in a pane to add to the main window.
 * Upon selection of files or feature lists in the main window, these tabs can be updated to
 * visualise the current selection. Tabs are not updated if they are not selected. However, they
 * will be updated, if a tab gets selected after a change of file or feature list selection.
 *
 * @author SteffenHeu - https://github.com/SteffenHeu - steffen.heuckeroth@uni-muenster.de
 */
public abstract class MZmineTab extends Tab {

  public static final Logger logger = Logger.getLogger(MZmineTab.class.getName());

  /**
   * Maximum width of tab headers in tab panes that hold {@link MZmineTab}s. Each line of the two
   * line tab text is truncated with an ellipsis.
   */
  public static final double MAX_TAB_WIDTH = 230;

  /**
   * Style class added to the tab (and copied to its header) while title and sub title are shown in
   * two lines. The themes use it to increase the header height.
   */
  public static final String TWO_LINE_STYLE_CLASS = "two-line-tab";

  /**
   * If checked/unchecked the tab is/is not updated according to the selection of files/lists in the
   * main window.
   */
  private final CheckBox cbUpdateOnSelection;
  private final ContextMenu contextMenu;

  private final StringProperty title = new SimpleStringProperty();
  private final StringProperty subTitle = new SimpleStringProperty();

  public MZmineTab(String title, boolean showBinding, boolean defaultBindingState) {
    super(title);

    cbUpdateOnSelection = new CheckBox("");
    cbUpdateOnSelection.setTooltip(new Tooltip(
        "If selected this tab is updated according to the current selection of raw files or feature lists."));
    cbUpdateOnSelection.setSelected(defaultBindingState);

    if (showBinding) {
      setGraphic(cbUpdateOnSelection);
    }

    this.title.set(title);
    // decision: use the plain tab text with a line break instead of a custom graphic. The overflow
    // menu of the TabPane only shows the tab text and cannot copy custom graphics.
    // Not bound because some tabs still call setText directly.
    this.title.subscribe(_ -> updateTabText());
    this.subTitle.subscribe(_ -> updateTabText());

    final Tooltip tooltip = new Tooltip();
    tooltip.textProperty()
        .bind(Bindings.createStringBinding(() -> createTabText(true), this.title, this.subTitle));
    setTooltip(tooltip);

    contextMenu = new ContextMenu();
    contextMenu.setOnShowing(e -> updateContextMenu());
    updateContextMenu();
    setContextMenu(contextMenu);
  }

  public MZmineTab(String title) {
    this(title, false, false);
  }

  /**
   * Configures a tab pane that holds {@link MZmineTab}s: limits the header width and allows two
   * line headers (title and sub title).
   *
   * @param tabPane the tab pane to configure
   */
  public static void configureTabPane(@NotNull TabPane tabPane) {
    tabPane.setTabMaxWidth(MAX_TAB_WIDTH);
    // modena limits the tab label height to 1.8333em (22 px), which cuts off the second line.
    // The header height itself is set by the themes, see TWO_LINE_STYLE_CLASS.
    tabPane.setTabMaxHeight(Double.MAX_VALUE);
  }

  public static @NotNull String getFeatureListsSubtitle(
      Collection<? extends FeatureList> featureLists) {
    String text;
    if (featureLists.size() <= 2) {
      text = featureLists.stream().map(FeatureList::getName).collect(Collectors.joining(", "));
    } else {
      text = featureLists.size() + " feature lists";
    }
    return text;
  }

  public static @NotNull String getRawDataFilesSubtitle(
      Collection<? extends RawDataFile> rawDataFiles) {
    final String text;
    if (rawDataFiles.size() <= 2) {
      text = rawDataFiles.stream().map(RawDataFile::getName).collect(Collectors.joining(", "));
    } else {
      text = rawDataFiles.size() + " MS data files";
    }
    return text;
  }

  private void updateTabText() {
    final String text = createTabText(
        ConfigService.getPreference(MZminePreferences.useTabSubtitles));
    setText(text);

    final boolean twoLines = text.contains("\n");
    if (twoLines && !getStyleClass().contains(TWO_LINE_STYLE_CLASS)) {
      getStyleClass().add(TWO_LINE_STYLE_CLASS);
    } else if (!twoLines) {
      getStyleClass().remove(TWO_LINE_STYLE_CLASS);
    }
  }

  /**
   * @param includeSubTitle add the sub title as a second line if it is not blank
   * @return the title and the sub title separated by a line break
   */
  private @NotNull String createTabText(final boolean includeSubTitle) {
    final String mainTitle = Objects.requireNonNullElse(title.get(), "");
    final String sub = subTitle.get();
    if (!includeSubTitle || StringUtils.isBlank(sub)) {
      return mainTitle;
    }
    return mainTitle + "\n" + sub;
  }

  /**
   * @return the title without the sub title
   */
  public String getTitle() {
    return title.get();
  }

  public void setTitle(String title) {
    this.title.set(title);
  }

  public void setSubTitle(String subTitle) {
    this.subTitle.set(subTitle);
  }

  public StringProperty titleProperty() {
    return title;
  }

  public StringProperty subTitleProperty() {
    return subTitle;
  }

  /**
   * Utility method to get the text of a regular {@link Tab} or a {@link MZmineTab}. For an
   * MZmineTab, it returns the main title.
   */
  public static String getText(Tab tab) {
    if (tab instanceof MZmineTab mt) {
      return mt.getTitle();
    }
    return tab.getText();
  }

  @NotNull
  public abstract Collection<? extends RawDataFile> getRawDataFiles();

  @NotNull
  public abstract Collection<? extends FeatureList> getFeatureLists();

  @NotNull
  public abstract Collection<? extends FeatureList> getAlignedFeatureLists();

  public abstract void onRawDataFileSelectionChanged(
      Collection<? extends RawDataFile> rawDataFiles);

  public abstract void onFeatureListSelectionChanged(
      Collection<? extends FeatureList> featureLists);

  public abstract void onAlignedFeatureListSelectionChanged(
      Collection<? extends FeatureList> featureLists);

  public boolean isUpdateOnSelection() {
    return cbUpdateOnSelection.isSelected();
  }

  public BooleanProperty updateOnSelectionProperty() {
    return cbUpdateOnSelection.selectedProperty();
  }

  public void setUpdateOnSelection(boolean updateOnSelection) {
    this.cbUpdateOnSelection.setSelected(updateOnSelection);
  }

  private void updateContextMenu() {
    MenuItem bind = new MenuItem("(Un)bind to selection");
    MenuItem openInNewWindow = new MenuItem("Open in new window");
    Menu moveToWindow = new Menu("Move to window...");
    MenuItem closeTab = new MenuItem("Close");

    bind.setOnAction(e -> {
      cbUpdateOnSelection.setSelected(!cbUpdateOnSelection.isSelected());
      e.consume();
    });

    closeTab.setOnAction(e -> {
      if (this.isClosable()) {
        getTabPane().getTabs().remove(this);
      }
      e.consume();
    });

    openInNewWindow.setOnAction(e -> {
      getTabPane().getTabs().remove(this);
      new MZmineWindow().addTab(this);
      e.consume();
    });

    MenuItem moveToMainWindow = new MenuItem("Main window");
    moveToMainWindow.setOnAction(e -> {
      if (MZmineCore.getDesktop().getTabsInMainWindow().size() < MZmineGUI.MAX_TABS) {
        getTabPane().getTabs().remove(this);
        MZmineCore.getDesktop().addTab(this);
        e.consume();
        return;
      }
      logger.info(
          "Maximum number of tabs in main window reached or tab cannot be moved. Cannot move tab to main window.");
      e.consume();
    });

    moveToWindow.getItems().add(moveToMainWindow);

    for (MZmineWindow window : MZmineCore.getDesktop().getWindows()) {
      MenuItem wi = new MenuItem(window.getTitle());

      if (window.isExclusive()) {
        wi.setDisable(true);
      }

      wi.setOnAction(e -> {
        if (window.getNumberOfTabs() >= MZmineGUI.MAX_TABS) {
          logger.info("Maximum number of tabs in " + window.getTitle()
              + " window reached or tab cannot be moved. Cannot move tab to main window.");
          e.consume();
          return;
        }
        getTabPane().getTabs().remove(this);
        window.addTab(this);
        e.consume();
      });
      moveToWindow.getItems().add(wi);
    }
    contextMenu.getItems().clear();
    contextMenu.getItems().addAll(bind, new SeparatorMenuItem(), openInNewWindow, moveToWindow,
        new SeparatorMenuItem(), closeTab);
  }
}
