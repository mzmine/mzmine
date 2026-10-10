package io.github.mzmine.gui.mainwindow;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import org.jetbrains.annotations.NotNull;

/** Application menu contributions, rebuilt with each workspace on the JavaFX thread. */
public final class ApplicationMenus {
  private static final Map<String, Supplier<Menu>> factories = new LinkedHashMap<>();
  private ApplicationMenus() {}

  public static synchronized void register(final @NotNull String id,
      final @NotNull Supplier<Menu> factory) {
    factories.put(id, factory);
  }

  public static synchronized void contribute(final @NotNull MenuBar bar) {
    final int help = java.util.stream.IntStream.range(0, bar.getMenus().size())
        .filter(i -> "Help".equals(bar.getMenus().get(i).getText().replace("_", "")))
        .findFirst().orElse(bar.getMenus().size());
    bar.getMenus().addAll(help, factories.values().stream().map(Supplier::get).toList());
  }
}
