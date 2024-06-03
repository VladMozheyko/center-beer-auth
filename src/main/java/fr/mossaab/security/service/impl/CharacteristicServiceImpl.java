package fr.mossaab.security.service.impl;

import fr.mossaab.security.entities.Characteristic;
import fr.mossaab.security.repository.CharacteristicRepository;
import fr.mossaab.security.service.api.CharacteristicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CharacteristicServiceImpl implements CharacteristicService {

    private CharacteristicRepository characteristicRepository;

    @Autowired
    public void setCharacteristicRepository(CharacteristicRepository characteristicRepository) {
        this.characteristicRepository = characteristicRepository;
    }

    @Override
    public void addAll(List<Characteristic> characteristics) {
        if(!characteristics.isEmpty()){
           characteristicRepository.saveAllAndFlush(characteristics);
        }
    }
}
