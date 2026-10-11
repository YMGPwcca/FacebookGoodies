package ymg.pwcca.facebookgoodies;

/** Immutable snapshot of one active video; never persisted or indexed by ID. */
final class DownloadMedia {
    final String id;
    final String manifest;
    final String hd;
    final String sd;
    DownloadMedia(String id, String manifest, String hd, String sd) {
        this.id = id; this.manifest = manifest; this.hd = hd; this.sd = sd;
    }
    boolean hasSource() { return present(manifest) || present(hd) || present(sd); }
    static boolean present(String value) { return value != null && !value.trim().isEmpty(); }
}
