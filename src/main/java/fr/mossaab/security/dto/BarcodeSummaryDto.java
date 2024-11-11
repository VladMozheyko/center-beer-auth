package fr.mossaab.security.dto;

import lombok.Data;

@Data
public  class BarcodeSummaryDto {
    private Long id;
    private Long code;
    private boolean used;

    public BarcodeSummaryDto(Long id, Long code, boolean used) {
        this.id = id;
        this.code = code;
        this.used = used;
    }
}