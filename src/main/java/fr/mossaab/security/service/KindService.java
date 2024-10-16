package fr.mossaab.security.service;


import fr.mossaab.security.entities.Kind;
import fr.mossaab.security.repository.KindRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class KindService {

    private final KindRepository kindRepository;

    public void addAll(List<Kind> kinds) {
        if (!kinds.isEmpty()) {
            kindRepository.saveAllAndFlush(kinds);
        }
    }

    public List<Kind> getAll() {
        return kindRepository.findAll();
    }

    public List<Kind> getByTypeId(Long id) {
        return kindRepository.getKindByTypeId(id);
    }

}