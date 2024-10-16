package fr.mossaab.security.service;

import fr.mossaab.security.entities.ServiceCentre;
import fr.mossaab.security.repository.ServiceCentreRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class ServiceCentreService {
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
