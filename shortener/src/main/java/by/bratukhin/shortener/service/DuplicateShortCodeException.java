package by.bratukhin.shortener.service;

///
/// The exception that is thrown when a short code is already taken.
///
public class DuplicateShortCodeException extends RuntimeException {

    ///
    /// Constructor.
    ///
    /// @param shortCode the duplicate short code; must not be null
    ///
    public DuplicateShortCodeException(String shortCode) {
        super("Short code '%s' is already in use".formatted(shortCode));
    }

}
