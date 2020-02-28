package org.modmappings.crispycomputingmachine.utils;

import org.apache.logging.log4j.LogManager;
import reactor.util.Logger;

public class ApacheLogger implements Logger {

    private static Logger getFor(final Class<?> clz)
    {
        return new ApacheLogger(LogManager.getLogger(clz));
    }

    private final org.apache.logging.log4j.Logger apacheLogger;

    public ApacheLogger(final org.apache.logging.log4j.Logger apacheLogger) {
        this.apacheLogger = apacheLogger;
    }

    @Override
    public String getName() {
        return apacheLogger.getName();
    }

    @Override
    public boolean isTraceEnabled() {
        return apacheLogger.isTraceEnabled();
    }

    @Override
    public void trace(final String msg) {
        apacheLogger.trace(msg);
    }

    @Override
    public void trace(final String format, final Object... arguments) {
        apacheLogger.trace(format, arguments);
    }

    @Override
    public void trace(final String msg, final Throwable t) {
        apacheLogger.trace(msg, t);
    }

    @Override
    public boolean isDebugEnabled() {
        return apacheLogger.isDebugEnabled();
    }

    @Override
    public void debug(final String msg) {
        apacheLogger.debug(msg);
    }

    @Override
    public void debug(final String format, final Object... arguments) {
        apacheLogger.debug(format, arguments);
    }

    @Override
    public void debug(final String msg, final Throwable t) {
        apacheLogger.debug(msg, t);
    }

    @Override
    public boolean isInfoEnabled() {
        return apacheLogger.isInfoEnabled();
    }

    @Override
    public void info(final String msg) {
        apacheLogger.info(msg);
    }

    @Override
    public void info(final String format, final Object... arguments) {
        apacheLogger.info(format, arguments);
    }

    @Override
    public void info(final String msg, final Throwable t) {
        apacheLogger.info(msg, t);
    }

    @Override
    public boolean isWarnEnabled() {
        return apacheLogger.isWarnEnabled();
    }

    @Override
    public void warn(final String msg) {
        apacheLogger.warn(msg);
    }

    @Override
    public void warn(final String format, final Object... arguments) {
        apacheLogger.warn(format, arguments);
    }

    @Override
    public void warn(final String msg, final Throwable t) {
        apacheLogger.warn(msg, t);
    }

    @Override
    public boolean isErrorEnabled() {
        return apacheLogger.isErrorEnabled();
    }

    @Override
    public void error(final String msg) {
        apacheLogger.error(msg);
    }

    @Override
    public void error(final String format, final Object... arguments) {
        apacheLogger.error(format, arguments);
    }

    @Override
    public void error(final String msg, final Throwable t) {
        apacheLogger.error(msg, t);
    }
}
