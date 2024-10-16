package fr.mossaab.security.archive;
/*
import fr.mossaab.security.entities.Kind;
import fr.mossaab.security.repository.KindRepository;
import fr.mossaab.security.service.KindService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class KindServiceImpl implements KindService {

    private final KindRepository kindRepository;

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
*/