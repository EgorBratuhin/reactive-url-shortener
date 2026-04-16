package by.bratukhin.shortener.service;

///
/// The exception that is thrown when an object is not found.
///
public class ObjectNotFoundException extends RuntimeException {

    ///
    /// Constructor.
    ///
    /// @param message the detail message (must not be `null`)
    ///
    public ObjectNotFoundException(String message) {
        super(message);
    }

}
