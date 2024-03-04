package fr.mossaab.security.service;


import fr.mossaab.security.entities.Kind;

import java.util.List;

public interface KindService {

    void addAll(List<Kind> kinds);

    List<Kind> getAll();

    List<Kind> getByTypeId(Long id);
}
