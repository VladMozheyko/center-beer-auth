package fr.mossaab.security.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "service_centres")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ServiceCenter {
    @Id
    @GeneratedValue
    private Long id;


    private String title;

    private String city;

    private String address;

    private String phone;

    private String servicedEquipment;

    private double latitude;

    private double longitude;

    @ManyToMany
    @JoinTable(
            name = "service_centre_series",
            joinColumns = @JoinColumn(name = "service_centre_id"),
            inverseJoinColumns = @JoinColumn(name = "series_id")
    )
    private List<Series> seriesList = new ArrayList<>();
}

