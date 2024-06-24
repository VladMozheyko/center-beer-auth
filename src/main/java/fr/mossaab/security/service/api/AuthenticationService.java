package fr.mossaab.security.service.api;

import fr.mossaab.security.dto.request.AuthenticationRequest;
import fr.mossaab.security.dto.request.RegisterRequest;
import fr.mossaab.security.dto.response.AuthenticationResponse;
import fr.mossaab.security.entities.User;

import java.text.ParseException;

public interface AuthenticationService {
    AuthenticationResponse register(RegisterRequest request) throws ParseException;

    void  resendActivationCode(User user) throws ParseException;

    AuthenticationResponse authenticate(AuthenticationRequest request);

    boolean activateUser(String code);
}
