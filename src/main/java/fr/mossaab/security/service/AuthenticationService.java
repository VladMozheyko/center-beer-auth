package fr.mossaab.security.service;

import fr.mossaab.security.payload.request.AuthenticationRequest;
import fr.mossaab.security.payload.request.RegisterRequest;
import fr.mossaab.security.payload.response.AuthenticationResponse;

import java.text.ParseException;

public interface AuthenticationService {
    AuthenticationResponse register(RegisterRequest request) throws ParseException;
    AuthenticationResponse authenticate(AuthenticationRequest request);

    boolean activateUser(String code);
}
