/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

import io.github.mzmine.datamodel.features.FeatureListRow;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.graphstream.graph.Node;
import org.jetbrains.annotations.NotNull;

/**
 * Searches for parts of annotations in all annotations
 */
public class NodeAnnotationsFilter {


  private final FeatureNetworkPane network;
  private boolean computed = false;
  private final List<AnnotationEntry> entries = new ArrayList<>();

  public NodeAnnotationsFilter(final FeatureNetworkPane network) {
    this.network = network;
  }

  /**
   * Get all annotations and ready them for search
   */
  public void compute() {
    if (computed) {
      return;
    }
    computed = true;
    var featureList = network.getFeatureList();
    for (final FeatureListRow row : featureList.getRows()) {
      // just connect all strings with a sequence that is not likely in the search string
      var annotations = row.getAllFeatureAnnotations().stream().filter(Objects::nonNull)
          .map(Object::toString).filter(s -> !s.isBlank())
          .map(NodeAnnotationsFilter::harmonizeString).collect(Collectors.joining("^#"));
      var node = network.getNode(row);
      if (annotations.isBlank()) {
        continue;
      }
      entries.add(new AnnotationEntry(node, annotations));
    }
  }

  @NotNull
  private static String harmonizeString(final String s) {
    return s.toLowerCase().trim();
  }

  /**
   * @param annotationFilter filter part
   * @return all nodes that contain the filter string (case-insensitive)
   */
  public List<Node> findNodes(final String annotationFilter) {
    if (!computed) {
      compute();
    }
    final String part = harmonizeString(annotationFilter);
    return entries.stream().filter(e -> e.concatAnnotations.contains(part))
        .map(AnnotationEntry::node).toList();
  }

  private record AnnotationEntry(Node node, String concatAnnotations) {

  }
}

