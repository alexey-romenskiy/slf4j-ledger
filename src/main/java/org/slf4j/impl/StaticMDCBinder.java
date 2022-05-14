package org.slf4j.impl;

import codes.writeonce.slf4j.ledger.MDCAdapterImpl;
import org.slf4j.spi.MDCAdapter;

public class StaticMDCBinder {

    /**
     * The unique instance of this class.
     */
    public static final StaticMDCBinder SINGLETON = new StaticMDCBinder();

    private StaticMDCBinder() {
        // empty
    }

    /**
     * Currently this method always returns an instance of
     * {@link StaticMDCBinder}.
     */
    public MDCAdapter getMDCA() {
        return new MDCAdapterImpl();
    }

    public String getMDCAdapterClassStr() {
        return MDCAdapterImpl.class.getName();
    }
}
