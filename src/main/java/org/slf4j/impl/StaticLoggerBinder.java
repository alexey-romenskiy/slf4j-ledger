package org.slf4j.impl;

import codes.writeonce.slf4j.ledger.ILoggerFactoryImpl;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LoggerFactoryBinder;

/**
 * The binding of {@link LoggerFactory} class with an actual instance of
 * {@link ILoggerFactory} is performed using information returned by this class.
 */
public class StaticLoggerBinder implements LoggerFactoryBinder {

    private static final StaticLoggerBinder INSTANCE = new StaticLoggerBinder();

    private final ILoggerFactoryImpl loggerFactory = new ILoggerFactoryImpl();

    private StaticLoggerBinder() {
        // empty
    }

    public static StaticLoggerBinder getSingleton() {
        return INSTANCE;
    }

    public ILoggerFactory getLoggerFactory() {
        return loggerFactory;
    }

    public String getLoggerFactoryClassStr() {
        return ILoggerFactoryImpl.class.getName();
    }
}
