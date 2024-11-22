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

package io.github.mzmine.modules.visualization.lipidannotationsummary;

import eu.hansolo.fx.charts.SunburstChart;
import eu.hansolo.fx.charts.SunburstChartBuilder;
import eu.hansolo.fx.charts.data.ChartItem;
import eu.hansolo.fx.charts.data.TreeNode;
import eu.hansolo.fx.charts.tools.TextOrientation;
import eu.hansolo.fx.charts.tools.VisibleData;
import io.github.mzmine.gui.chartbasics.chartthemes.EStandardChartTheme;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.ILipidAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.ILipidClass;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidCategories;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidMainClasses;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.lipidchain.LipidChainType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javafx.scene.paint.Color;

public class LipidAnnotationSunburstPlot extends SunburstChart {

  private final List<MatchedLipid> matchedLipids;
  private final boolean includeLipidCategory;
  private final boolean includeLipidMainClass;
  private final boolean includeLipidSubClass;
  private final boolean includeLipidSpecies;
  private TreeNode<ChartItem> treeNodeDataset;
  private final EStandardChartTheme theme;
  private SunburstChart sunburstChart;

  public LipidAnnotationSunburstPlot(List<MatchedLipid> matchedLipids, boolean includeLipidCategory,
      boolean includeLipidMainClass, boolean includeLipidSubClass, boolean includeLipidSpecies) {
    this.matchedLipids = matchedLipids;
    this.includeLipidCategory = includeLipidCategory;
    this.includeLipidMainClass = includeLipidMainClass;
    this.includeLipidSubClass = includeLipidSubClass;
    this.includeLipidSpecies = includeLipidSpecies;
    this.treeNodeDataset = buildTreeDataset();
    this.theme = MZmineCore.getConfiguration().getDefaultChartTheme();
    initTotalLipidIDSunburstPlot();
  }

  public SunburstChart getSunburstChart() {
    return sunburstChart;
  }

  private void initTotalLipidIDSunburstPlot() {
    TreeNode<ChartItem> tree = buildTreeDataset();
    sunburstChart = SunburstChartBuilder.create().prefSize(400, 400).tree(tree)
        .textOrientation(TextOrientation.TANGENT).autoTextColor(true).visibleData(VisibleData.NAME)
        .backgroundColor(Color.TRANSPARENT).textColor(Color.WHITE).decimals(0).interactive(true)
        .build();
  }

  private TreeNode<ChartItem> buildTreeDataset() {
    treeNodeDataset = new TreeNode<>(new ChartItem("root"));
    Map<LipidCategories, Map<LipidMainClasses, Map<ILipidClass, List<ILipidAnnotation>>>> lipidCategoryToMainClassMap = new HashMap<>();

    // Map annotation hierarchy
    for (MatchedLipid matchedLipid : matchedLipids) {
      ILipidAnnotation lipidAnnotation = matchedLipid.getLipidAnnotation();
      LipidCategories lipidCategory = lipidAnnotation.getLipidClass().getMainClass()
          .getLipidCategory();
      LipidMainClasses lipidMainClass = lipidAnnotation.getLipidClass().getMainClass();
      ILipidClass lipidClass = lipidAnnotation.getLipidClass();
      lipidCategoryToMainClassMap.computeIfAbsent(lipidCategory, k -> new HashMap<>())
          .computeIfAbsent(lipidMainClass, k -> new HashMap<>())
          .computeIfAbsent(lipidClass, k -> new ArrayList<>()).add(lipidAnnotation);
    }

    int colorIndex = 1;
    for (Entry<LipidCategories, Map<LipidMainClasses, Map<ILipidClass, List<ILipidAnnotation>>>> entry : lipidCategoryToMainClassMap.entrySet()) {
      int categoryValue = getCategoryLipidAnnotationCount(entry);
      Color categoryColor = ConfigService.getConfiguration().getDefaultColorPalette()
          .get(colorIndex);
      TreeNode lipidCategoryTreeNode = includeLipidCategory ? new TreeNode(
          new ChartItem(categoryValue + "\n" + entry.getKey().getAbbreviation(), categoryValue,
              categoryColor), treeNodeDataset) : null;

      for (Map.Entry<LipidMainClasses, Map<ILipidClass, List<ILipidAnnotation>>> mainClassEntry : entry.getValue()
          .entrySet()) {
        Color mainClassColor = categoryColor.desaturate();
        int mainClassValue = getMainClassLipidAnnotationCount(mainClassEntry);
        TreeNode lipidMainClassTreeNode =
            includeLipidCategory && includeLipidMainClass ? new TreeNode(
                new ChartItem(mainClassValue + "\n" + mainClassEntry.getKey().getName(),
                    mainClassValue, mainClassColor), lipidCategoryTreeNode)
                : (includeLipidMainClass ? new TreeNode(
                    new ChartItem(mainClassValue + "\n" + mainClassEntry.getKey().getName(),
                        mainClassValue, mainClassColor), treeNodeDataset) : null);

        for (Map.Entry<ILipidClass, List<ILipidAnnotation>> lipidClassEntry : mainClassEntry.getValue()
            .entrySet()) {
          Color lipidClassColor = mainClassColor.desaturate();
          int lipidClassValue = getLipidClassLipidAnnotationCount(lipidClassEntry);
          TreeNode lipidClassTreeNode =
              includeLipidCategory && includeLipidMainClass && includeLipidSubClass ? new TreeNode(
                  new ChartItem(
                      lipidClassValue + "\n" + getFormatLipidClassAbbreviation(lipidClassEntry),
                      lipidClassValue, lipidClassColor), lipidMainClassTreeNode)
                  : (includeLipidCategory && includeLipidSubClass ? new TreeNode(new ChartItem(
                      lipidClassValue + "\n" + getFormatLipidClassAbbreviation(lipidClassEntry),
                      lipidClassValue, lipidClassColor), lipidCategoryTreeNode)
                      : (includeLipidMainClass && includeLipidSubClass ? new TreeNode(new ChartItem(
                          lipidClassValue + "\n" + getFormatLipidClassAbbreviation(lipidClassEntry),
                          lipidClassValue, lipidClassColor), lipidMainClassTreeNode) : null));

          if (includeLipidCategory && includeLipidMainClass && includeLipidSubClass
              && includeLipidSpecies) {
            for (ILipidAnnotation lipidAnnotation : lipidClassEntry.getValue()) {
              Color lipidAnnotationColor = lipidClassColor.desaturate();
              new TreeNode(new ChartItem(lipidAnnotation.getAnnotation(), 1, lipidAnnotationColor),
                  lipidClassTreeNode);
            }
          } else if (includeLipidCategory && includeLipidMainClass && includeLipidSpecies) {
            for (ILipidAnnotation lipidAnnotation : lipidClassEntry.getValue()) {
              Color lipidAnnotationColor = lipidClassColor.desaturate();
              new TreeNode(new ChartItem(lipidAnnotation.getAnnotation(), 1, lipidAnnotationColor),
                  lipidMainClassTreeNode);
            }
          } else if (includeLipidCategory && includeLipidSubClass && includeLipidSpecies) {
            for (ILipidAnnotation lipidAnnotation : lipidClassEntry.getValue()) {
              Color lipidAnnotationColor = lipidClassColor.desaturate();
              new TreeNode(new ChartItem(lipidAnnotation.getAnnotation(), 1, lipidAnnotationColor),
                  lipidClassTreeNode);
            }
          } else if (includeLipidCategory && includeLipidSpecies) {
            for (ILipidAnnotation lipidAnnotation : lipidClassEntry.getValue()) {
              Color lipidAnnotationColor = lipidClassColor.desaturate();
              new TreeNode(new ChartItem(lipidAnnotation.getAnnotation(), 1, lipidAnnotationColor),
                  lipidCategoryTreeNode);
            }
          } else if (includeLipidSpecies) {
            for (ILipidAnnotation lipidAnnotation : lipidClassEntry.getValue()) {
              Color lipidAnnotationColor = lipidClassColor.desaturate();
              new TreeNode(new ChartItem(lipidAnnotation.getAnnotation(), 1, lipidAnnotationColor),
                  treeNodeDataset);
            }
          }
        }
      }
      colorIndex++;
    }
    return treeNodeDataset;
  }

  private int getCategoryLipidAnnotationCount(
      Entry<LipidCategories, Map<LipidMainClasses, Map<ILipidClass, List<ILipidAnnotation>>>> entry) {
    int totalCount = 0;
    for (Map.Entry<LipidMainClasses, Map<ILipidClass, List<ILipidAnnotation>>> mainClassEntry : entry.getValue()
        .entrySet()) {
      for (Map.Entry<ILipidClass, List<ILipidAnnotation>> classEntry : mainClassEntry.getValue()
          .entrySet()) {
        totalCount += classEntry.getValue().size();
      }
    }
    return totalCount;
  }

  private int getMainClassLipidAnnotationCount(
      Entry<LipidMainClasses, Map<ILipidClass, List<ILipidAnnotation>>> mainClassEntry) {
    int totalCount = 0;
    for (Map.Entry<ILipidClass, List<ILipidAnnotation>> classEntry : mainClassEntry.getValue()
        .entrySet()) {
      totalCount += classEntry.getValue().size();
    }
    return totalCount;
  }

  private int getLipidClassLipidAnnotationCount(
      Entry<ILipidClass, List<ILipidAnnotation>> lipidClassEntry) {
    return lipidClassEntry.getValue().size();
  }

  private String getFormatLipidClassAbbreviation(
      Entry<ILipidClass, List<ILipidAnnotation>> lipidClassEntry) {
    ILipidClass lipidClass = lipidClassEntry.getKey();
    String annotation;
    boolean hasAlkylChain = false;

    for (LipidChainType type : lipidClass.getChainTypes()) {
      if (type.equals(LipidChainType.ALKYL_CHAIN)) {
        hasAlkylChain = true;
        break;
      }

    }
    if (hasAlkylChain) {
      annotation = lipidClass.getAbbr() + " O";
    } else {
      annotation = lipidClass.getAbbr();
    }

    return annotation;
  }

}
