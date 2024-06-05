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

    public List<ServiceCentre> addServiceCentre(List<ServiceCentre> serviceCentres){
        return new ArrayList<>(serviceCentreRepository.saveAll(serviceCentres));
    }

    public void deleteServiceCentreByTitle(String title) {
        serviceCentreRepository.deleteByTitle(title);
    }
}
