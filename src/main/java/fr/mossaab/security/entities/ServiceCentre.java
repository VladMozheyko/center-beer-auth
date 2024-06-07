package fr.mossaab.security.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "service_centres")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ServiceCentre {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;


    private String title;

    private String city;

    private String address;

    private String phone;

    private String servicedEquipment;

    private double latitude;

    private double longitude;

    public ServiceCentre(String title, String city, String address, String phone, String servicedEquipment, double latitude, double longitude) {
        this.title = title;
        this.city = city;
        this.address = address;
        this.phone = phone;
        this.servicedEquipment = servicedEquipment;
        this.latitude = latitude;
        this.longitude = longitude;

    }

    @Override
    public String toString() {
        return "ServiceCentre{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", city='" + city + '\'' +
                ", address='" + address + '\'' +
                ", phone='" + phone + '\'' +
                ", servicedEquipment='" + servicedEquipment + '\'' +
                ", latitude='" + latitude + '\'' +
                ", longitude='" + longitude + '\'' +
                '}';
    }
}

