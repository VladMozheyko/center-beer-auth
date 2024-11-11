//package fr.mossaab.security.service;
//
//
//
//
//import fr.mossaab.security.entities.Type;
//import fr.mossaab.security.repository.TypeRepository;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//
//@Service
//public class TypeService {
//
//    private TypeRepository typeRepository;
//
//    @Autowired
//    public void setTypeRepository(TypeRepository typeRepository) {
//        this.typeRepository = typeRepository;
//    }
//
//    public void add(Type type) {
//        if(type != null){
//            typeRepository.saveAndFlush(type);
//        }
//    }
//
//    public void addAll(List<Type> types) {
//        if(!types.isEmpty()){
//            typeRepository.saveAllAndFlush(types);
//        }
//    }
//
//    public List<Type> getAll() {return typeRepository.findAll();}
//
//    public Long count() {
//        return typeRepository.count();
//    }
//
//    public void deleteAll() {
//        typeRepository.deleteAll();
//    }
//}