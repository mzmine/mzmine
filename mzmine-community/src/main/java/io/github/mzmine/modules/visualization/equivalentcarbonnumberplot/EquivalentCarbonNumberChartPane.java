/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.visualization.equivalentcarbonnumberplot;

import io.github.mzmine.javafx.components.factories.FxTooltips;
import io.github.mzmine.javafx.util.FxColorUtil;
import io.github.mzmine.javafx.util.color.ColorScaleUtil;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.ILipidClass;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.lipidchain.LipidChainType;
import java.util.Arrays;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
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
      lbl.setTooltip(FxTooltips.newTooltip(tooltip));
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
