package stryker4jvm.core.logging;

/** Indicates the severity or importance of a log message. */
public enum LogLevel {
  /** Log level developer messages. */
  Debug,
  /** Log level for support staff. Mostly progress and context updates. */
  Info,
  /** Log level for issues encountered that may invalidate the integrity of the program. */
  Warn,
  /** Log level for issues encountered that may result in a fatal crash of the program. */
  Error;
}
