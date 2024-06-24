package fr.mossaab.security.dto.response;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class ResponseGetProfile {
    private String status;
    private String notify;
    private AnswerGetProfile answer;
}