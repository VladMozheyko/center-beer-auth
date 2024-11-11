package fr.mossaab.security.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "error")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Error {
    @Id
    @GeneratedValue
    private Long id;

    private String code;
    private String cause;
    private String description;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "series_id", nullable = true)
    private Series series;
}
