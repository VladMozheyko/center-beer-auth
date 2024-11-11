//package fr.mossaab.security.controller;
//
//import fr.mossaab.security.entities.ServiceCenter;
//import fr.mossaab.security.service.ServiceCentreService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/service-centres")
//public class ServiceCentreController {
//
//    @Autowired
//    private ServiceCentreService serviceCentreService;
//    /**
//     * GET-метод для получения списка ServiceCenter.
//     *
//     * @return Ответ со списком ServiceCenter статусом 200 OK.
//     */
//    @GetMapping("/getAll")
//    public List<ServiceCenter> getAllServiceCentres() {
//        return serviceCentreService.getAllServiceCentres();
//    }
//    /**
//     * POST-метод для сохранени списка ServiceCenter.
//     *
//     * @return Ответ после добавления со статусом 200 OK.
//     */
//    @PostMapping("/add")
//    public void addServiceCentre(@RequestBody ServiceCenter serviceCenters) {
//        serviceCentreService.addServiceCentre(serviceCenters);
//    }
//
//    /**
//     * DELETE-метод для удаления ServiceCenter по заголовку.
//     *
//     * @return Ответ после удаления со статусом 200 OK.
//     */
//    @DeleteMapping("/{id}")
//    public void deleteById (@PathVariable("id") int id){
//        serviceCentreService.deleteServiceCentreById(id);
//        System.out.println("Deleted successfully ");
//    }
//
//
//}
//
