package fr.mossaab.security.backup.core.enums;

public enum BackupFileExtension {
    JSON(".json"),
    ZIP(".zip"),
    GZ(".gz");

    private final String extension;

    BackupFileExtension(String extension) {
        this.extension = extension;
    }

    public String getExtension() {
        return extension;
    }
}

