package fr.mossaab.security.payload.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GetUsersDto {
    private String firstname;
    private String email;
    private String lastname;
    private String phoneNumber;

    private String workerRole;

    private String dateOfBirth;

    private String photo;
    private Boolean activationCode;
    private String role;
    private String id;
}
