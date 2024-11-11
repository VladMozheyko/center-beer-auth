//package fr.mossaab.security.service;
//
//
//import fr.mossaab.security.entities.Advantage;
//import fr.mossaab.security.repository.AdvantageRepository;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//
//@Service
//public class AdvantageService  {
//
//    private AdvantageRepository advantageRepository;
//
//    @Autowired
//    public void setAdvantageRepository(AdvantageRepository advantageRepository) {
//        this.advantageRepository = advantageRepository;
//    }
//    public void addAll(List<Advantage> advantages) {
//        if(!advantages.isEmpty()){
//            advantageRepository.saveAllAndFlush(advantages);
//        }
//    }
//}
//
