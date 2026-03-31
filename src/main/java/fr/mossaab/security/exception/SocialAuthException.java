package fr.mossaab.security.exception;

import lombok.Getter;

@Getter
public class SocialAuthException extends RuntimeException {
    private final int httpStatus;

    public SocialAuthException(String message, int httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

}
