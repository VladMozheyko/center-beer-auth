package fr.mossaab.security.controller;

import fr.mossaab.security.service.api.KindService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/kinds")
public class KindController {

    private final KindService kindService;

    // для фрагмента ListFragment
    @GetMapping(value = "/all")
    public ResponseEntity<Object> getAll(){
        Map<String, Object> response = new HashMap<>();

        response.put("data",  kindService.getByTypeId(1L));
        return ResponseEntity.ok(response);
    }
}
