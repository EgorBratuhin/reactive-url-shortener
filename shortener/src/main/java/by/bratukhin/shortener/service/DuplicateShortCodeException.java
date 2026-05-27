package by.bratukhin.shortener.service;

///
/// The exception that is thrown when a short code is already taken.
///
public class DuplicateShortCodeException extends RuntimeException {

    ///
    /// Constructor.
    ///
    /// @param shortCode the duplicate short code; must not be null
    /// @param cause     the cause
    ///
    public DuplicateShortCodeException(String shortCode, Exception cause) {
        super("Short code '%s' is already in use".formatted(shortCode), cause);
    }

}
