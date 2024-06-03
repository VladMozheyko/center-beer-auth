package fr.mossaab.security.controller;

import fr.mossaab.security.mapper.TypeListMapper;
import fr.mossaab.security.service.api.TypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/types")
public class TypeController {

  private final TypeService typeService;
  private final TypeListMapper typeListMapper;

    // для ItemsFragment
    @GetMapping(value = "/all")
    public ResponseEntity<Object> getAll(){
      Map<String, Object> response = new HashMap<>();

      response.put("data", typeListMapper.toDTOList(typeService.getAll()));
      return ResponseEntity.ok(response);
    }

  @GetMapping(value = "/all-data")
  public ResponseEntity<Object> getAllData(){
    Map<String, Object> response = new HashMap<>();

    response.put("data", typeService.getAll());
    return ResponseEntity.ok(response);
  }
}
