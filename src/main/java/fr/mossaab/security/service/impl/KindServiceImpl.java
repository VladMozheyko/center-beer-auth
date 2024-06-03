package fr.mossaab.security.service.impl;

import fr.mossaab.security.entities.Kind;
import fr.mossaab.security.repository.KindRepository;
import fr.mossaab.security.service.api.KindService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KindServiceImpl implements KindService {

    private KindRepository kindRepository;

    @Autowired
    public void setKindRepository(KindRepository kindRepository) {
        this.kindRepository = kindRepository;
    }

    @Override
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
