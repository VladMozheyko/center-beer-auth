//package fr.mossaab.security.service;
//
//
//import fr.mossaab.security.entities.Attribute;
//import fr.mossaab.security.repository.AttributeRepository;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//
//@Service
//public class AttributeService {
//
//    private AttributeRepository attributeRepository;
//
//    @Autowired
//    public void setAttributeRepository(AttributeRepository attributeRepository) {
//        this.attributeRepository = attributeRepository;
//    }
//
//    public void addAll(List<Attribute> attributes) {
//        if(!attributes.isEmpty()){
//            attributeRepository.saveAllAndFlush(attributes);
//        }
//    }
//}
