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

package io.github.mzmine.parameters.parametertypes.ionidentity;

import io.github.mzmine.datamodel.identities.iontype.IonPartFrequency;
import io.github.mzmine.datamodel.identities.iontype.IonTypeRanking;
import io.github.mzmine.javafx.components.factories.FxButtons;
import io.github.mzmine.javafx.util.FxIcons;
import io.github.mzmine.parameters.ParameterComponent;
import java.util.List;
import java.util.Optional;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Shows the {@link IonTypeRanking} as a single button that opens
 * {@link IonPartRankingSetupDialog}.
 */
public class IonTypeRankingComponent extends VBox implements ParameterComponent<IonTypeRanking> {

  private final @NotNull ObjectProperty<@NotNull IonTypeRanking> ranking = new SimpleObjectProperty<>(
      IonTypeRanking.createDefault());

  public IonTypeRankingComponent(final @Nullable IonTypeRanking initial) {
    super(2);
    setPadding(new Insets(2));
    setValue(initial);

    getChildren().add(FxButtons.createButton("Edit ranking", FxIcons.EDIT,
        "Define the frequency of each ion building block, for both polarities and neutral modifications",
        this::edit));
  }

  private void edit() {
    final IonTypeRanking current = ranking.get();
    final IonPartRankingSetupDialog dialog = new IonPartRankingSetupDialog(
        current.getFrequencies());
    final Optional<List<IonPartFrequency>> result = dialog.showAndWait();
    result.ifPresent(entries -> ranking.set(current.withFrequencies(entries)));
  }

  @Override
  public @NotNull IonTypeRanking getValue() {
    return ranking.get();
  }

  @Override
  public void setValue(final @Nullable IonTypeRanking value) {
    ranking.set(value != null ? value : IonTypeRanking.createDefault());
  }
}
