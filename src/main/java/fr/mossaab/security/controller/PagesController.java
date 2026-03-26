package fr.mossaab.security.controller;

import fr.mossaab.security.entities.User;
import fr.mossaab.security.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PagesController {

    private final UserRepository userRepository;

    public PagesController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/home")
    public String index() {
        return "index";
    }

    @GetMapping("/profile")
    public String profile(Authentication authentication, Model model) {

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            model.addAttribute("user", userDetails);
        }
        return "profile";
    }
}
