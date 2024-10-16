package fr.mossaab.security.controller;

import fr.mossaab.security.entities.ServiceCentre;
import fr.mossaab.security.service.ServiceCentreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/service-centres")
public class ServiceCentreController {

    @Autowired
    private ServiceCentreService serviceCentreService;
    /**
     * GET-метод для получения списка ServiceCentre.
     *
     * @return Ответ со списком ServiceCentre статусом 200 OK.
     */
    @GetMapping("/getAll")
    public List<ServiceCentre> getAllServiceCentres() {
        return serviceCentreService.getAllServiceCentres();
    }
    /**
     * POST-метод для сохранени списка ServiceCentre.
     *
     * @return Ответ после добавления со статусом 200 OK.
     */
    @PostMapping("/add")
    public void addServiceCentre(@RequestBody ServiceCentre serviceCentres) {
        serviceCentreService.addServiceCentre(serviceCentres);
    }

    /**
     * DELETE-метод для удаления ServiceCentre по заголовку.
     *
     * @return Ответ после удаления со статусом 200 OK.
     */
    @DeleteMapping("/{id}")
    public void deleteById (@PathVariable("id") int id){
        serviceCentreService.deleteServiceCentreById(id);
        System.out.println("Deleted successfully ");
    }


}

