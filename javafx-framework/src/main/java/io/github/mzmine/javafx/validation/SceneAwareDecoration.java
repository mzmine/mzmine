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

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.util.Subscription;
import org.controlsfx.control.decoration.Decoration;
import org.controlsfx.control.decoration.Decorator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Adds a decoration to a node once it is part of a scene. The {@link Decorator} replaces the scene
 * root with a decoration pane. If this happens while the scene is still setting its root, e.g.,
 * when a decoration is added before the node is shown, the CSS lookups of the scene break.
 * Therefore, the decoration is only added after the scene setup finished.
 * <p>
 * {@link #unsubscribe()} removes the decoration or cancels it if it was not added yet.
 */
class SceneAwareDecoration implements Subscription {

  private final @NotNull Node target;
  private final @NotNull Decoration decoration;
  private @Nullable Subscription sceneSubscription;
  private boolean added = false;
  private boolean removed = false;

  private SceneAwareDecoration(@NotNull Node target, @NotNull Decoration decoration) {
    this.target = target;
    this.decoration = decoration;
  }

  /**
   * @return subscription to remove the decoration
   */
  static @NotNull Subscription add(@NotNull Node target, @NotNull Decoration decoration) {
    final SceneAwareDecoration sceneAware = new SceneAwareDecoration(target, decoration);
    sceneAware.install();
    return sceneAware;
  }

  private void install() {
    if (target.getScene() != null) {
      apply();
      return;
    }
    // subscribe is called directly with the current null scene
    sceneSubscription = target.sceneProperty().subscribe(scene -> {
      if (scene == null) {
        return;
      }
      unsubscribeScene();
      // the scene may still be setting up its root
      Platform.runLater(this::apply);
    });
  }

  private void apply() {
    if (removed || added) {
      return;
    }
    Decorator.addDecoration(target, decoration);
    added = true;
  }

  private void unsubscribeScene() {
    if (sceneSubscription != null) {
      sceneSubscription.unsubscribe();
      sceneSubscription = null;
    }
  }

  @Override
  public void unsubscribe() {
    removed = true;
    unsubscribeScene();
    if (added) {
      Decorator.removeDecoration(target, decoration);
      added = false;
    }
  }
}
