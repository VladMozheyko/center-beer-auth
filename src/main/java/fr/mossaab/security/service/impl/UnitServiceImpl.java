package fr.mossaab.security.service.impl;

import fr.mossaab.security.entities.Unit;
import fr.mossaab.security.repository.UnitRepository;
import fr.mossaab.security.service.api.UnitService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UnitServiceImpl implements UnitService {

    private UnitRepository unitRepository;

    @Autowired
    public void setUnitRepository(UnitRepository unitRepository) {this.unitRepository = unitRepository;}

    @Override
    public void addAll(List<Unit> units) {
        if(!units.isEmpty()){
            unitRepository.saveAllAndFlush(units);
        }
    }
}
