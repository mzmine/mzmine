/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 */
package io.github.mzmine.localbridge;

import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.main.MZmineCore;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import org.jetbrains.annotations.NotNull;

/**
 * Parses and delivers opaque mzmine deep links. The application integration supplies the
 * destination-specific handler; the community layer deliberately contains no project or commercial
 * policy.
 */
public final class DeepLinkRouter {

  private static final Logger logger = Logger.getLogger(DeepLinkRouter.class.getName());
  private static final AtomicReference<DeepLinkHandler> handler = new AtomicReference<>();
  private static final int MAX_PENDING_LINKS = 32;
  private static final List<DeepLink> pending = new ArrayList<>();
  private static boolean malformedLinkPending;
  private static boolean guiReady;

  private DeepLinkRouter() {
  }

  /** Registers the application handler. Register before GUI startup to receive launch links. */
  public static void registerHandler(final @NotNull DeepLinkHandler newHandler) {
    handler.set(newHandler);
  }

  /** Parses a URL received from the OS and queues it until the GUI is available. */
  public static boolean queue(final @NotNull String value) {
    try {
      synchronized (pending) {
        if (pending.size() == MAX_PENDING_LINKS) {
          logger.warning("Ignoring mzmine deep link because the pending queue is full");
          return false;
        }
        pending.add(parse(value));
      }
      return true;
    } catch (IllegalArgumentException ex) {
      logger.log(Level.WARNING, "Ignoring malformed mzmine deep link", ex);
      synchronized (pending) {
        malformedLinkPending = true;
      }
      return false;
    }
  }

  /** Finds mzmine URLs in launcher arguments without interpreting any other command-line option. */
  public static void queueLaunchArguments(final @NotNull String[] arguments) {
    for (final String argument : arguments) {
      if (argument != null && argument.toLowerCase(Locale.ROOT).startsWith("mzmine:")) {
        queue(argument);
      }
    }
  }

  /** Removes only mzmine URLs before normal command-line parsing. */
  public static @NotNull String[] withoutDeepLinkArguments(final @NotNull String[] arguments) {
    return java.util.Arrays.stream(arguments)
        .filter(argument -> argument == null || !argument.toLowerCase(Locale.ROOT).startsWith("mzmine:"))
        .toArray(String[]::new);
  }

  /** Delivers all launch links after the GUI is initialized. */
  public static void dispatchPending() {
    synchronized (pending) {
      if (!guiReady) {
        return;
      }
    }
    final List<DeepLink> links;
    final boolean showMalformedMessage;
    synchronized (pending) {
      links = List.copyOf(pending);
      pending.clear();
      showMalformedMessage = malformedLinkPending;
      malformedLinkPending = false;
    }
    if (showMalformedMessage) {
      Platform.runLater(() -> MZmineCore.getDesktop().displayMessage("Cannot open mzmine link",
          "The link is malformed or no longer supported."));
    }
    links.forEach(DeepLinkRouter::navigate);
  }

  /**
   * Delivers a previously validated link on the FX thread. This is also the entry point for the
   * authenticated local bridge; callers must validate instance and project ownership themselves.
   *
   * @return false if no application handler is installed
   */
  public static boolean navigate(final @NotNull DeepLink deepLink) {
    synchronized (pending) {
      if (!guiReady) {
        if (pending.size() == MAX_PENDING_LINKS) {
          logger.warning("Ignoring mzmine deep link because the pending queue is full");
          return false;
        }
        pending.add(deepLink);
        return true;
      }
    }
    if (!FxThread.isFxInitialized()) {
      synchronized (pending) {
        if (pending.size() == MAX_PENDING_LINKS) {
          logger.warning("Ignoring mzmine deep link because the pending queue is full");
          return false;
        }
        pending.add(deepLink);
      }
      return true;
    }
    final DeepLinkHandler registered = handler.get();
    if (registered == null) {
      logger.warning("Ignoring mzmine deep link because no application handler is installed");
      return false;
    }
    final Runnable navigation = () -> {
      try {
        registered.navigate(deepLink);
      } catch (RuntimeException ex) {
        logger.log(Level.WARNING, "Could not navigate mzmine deep link", ex);
      }
    };
    if (Platform.isFxApplicationThread()) {
      navigation.run();
    } else {
      Platform.runLater(navigation);
    }
    return true;
  }

  /** Marks the application desktop as available for navigation and user-visible errors. */
  public static void markGuiReady() {
    synchronized (pending) {
      guiReady = true;
    }
  }

  /** Strictly parses mzmine://navigate/&lt;instance&gt;/&lt;project&gt;/&lt;destination&gt;[/&lt;id&gt;]. */
  public static @NotNull DeepLink parse(final @NotNull String value) {
    if (value.length() > 256) {
      throw new IllegalArgumentException("mzmine deep-link URI exceeds the maximum length");
    }
    final URI uri;
    try {
      uri = new URI(value);
    } catch (URISyntaxException ex) {
      throw new IllegalArgumentException("Invalid mzmine deep-link URI", ex);
    }
    if ("mzmine://app/connection".equals(value)) {
      return new DeepLink(null, null, DeepLink.Destination.APP_CONNECTION, Optional.empty());
    }
    if (!"mzmine".equalsIgnoreCase(uri.getScheme()) || !"navigate".equals(uri.getHost())
        || uri.getRawQuery() != null || uri.getRawFragment() != null || uri.getUserInfo() != null
        || uri.getPort() != -1) {
      throw new IllegalArgumentException("Unsupported mzmine deep-link URI");
    }
    final String[] segments = uri.getRawPath().split("/", -1);
    if (segments.length < 4 || !segments[0].isEmpty() || segments.length > 5
        || (segments.length == 5 && segments[4].isEmpty())) {
      throw new IllegalArgumentException("Invalid mzmine deep-link path");
    }
    final UUID instanceId = parseUuid(segments[1], "instance");
    final UUID projectId = parseUuid(segments[2], "project");
    final DeepLink.Destination destination = DeepLink.Destination.fromPathSegment(segments[3]);
    final Optional<UUID> resourceId = segments.length == 5
        ? Optional.of(parseUuid(segments[4], "resource")) : Optional.empty();
    return new DeepLink(instanceId, projectId, destination, resourceId);
  }

  private static @NotNull UUID parseUuid(final @NotNull String value, final @NotNull String name) {
    try {
      final UUID parsed = UUID.fromString(value);
      if (!parsed.toString().equalsIgnoreCase(value)) {
        throw new IllegalArgumentException("Invalid " + name + " identifier");
      }
      return parsed;
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException("Invalid " + name + " identifier", ex);
    }
  }
}
