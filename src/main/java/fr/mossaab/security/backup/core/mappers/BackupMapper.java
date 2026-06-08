package fr.mossaab.security.backup.core.mappers;

public interface BackupMapper<E, D> {
    D toDto(E entity);
    E fromDto(D dto);
}
