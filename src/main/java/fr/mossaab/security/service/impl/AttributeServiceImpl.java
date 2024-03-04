package fr.mossaab.security.service.impl;

import fr.mossaab.security.entities.Attribute;
import fr.mossaab.security.repository.AttributeRepository;
import fr.mossaab.security.service.AttributeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AttributeServiceImpl implements AttributeService {

    private AttributeRepository attributeRepository;

    @Autowired
    public void setAttributeRepository(AttributeRepository attributeRepository) {
        this.attributeRepository = attributeRepository;
    }

    @Override
    public void addAll(List<Attribute> attributes) {
        if(!attributes.isEmpty()){
            attributeRepository.saveAllAndFlush(attributes);
        }
    }
}
