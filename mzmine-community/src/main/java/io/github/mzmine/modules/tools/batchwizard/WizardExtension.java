/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 */
package io.github.mzmine.modules.tools.batchwizard;

import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardStepParameters;
import javafx.scene.Node;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A per-tab contribution supplied by the application at startup. The wizard creates one extension
 * per tab and invokes every method on the JavaFX application thread.
 */
public interface WizardExtension {

  /**
   * Creates optional controls below the wizard toolbar once while the tab is built. Return null
   * for a contribution that only needs part-specific content.
   */
  @Nullable Node createControls();

  /**
   * Creates optional context below one part's parameters. This is called again whenever wizard
   * panes are rebuilt, so implementations must not reuse a Node already attached to a parent.
   */
  default @Nullable Node createPartContent(final @NotNull WizardStepParameters step) {
    return null;
  }

  /** Releases subscriptions and cancels outstanding work when the owning wizard tab closes. */
  default void close() {
  }
}
