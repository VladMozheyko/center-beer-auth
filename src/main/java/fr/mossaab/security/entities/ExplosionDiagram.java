package fr.mossaab.security.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "EXPLOSION_DIAGRAM")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ExplosionDiagram {

    /**
     * Уникальный идентификатор взрывной схемы.
     */
    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private String name;
    /**
     * Список запчастей, относящихся к данной взрывной схеме.
     */
    @OneToMany(mappedBy = "explosionDiagram", cascade = CascadeType.ALL)
    private List<SparePart> spareParts;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "file_data_id", referencedColumnName = "id")
    private FileData fileData;
}