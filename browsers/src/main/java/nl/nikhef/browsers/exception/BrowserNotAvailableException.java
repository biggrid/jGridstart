package nl.nikhef.browsers.exception;

/**
 * Browser exception occuring when the requested browser is not present
 */
public class BrowserNotAvailableException extends BrowserException {
    public BrowserNotAvailableException(String browser) {
        super(browser, "Browser not available");
    }
}
