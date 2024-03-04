package fr.mossaab.security.service.impl;

import fr.mossaab.security.entities.Boiler;
import fr.mossaab.security.repository.BoilerRepository;
import fr.mossaab.security.service.BoilerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BoilerServiceImpl implements BoilerService {

    private BoilerRepository boilerRepository;

    @Autowired
    public void setBoilerRepository(BoilerRepository boilerRepository) {
        this.boilerRepository = boilerRepository;
    }

    @Override
    public void addAll(List<Boiler> boilers) {
        if(!boilers.isEmpty()){
            boilerRepository.saveAllAndFlush(boilers);
        }
    }
}
