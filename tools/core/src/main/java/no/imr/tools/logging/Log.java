package no.imr.tools.logging;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Contains a reference, {@link #global}, to the global logger {@code Logger.global}.
 */
public final class Log {
   public static final String NO_MAREC_LOGGING_CONSOLE = "no.marec.logging.console";

   /**
    * Since Java 6 use of {@code Logger.global} is deprecated.
    * This field uses the preferred way to get the global logger.
    */
   public static final Logger global = Logger.getGlobal();

   public static final Level SILENT_WARNING = Boolean.parseBoolean(System.getProperty("no.marec.silentWarning"))
         ? new CustomLogLevel("SILENT_WARNING", Level.WARNING.intValue() - 1)
         : Level.WARNING;

   private static final ConsoleHandler CONSOLE_HANDLER = new ConsoleHandler();

   private static final List<Handler> HANDLERS = new CopyOnWriteArrayList<>();
   private static final Handler MAIN_HANDLER = new Handler() {
      @Override
      public void publish(LogRecord record) {
         if (isIgnorableLogging(record)) {
            return;
         }
         HANDLERS.forEach(handler -> handler.publish(record));
      }

      @Override
      public void flush() {
         HANDLERS.forEach(Handler::flush);
      }

      @Override
      public void close() {
         HANDLERS.forEach(Handler::close);
      }
   };

   private static LogRecord maxLevelLogRecord = new LogRecord(Level.ALL, "");
   private static final MaxLevelHandler MAX_LEVEL_HANDLER = new MaxLevelHandler() {
      @Override
      protected void newMaxLevel(LogRecord record) {
         // Don't need to do anything here
         maxLevelLogRecord = record;
      }
   };

   @SuppressWarnings("PMD.SystemPrintln")
   public static final Thread.UncaughtExceptionHandler UNCAUGHT_EXCEPTION_HANDLER = (t, e) -> {
      try {
         global.log(Level.SEVERE, "Uncaught exception in thread " + t, e);
      } catch (Throwable throwable) {
         // Propagating a throwable can cause the EventDispatchThread to stop.
         System.err.println("Error logging an uncaught exception in thread " + t);
         throwable.printStackTrace(System.err);
         throw throwable;
      }
   };

   static {
      System.setProperty("org.jboss.logging.provider", "jdk"); // See org.jboss.logging.LoggerProviders, used by Hibernate.

      initLogger(global, Level.ALL);
      initLogger(rootLogger(), Level.WARNING);

      addHandler(MAX_LEVEL_HANDLER);
      addHandler(CONSOLE_HANDLER);
      CONSOLE_HANDLER.setFormatter(new OneLineFormatter());
      CONSOLE_HANDLER.setLevel(Level.INFO);

      Thread.setDefaultUncaughtExceptionHandler(UNCAUGHT_EXCEPTION_HANDLER);
   }

   private Log() {
   }

   public static void init() {
      // Done by static initialization.
   }

   private static Logger rootLogger() {
      return Logger.getLogger("");
   }

   private static void initLogger(Logger logger, Level level) {
      logger.setLevel(level);
      logger.setUseParentHandlers(false);
      for (Handler handler : logger.getHandlers()) {
         logger.removeHandler(handler);
      }
      logger.addHandler(MAIN_HANDLER);
   }

   public static void stop() {
      global.removeHandler(MAIN_HANDLER);
      rootLogger().removeHandler(MAIN_HANDLER);
      Thread.setDefaultUncaughtExceptionHandler(null);
   }

   public static Level getMaxLevel() {
      return MAX_LEVEL_HANDLER.getMaxLevel();
   }

   public static LogRecord getMaxLevelLogRecord() {
      return maxLevelLogRecord;
   }

   public static void setConsoleLevel(Level level) {
      CONSOLE_HANDLER.setLevel(level);
   }

   public static void possiblyTurnOffConsoleLogging() {
      if (!Boolean.parseBoolean(System.getProperty(NO_MAREC_LOGGING_CONSOLE))) {
         setConsoleLevel(Level.OFF);
      }
   }

   public static void addHandler(Handler handler) {
      HANDLERS.add(handler);
   }

   static void removeHandler(Handler handler) {
      HANDLERS.remove(handler);
   }

   private static boolean isIgnorableLogging(LogRecord record) {
      if (record.getLoggerName() == null) {
         return false;
      }
      switch (record.getLevel().intValue()) {
         case 1000 -> { // ERROR
            if (record.getLoggerName().equals("org.glassfish.jersey.server.ServerRuntime$Responder")) {
               Throwable thrown = record.getThrown();
               Throwable cause = thrown != null ? getDeepestCause(thrown) : null;
               String message = cause != null ? cause.getMessage() : null;
               if (message != null) {
                  if (message.contains("connection was aborted")) {
                     record.setThrown(null);
                     record.setMessage(message);
                     record.setLevel(Level.INFO);
                     return false;
                  }
               }
            }
            if (record.getLoggerName().equals("org.hibernate.tool.hbm2ddl.SchemaExport")) {
               String message = record.getMessage();
               if (message != null) {
                  if (message.equals("Schema 'SA' does not exist")
                        || message.matches("user lacks privilege or object not found: PUBLIC\\.\\w+")
                        || message.matches("ERROR: relation \"\\w+\" does not exist")
                        || message.matches("'DROP TABLE' cannot be performed on '\\w+' because it does not exist.")
                        || message.matches("'ALTER TABLE' cannot be performed on '\\w+' because it does not exist.")
                        || message.matches("HHH000389: Unsuccessful: alter table \\w+ drop constraint \\w+")
                        || message.matches("HHH000389: Unsuccessful: drop table \\w+")) {
                     return true;
                  }
               }
            }
         }
         case 900 -> { // WARN
            if (record.getLoggerName().equals("org.hibernate.engine.jdbc.spi.SqlExceptionHelper")) {
               String message = record.getMessage();
               if (message != null) {
                  if (message.equals("SQL Warning Code: -1100, SQLState: 02000")
                        || message.equals("SQL Warning Code: 10000, SQLState: 02000")
                        || message.equals("No row was found for FETCH, UPDATE or DELETE; or the result of a query is an empty table.")
                        || message.equals("no data")
                        || message.equals("SQL Warning Code: 0, SQLState: 00000")
                        || message.matches("CREATE TABLE / PRIMARY KEY will create implicit index \"\\w+\" for table \"\\w+\"")) {
                     return true;
                  }
               }
            }
            if (record.getLoggerName().equals("org.hibernate.tool.schema.internal.ExceptionHandlerLoggedImpl")) {
               Throwable thrown = record.getThrown();
               Throwable cause = thrown != null ? thrown.getCause() : null;
               String message = cause != null ? cause.getMessage() : null;
               if (message != null) {
                  if (message.equals("Schema 'SA' does not exist")
                        || message.equals("SQL Warning Code: 10000, SQLState: 02000")
                        || message.equals("No row was found for FETCH, UPDATE or DELETE; or the result of a query is an empty table.")
                        || message.equals("no data")
                        || message.matches("user lacks privilege or object not found: PUBLIC.\\w+")
                        || message.matches("'ALTER TABLE' cannot be performed on '\\w+' because it does not exist.")
                        || message.matches("'DROP TABLE' cannot be performed on '\\w+' because it does not exist.")
                        || message.matches("GDS Exception. 335544351. unsuccessful metadata update\nTable \\w+ already exists\nnull\nnull")) {
                     return true;
                  }
               }
            }
         }
         default -> {
         }
      }

      return false;
   }

   private static Throwable getDeepestCause(Throwable throwable) {
      for (int i = 0; i < 100; i++) {
         Throwable cause = throwable.getCause();
         if (cause == null || cause == throwable) {
            break;
         }
         throwable = cause;
      }
      return throwable;
   }
}
