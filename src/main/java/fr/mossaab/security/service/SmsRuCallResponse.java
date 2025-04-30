package fr.mossaab.security.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SmsRuCallResponse {
    private String status;
    @JsonProperty("status_text")
    private String statusText;
    private String code;
}
