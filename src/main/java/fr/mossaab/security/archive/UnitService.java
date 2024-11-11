//package fr.mossaab.security.service;
//
//
//import fr.mossaab.security.entities.Unit;
//import fr.mossaab.security.repository.UnitRepository;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//
//@Service
//public class UnitService {
//
//    private UnitRepository unitRepository;
//
//    @Autowired
//    public void setUnitRepository(UnitRepository unitRepository) {this.unitRepository = unitRepository;}
//
//    public void addAll(List<Unit> units) {
//        if(!units.isEmpty()){
//            unitRepository.saveAllAndFlush(units);
//        }
//    }
//}
