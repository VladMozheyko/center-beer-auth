package fr.mossaab.security.entities;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.OneToOne;
import lombok.*;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Embeddable
public class ProposedChanges {
    @ElementCollection
    private Map<String, String> changes = new HashMap<>();

    @OneToOne
    private FileData proposedPhoto; // новое поле для хранения предложенной фотографии
}