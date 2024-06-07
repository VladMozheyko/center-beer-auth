package fr.mossaab.security.service.impl;

import fr.mossaab.security.entities.ServiceCentre;
import fr.mossaab.security.repository.ServiceCentreRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ServiceCentreService {
    @Autowired
    private ServiceCentreRepository serviceCentreRepository;

    public List<ServiceCentre> getAllServiceCentres(){
        return serviceCentreRepository.findAll();
    }

    public void addServiceCentre(ServiceCentre serviceCentres){
        serviceCentreRepository.save(serviceCentres);
    }

    public void addAllServiceCentre(List<ServiceCentre> serviceCentres){
        serviceCentreRepository.saveAll(serviceCentres);
    }
    public void deleteServiceCentreById(int id) {
        serviceCentreRepository.deleteById(id);
    }
}
