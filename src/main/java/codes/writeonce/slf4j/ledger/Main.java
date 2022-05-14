package codes.writeonce.slf4j.ledger;

import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class Main {

    public static void main(String[] args) {
        MDC.put("foo", "bar");
        LoggerFactory.getLogger(Main.class).info("Hello!");
    }
}
