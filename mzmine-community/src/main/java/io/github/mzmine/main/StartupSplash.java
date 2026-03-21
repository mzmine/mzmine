/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.main;

import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.javafx.util.FxIconUtil;
import io.github.mzmine.util.io.SemverVersionReader;
import java.awt.GraphicsEnvironment;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A lightweight splash that can be shown before JavaFX is initialized.
 */
public final class StartupSplash {

  private static final Logger logger = Logger.getLogger(StartupSplash.class.getName());
  private static final Logger rootLogger = Logger.getLogger("");
  private static final String SPLASH_RESOURCE = "icons/introductiontab/logos_mzio_mzmine.png";
  private static final String MZIO_LOGO_RESOURCE = "splash/logo_mzio.png";
  private static final String COMMUNITY_LOGO_RESOURCE = "splash/mzmine_community.png";
  private static final int MAX_LOGO_WIDTH = 800;
  private static final int LOG_LABEL_HEIGHT = 18;
  private static final int BRAND_FONT_SIZE = 16;
  private static final int BRAND_LOGO_HEIGHT = 16;
  private static final double BRAND_TEXT_Y_OFFSET = 6.0;
  private static final int VERSION_FONT_SIZE = 24;
  private static final int VERSION_AND_LOGO_SPACE = 5;
  private static final AtomicReference<Stage> splashStage = new AtomicReference<>();
  private static final AtomicReference<Handler> splashLogHandler = new AtomicReference<>();
  private static final Color fontColor = Color.web("#3C3C3B");
  private static Insets OVERALL_PADDING = new Insets(20);

  private StartupSplash() {
  }

  public static void show() {
    if (GraphicsEnvironment.isHeadless() || splashStage.get() != null) {
      return;
    }
    logger.info("Showing startup splash screen");
    FxThread.initJavaFx();
    FxThread.runOnFxThreadAndWait(() -> {
      if (splashStage.get() != null) {
        return;
      }

      final Text logText = createLogText();
      final TextFlow logFlow = createLogFlow(logText);
      final Handler logHandler = createLogHandler(logText);
      try {
        final Stage stage = createSplashStage(logFlow);
        splashStage.set(stage);
        splashLogHandler.set(logHandler);
        rootLogger.addHandler(logHandler);
        stage.show();
        stage.toFront();
      } catch (Exception e) {
        splashStage.set(null);
        splashLogHandler.set(null);
        rootLogger.removeHandler(logHandler);
        logger.log(Level.WARNING, "Could not show startup splash", e);
      }
    }, true);
  }

  public static void hide() {
    if (!FxThread.isFxInitialized()) {
      return;
    }

    FxThread.runOnFxThreadAndWait(() -> {
      final Handler logHandler = splashLogHandler.getAndSet(null);
      if (logHandler != null) {
        rootLogger.removeHandler(logHandler);
      }

      final Stage stage = splashStage.getAndSet(null);
      if (stage == null) {
        return;
      }
      stage.hide();
    }, true);
  }

  @NotNull
  private static Stage createSplashStage(final @NotNull TextFlow logFlow) {
    final BorderPane logoPane = createLogoPane();
    final BorderPane footer = createFooter(logFlow);
    final BorderPane content = new BorderPane(logoPane);
    content.setBottom(footer);

    content.setPrefWidth(MAX_LOGO_WIDTH + OVERALL_PADDING.getLeft() + OVERALL_PADDING.getRight());
    content.setMinWidth(Region.USE_PREF_SIZE);
    content.setBackground(
        new Background(new BackgroundFill(Color.WHITE, new CornerRadii(15), Insets.EMPTY)));
    content.setPadding(OVERALL_PADDING);

    final Scene scene = new Scene(content);
    scene.setFill(Color.TRANSPARENT);

    final Stage stage = new Stage(StageStyle.TRANSPARENT);
    stage.setAlwaysOnTop(true);
    stage.setResizable(false);
    stage.setScene(scene);
    stage.sizeToScene();
    stage.centerOnScreen();
    stage.getIcons().add(FxIconUtil.loadImageFromResources("mzmineIcon.png"));

    return stage;
  }

  @NotNull
  private static ImageView createLogoView() {
    final ImageView logoView = new ImageView(FxIconUtil.loadImageFromResources(SPLASH_RESOURCE));
    logoView.setPreserveRatio(true);
    logoView.setSmooth(true);
    logoView.setFitWidth(MAX_LOGO_WIDTH);
    return logoView;
  }

  @NotNull
  private static BorderPane createLogoPane() {
    final Label versionLabel = createVersionLabel();
    final VBox brandingPane = new VBox(VERSION_AND_LOGO_SPACE, createLogoView(), versionLabel);
    brandingPane.setAlignment(Pos.CENTER);
    return new BorderPane(brandingPane);
  }

  @NotNull
  private static Label createVersionLabel() {
    final Label versionLabel = new Label(String.valueOf(SemverVersionReader.getMZmineVersion()));
    versionLabel.setTextFill(fontColor);
    versionLabel.setFont(
        Font.font(Font.getDefault().getFamily(), FontWeight.BOLD, VERSION_FONT_SIZE));
    return versionLabel;
  }

  @NotNull
  private static HBox createMzioBrand() {
    final Label byLabel = new Label("by");
    byLabel.setTextFill(fontColor);
    byLabel.setFont(Font.font(Font.getDefault().getFamily(), FontWeight.THIN, BRAND_FONT_SIZE));
    // decision: compensate for font bounds so the text visually aligns with the logo.
    byLabel.setTranslateY(BRAND_TEXT_Y_OFFSET);

    final ImageView mzioLogo = new ImageView(FxIconUtil.loadImageFromResources(MZIO_LOGO_RESOURCE));
    mzioLogo.setPreserveRatio(true);
    mzioLogo.setSmooth(true);
    mzioLogo.setFitHeight(BRAND_LOGO_HEIGHT);

    final HBox brand = new HBox(4, byLabel, mzioLogo);
    brand.setAlignment(Pos.BOTTOM_RIGHT);
    return brand;
  }

  @NotNull
  private static BorderPane createFooter(final @NotNull TextFlow logFlow) {
    final HBox mzioBrand = createMzioBrand();
    final HBox communityLogo = createCommunityLogoView();
    final BorderPane footer = new BorderPane();
    footer.setLeft(communityLogo);
    footer.setCenter(logFlow);
    footer.setRight(mzioBrand);
    BorderPane.setAlignment(communityLogo, Pos.BOTTOM_LEFT);
    BorderPane.setAlignment(mzioBrand, Pos.BOTTOM_RIGHT);
    BorderPane.setAlignment(logFlow, Pos.BOTTOM_LEFT);
    footer.setPadding(new Insets(5));
    communityLogo.setPadding(new Insets(0, 5, 0, 0));
    return footer;
  }

  @NotNull
  private static HBox createCommunityLogoView() {
    final ImageView communityLogo = new ImageView(
        FxIconUtil.loadImageFromResources(COMMUNITY_LOGO_RESOURCE));
    communityLogo.setPreserveRatio(true);
    communityLogo.setSmooth(true);
    communityLogo.setFitHeight(BRAND_LOGO_HEIGHT * 2);
    communityLogo.setTranslateY(5);
    HBox hBox = new HBox(communityLogo);
    hBox.setAlignment(Pos.BOTTOM_LEFT);
    return hBox;
  }

  @NotNull
  private static Text createLogText() {
    final Text logText = new Text("Initializing...");
    logText.setFill(fontColor);
    logText.setFont(new Font(Font.getDefault().getFamily(), 10));
    return logText;
  }

  @NotNull
  private static TextFlow createLogFlow(final @NotNull Text logText) {
    final TextFlow logFlow = new TextFlow(logText);
    logFlow.setTextAlignment(TextAlignment.LEFT);
    logFlow.setMinWidth(0);
    logFlow.setMinHeight(LOG_LABEL_HEIGHT);
    logFlow.setPrefHeight(LOG_LABEL_HEIGHT);
    logFlow.setMaxHeight(LOG_LABEL_HEIGHT);
    logFlow.setMaxWidth(Double.MAX_VALUE);
    return logFlow;
  }

  @NotNull
  private static Handler createLogHandler(final @NotNull Text logText) {
    final Handler handler = new Handler() {
      @Override
      public void publish(final @Nullable LogRecord record) {
        if (record == null || !isLoggable(record)) {
          return;
        }

        final String message = formatLogRecord(record);
        if (message == null) {
          return;
        }

        FxThread.runLaterEnsureFxInitialized(() -> {
          if (splashStage.get() != null) {
            logText.setText(message);
          }
        });
      }

      @Override
      public void flush() {
      }

      @Override
      public void close() {
      }
    };
    handler.setLevel(Level.ALL);
    return handler;
  }

  @Nullable
  private static String formatLogRecord(final @NotNull LogRecord record) {
    String message = record.getMessage();
    if (message == null) {
      return null;
    }
    return message.replace('\n', ' ').replace('\r', ' ').trim();
  }
}
