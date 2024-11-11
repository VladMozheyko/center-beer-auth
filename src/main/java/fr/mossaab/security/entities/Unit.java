package fr.mossaab.security.entities;

import lombok.*;

import jakarta.persistence.*;

@Entity
@Table(name="units")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Unit {

    @Id
    @GeneratedValue
    private Long id;

    private String shortName;
    private String longName;
}
