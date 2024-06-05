package fr.mossaab.security.controller;

import fr.mossaab.security.entities.ServiceCentre;
import fr.mossaab.security.service.impl.ServiceCentreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/service-centres")
public class ServiceCentreController {

    @Autowired
    private ServiceCentreService serviceCentreService;

    @GetMapping("/getAll")
    public List<ServiceCentre> getAllServiceCentres() {
        return serviceCentreService.getAllServiceCentres();
    }

    @PostMapping("/add")
    public List<ServiceCentre> addServiceCentre(@RequestBody List<ServiceCentre> serviceCentres) {
        return serviceCentreService.addServiceCentre(serviceCentres);
    }


    @DeleteMapping("/{title}")
    public void deleteByName (@PathVariable("title") String title){
        serviceCentreService.deleteServiceCentreByTitle(title);
        System.out.println("Deleted successfully ");
    }


}

