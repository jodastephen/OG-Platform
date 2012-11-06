/**
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.util.log;

import org.apache.log4j.Level;

import com.opengamma.util.ArgumentChecker;

import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * Implementation of {@link LogEvent} which handles logs sent through the Logback logging framework.
 */
public class LogbackLogEvent implements LogEvent {

  private final ILoggingEvent _loggingEvent;
  
  public LogbackLogEvent(ILoggingEvent loggingEvent) {
    ArgumentChecker.notNull(loggingEvent, "loggingEvent");
    _loggingEvent = loggingEvent;
  }
  
  //-------------------------------------------------------------------------
  @Override
  public LogLevel getLevel() {
    switch (getLoggingEvent().getLevel().toInt()) {
      case Level.FATAL_INT:
        return LogLevel.FATAL;
      case Level.ERROR_INT:
        return LogLevel.ERROR;
      case Level.WARN_INT:
        return LogLevel.WARN;
      case Level.INFO_INT:
        return LogLevel.INFO;
      case Level.DEBUG_INT:
        return LogLevel.DEBUG;
      case Level.TRACE_INT:
        return LogLevel.TRACE;
      default:
        return LogLevel.WARN;
    }
  }

  @Override
  public String getMessage() {
    return getLoggingEvent().getFormattedMessage();
  }
  
  //-------------------------------------------------------------------------
  private ILoggingEvent getLoggingEvent() {
    return _loggingEvent;
  }

}
