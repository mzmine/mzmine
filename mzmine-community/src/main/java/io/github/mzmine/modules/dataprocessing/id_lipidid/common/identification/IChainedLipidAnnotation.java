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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 */

package io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification;

import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.lipidchain.ILipidChain;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Common contract for lipid annotations whose individual chains have been resolved.
 * <p>
 * Molecular-species and double-bond-position annotations both expose chains. Consumers should use
 * this interface when they need chain composition and do not specifically depend on positional
 * localization.
 */
public sealed interface IChainedLipidAnnotation extends ILipidAnnotation permits
    MolecularSpeciesLevelAnnotation, DoubleBondPositionLevelAnnotation {

  /** Returns the resolved chains in the annotation; callers must treat the list as read-only. */
  @NotNull List<ILipidChain> getLipidChains();
}
