package io.github.mzmine.modules.visualization.equivalentcarbonnumberplot;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.ILipidClass;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.lipidchain.LipidChainType;
import io.github.mzmine.util.color.ColorScaleUtil;
import io.github.mzmine.util.javafx.FxColorUtil;
import java.util.Arrays;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

public class EquivalentCarbonNumberChartPane extends BorderPane {

  public static Color MAX_COS_COLOR = Color.web("0x388E3C");
  public static Color MIN_COS_COLOR = Color.web("0xE30B0B");
  public static final double MIN_COS_COLOR_VALUE = 0.55;
  public static final double MAX_COS_COLOR_VALUE = 1.0;


  public EquivalentCarbonNumberChartPane(EquivalentCarbonNumberChart equivalentCarbonNumberChart,
      int numberOfDbes, List<MatchedLipid> matchedLipids) {
    setCenter(equivalentCarbonNumberChart);
    setTop(createTitlePane(numberOfDbes, equivalentCarbonNumberChart.getR2(), matchedLipids));
  }

  private Pane createTitlePane(int numberOfDes, double r2, List<MatchedLipid> matchedLipids) {
    String styleWhiteScoreSmall = "white-score-label-small";
    var ecnModelInfo = new VBox(0, createLabel("R2", styleWhiteScoreSmall),
        createLabel(matchedLipids.size() + " Lipids", styleWhiteScoreSmall));
    ecnModelInfo.setAlignment(Pos.CENTER_RIGHT);
    Color gradientCol = FxColorUtil.awtColorToFX(
        ColorScaleUtil.getColor(FxColorUtil.fxColorToAWT(MIN_COS_COLOR),
            FxColorUtil.fxColorToAWT(MAX_COS_COLOR), MIN_COS_COLOR_VALUE, MAX_COS_COLOR_VALUE, r2));
    Label lblHit = createLabel(
        getFullAbbr(matchedLipids.stream().findFirst().get().getLipidAnnotation().getLipidClass())
            + ": ECN model for DBE: " + numberOfDes, "white-larger-label");
    Label lblScore = createLabel(MZmineCore.getConfiguration().getScoreFormat().format(r2),
        "white-score-label");
    var scoreBox = new HBox(5, ecnModelInfo, lblScore);
    scoreBox.setPadding(new Insets(0, 5, 0, 10));
    scoreBox.setAlignment(Pos.CENTER);
    var titlePane = new BorderPane(lblHit);
    titlePane.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
    titlePane.setRight(scoreBox);
    titlePane.setPadding(new Insets(2));
    titlePane.setStyle("-fx-background-color: " + FxColorUtil.colorToHex(gradientCol));
    return titlePane;
  }

  private Label createLabel(final String label, final String styleClass) {
    return createLabel(label, null, styleClass);
  }

  private Label createLabel(final String label, String tooltip, final String styleClass) {
    Label lbl = new Label(label);
    lbl.getStyleClass().add(styleClass);
    if (tooltip != null) {
      lbl.setTooltip(new Tooltip(tooltip));
    }
    return lbl;
  }

  private static String getFullAbbr(ILipidClass lipidClass) {
    if (Arrays.asList(lipidClass.getChainTypes()).contains(LipidChainType.ALKYL_CHAIN)) {
      return lipidClass.getAbbr() + " " + "O";
    } else {
      return lipidClass.getAbbr();
    }
  }
}
