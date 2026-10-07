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

package io.github.mzmine.modules.visualization.networking.visual;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.types.numbers.NormalizedHeightType;
import io.github.mzmine.javafx.util.color.ColorsFX;
import io.github.mzmine.modules.visualization.networking.visual.enums.NodeAtt;
import io.github.mzmine.modules.visualization.projectmetadata.color.ColorByMetadataGroup;
import io.github.mzmine.modules.visualization.projectmetadata.color.ColorByMetadataResults;
import io.github.mzmine.modules.visualization.projectmetadata.color.ColorByMetadataUtils;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.util.GraphStreamUtils;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Computes and applies pie-chart styling to feature nodes of a feature network. Each feature node
 * is rendered as a pie where every slice is one sample group (defined by a metadata column) and the
 * slice size is the median feature intensity in that group. Intensities prefer
 * {@link NormalizedHeightType} and fall back to the raw {@code HeightType} when no normalized value
 * exists.
 *
 * <p>The static {@code node.PIE} style (shape, stroke, size mode) is defined in
 * {@code themes/graph_network_style.css}; this class injects the per-group fill-color list at
 * runtime via {@link #buildFillColorStyleRule()} and sets the per-node
 * {@link FeatureNetworkGenerator#PIE_VALUES_ATTR} and {@code ui.class}.
 */
public class NetworkPieChartStyler {

  private static final Logger logger = Logger.getLogger(NetworkPieChartStyler.class.getName());

  private final @NotNull FeatureList featureList;

  // groups for the currently selected column, or null if no/invalid column. Recomputed via
  // computeGroups whenever the column changes.
  private @Nullable List<ColorByMetadataGroup> groups;

  public NetworkPieChartStyler(final @NotNull FeatureList featureList) {
    this.featureList = featureList;
  }

  /**
   * Recompute the sample groups for the given metadata column. Colors follow the same categorical
   * palette as the rest of mzmine (see {@link ColorByMetadataUtils}).
   *
   * @param column the metadata column to group raw data files by, or null to clear
   * @return true if grouping yields at least 2 groups so pies are meaningful
   */
  public boolean computeGroups(final @Nullable MetadataColumn<?> column) {
    if (column == null) {
      groups = null;
      return false;
    }
    try {
      final ColorByMetadataResults result = ColorByMetadataUtils.colorByColumn(column,
          featureList.getRawDataFiles());
      groups = result.groups();
    } catch (Exception ex) {
      // e.g. column removed from metadata in the meantime
      logger.log(Level.WARNING, "Cannot group raw data files by column " + column.getTitle(), ex);
      groups = null;
    }
    return hasEnoughGroups();
  }

  /**
   * @return true if the current column produced at least 2 sample groups
   */
  public boolean hasEnoughGroups() {
    return groups != null && groups.size() >= 2;
  }

  public @Nullable List<ColorByMetadataGroup> getGroups() {
    return groups;
  }

  /**
   * Build a CSS rule that overrides the {@code node.PIE} fill-color list with one color per sample
   * group, so slice colors match the group colors used elsewhere.
   *
   * @return the rule, or an empty string when there are no groups
   */
  public @NotNull String buildFillColorStyleRule() {
    if (groups == null || groups.isEmpty()) {
      return "";
    }
    final String colors = groups.stream().map(g -> ColorsFX.toHexString(g.color()))
        .collect(Collectors.joining(", "));
    return "node.PIE { fill-color: " + colors + "; }";
  }

  /**
   * Tag every feature node (a node backed by a {@link FeatureListRow}) with its pie-slice fractions
   * and the {@link FeatureNetworkGenerator#PIE_UI_CLASS} ui.class. No-op if there are fewer than 2
   * groups.
   *
   * @param graph the graph whose nodes are styled (usually the displayed/filtered graph)
   */
  public void applyPies(final @NotNull Graph graph) {
    if (!hasEnoughGroups()) {
      return;
    }
    final List<ColorByMetadataGroup> currentGroups = groups;
    graph.nodes().forEach(node -> {
      final FeatureListRow row = getRow(node);
      if (row == null) {
        return;
      }
      final double[] pie = computePieValues(row, currentGroups);
      if (pie == null) {
        // no detected features for this row in any group - fall back to the default node styling.
        // restore the class too in case this node was a pie under a previously selected column.
        clearPie(node);
        return;
      }
      node.setAttribute(FeatureNetworkGenerator.PIE_VALUES_ATTR, pie);
      node.setAttribute("ui.class", FeatureNetworkGenerator.PIE_UI_CLASS);
    });
  }

  /**
   * Remove pie attributes and restore the original ui.class derived from the node type.
   *
   * @param graph the graph whose nodes are reset
   */
  public void clearPies(final @NotNull Graph graph) {
    graph.nodes().forEach(this::clearPie);
  }

  /**
   * Remove pie attributes from a single node and restore its original ui.class (only if we had
   * switched it to PIE).
   */
  private void clearPie(final @NotNull Node node) {
    node.removeAttribute(FeatureNetworkGenerator.PIE_VALUES_ATTR);
    // only restore nodes we actually switched to PIE; others keep their original class
    if (FeatureNetworkGenerator.PIE_UI_CLASS.equals(node.getAttribute("ui.class"))) {
      GraphStreamUtils.getUiClass(node).ifPresentOrElse(c -> node.setAttribute("ui.class", c),
          () -> node.removeAttribute("ui.class"));
    }
  }

  private @Nullable FeatureListRow getRow(final @NotNull Node node) {
    return (FeatureListRow) node.getAttribute(NodeAtt.ROW.toString());
  }

  /**
   * Pie fractions for a row: the median (normalized) height per group, normalized so the slices sum
   * to 1.
   *
   * @return the fractions in group order, or null if the row has no detected feature in any group
   */
  static @Nullable double[] computePieValues(final @NotNull FeatureListRow row,
      final @NotNull List<ColorByMetadataGroup> groups) {
    final double[] medians = new double[groups.size()];
    double total = 0;
    for (int i = 0; i < groups.size(); i++) {
      final double median = medianHeight(row, groups.get(i).files());
      medians[i] = median;
      total += median;
    }
    if (total <= 0) {
      return null;
    }
    for (int i = 0; i < medians.length; i++) {
      medians[i] /= total;
    }
    return medians;
  }

  private static double medianHeight(final @NotNull FeatureListRow row,
      final @NotNull List<RawDataFile> files) {
    final DoubleArrayList values = new DoubleArrayList(files.size());
    for (final RawDataFile raw : files) {
      final Feature feature = row.getFeature(raw);
      if (feature == null) {
        continue;
      }
      final Double height = heightOf(feature);
      // ignore missing / zero intensities so they do not drag the median down
      if (height == null || height <= 0) {
        continue;
      }
      values.add((double) height);
    }
    if (values.isEmpty()) {
      return 0;
    }
    values.sort(null);
    final int n = values.size();
    if (n % 2 == 1) {
      return values.getDouble(n / 2);
    }
    return (values.getDouble(n / 2 - 1) + values.getDouble(n / 2)) / 2.0;
  }

  // decision: prefer the normalized height; fall back to the raw height when no normalized value
  // exists for this feature.
  private static @Nullable Double heightOf(final @NotNull Feature feature) {
    if (feature instanceof ModularFeature mf) {
      final Float norm = mf.get(NormalizedHeightType.class);
      if (norm != null) {
        return (double) (float) norm;
      }
    }
    final Float height = feature.getHeight();
    return height == null ? null : (double) (float) height;
  }
}
