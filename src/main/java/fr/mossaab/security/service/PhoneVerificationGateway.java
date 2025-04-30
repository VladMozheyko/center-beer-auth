package fr.mossaab.security.service;

public interface PhoneVerificationGateway {
    /** @return собственно код, который надо сохранить в User */
    String sendCode(String phone);
}