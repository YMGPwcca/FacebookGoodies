package ymg.pwcca.facebookgoodies;

/** Only deliberate, identifier-free messages may be displayed to the user. */
final class DownloadFailure extends java.io.IOException {
    private static final long serialVersionUID = 1L;
    DownloadFailure(String message) { super(message); }
    DownloadFailure(String message, Throwable cause) { super(message, cause); }
}
