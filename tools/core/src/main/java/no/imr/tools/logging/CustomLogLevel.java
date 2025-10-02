package no.imr.tools.logging;

import java.util.logging.Level;

/**
 * Custom log level class, since {@link Level#Level(String, int)} is protected.
 * <p>
 * Note that the classloader leaks in Level.KnownLevel was fixed in Java 9.
 * See <a href="https://bugs.java.com/view_bug.do?bug_id=6543126">https://bugs.java.com/view_bug.do?bug_id=6543126</a>
 */
final class CustomLogLevel extends Level {
   CustomLogLevel(String name, int value) {
      super(name, value);
   }
}
