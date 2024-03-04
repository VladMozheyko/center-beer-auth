package fr.mossaab.security.service.impl;

import fr.mossaab.security.entities.Advantage;
import fr.mossaab.security.repository.AdvantageRepository;
import fr.mossaab.security.service.AdvantageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdvantageServiceImpl implements AdvantageService {

    private AdvantageRepository advantageRepository;

    @Autowired
    public void setAdvantageRepository(AdvantageRepository advantageRepository) {
        this.advantageRepository = advantageRepository;
    }

    @Override
    public void addAll(List<Advantage> advantages) {
        if(!advantages.isEmpty()){
            advantageRepository.saveAllAndFlush(advantages);
        }
    }
}
