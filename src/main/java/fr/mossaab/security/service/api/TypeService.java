package fr.mossaab.security.service.api;


import fr.mossaab.security.entities.Type;

import java.util.List;

public interface TypeService {

    List<Type> getAll();

    void add(Type type);

    void addAll(List<Type> types);

    Long count();

    void deleteAll();
}
