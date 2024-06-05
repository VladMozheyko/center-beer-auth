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
    private int id; // пока что интовая айдишка сервисного центра, не думаю, что нужен лонг


    private String title;

    private String city;

    private String address;

    private String phone;

    private String servicedEquipment;

    public ServiceCentre(String title, String city, String address, String phone, String servicedEquipment) {
        this.title = title;
        this.city = city;
        this.address = address;
        this.phone = phone;
        this.servicedEquipment = servicedEquipment;
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
                '}';
    }
}

