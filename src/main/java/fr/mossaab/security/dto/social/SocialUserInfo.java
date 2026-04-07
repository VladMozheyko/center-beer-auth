package fr.mossaab.security.dto.social;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SocialUserInfo {
    private String id;
    private String email;
    private String firstName;
    private String lastName;
}