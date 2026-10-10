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

/** A per-tab contribution supplied by the application at startup. All methods run on the FX thread. */
public interface WizardExtension {

  /** Optional controls below the wizard toolbar; null supports context-only integrations. */
  @Nullable Node createControls();

  /** Optional context below a part's parameters. Called again whenever the panes are rebuilt. */
  default @Nullable Node createPartContent(final @NotNull WizardStepParameters step) {
    return null;
  }

  /** Release subscriptions and cancel outstanding work when the tab closes. */
  default void close() {
  }
}
