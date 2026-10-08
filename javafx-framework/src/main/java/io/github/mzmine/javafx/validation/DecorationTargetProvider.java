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

package io.github.mzmine.javafx.validation;

import static java.util.Objects.requireNonNullElse;

import java.util.List;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Control;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Implemented by components that define which of their nodes decorations (validation icons,
 * checkmarks, ...) are attached to. Components often grow with the window, so decorating the whole
 * component would place the decoration far away from the actual inputs.
 *
 * @see DecorationTargetProvider#findDecorationTarget(Node)
 */
public interface DecorationTargetProvider {

  /**
   * Finds the node to attach decorations to within a component. Components often grow with the
   * window, so decorating the whole component would place the decoration at the window edge.
   * <ol>
   *   <li>{@link DecorationTargetProvider}s define their own target</li>
   *   <li>otherwise the last visible and managed {@link Control}, only searching through layout
   *   containers and not into controls</li>
   *   <li>otherwise the node itself</li>
   * </ol>
   * Based on the structure, not on the layout, so it can be called before the node is shown.
   *
   * @param node usually a parameter component
   * @return the decoration target
   */
  static @NotNull Node findDecorationTarget(@NotNull Node node) {
    return requireNonNullElse(findLastControl(node), node);
  }

  static @Nullable Node findLastControl(@NotNull Node node) {
    // provider is checked first as controls may also be providers
    if (node instanceof DecorationTargetProvider provider) {
      return provider.getDecorationTarget();
    }
    if (node instanceof Control) {
      return node;
    }
    if (!(node instanceof Parent parent)) {
      return null;
    }
    final List<Node> children = parent.getChildrenUnmodifiable();
    for (int i = children.size() - 1; i >= 0; i--) {
      final Node child = children.get(i);
      if (!child.isVisible() || !child.isManaged()) {
        continue;
      }
      final Node target = findLastControl(child);
      if (target != null) {
        return target;
      }
    }
    return null;
  }

  /**
   * @return the node that decorations are attached to, e.g., the main input field
   */
  @NotNull Node getDecorationTarget();
}
