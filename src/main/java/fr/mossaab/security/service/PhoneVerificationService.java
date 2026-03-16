package fr.mossaab.security.service;

import fr.mossaab.security.enums.RegistrationMethod;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class PhoneVerificationService {

    private final Map<String, PhoneVerificationGateway> gateways; // соберётся по @Service("...")

    public String sendCode(String phone, RegistrationMethod method) {
        return gateways.get(method.name()).sendCode(phone);
    }
}