package fr.mossaab.security.dto.social;

import fr.mossaab.security.enums.OAuthProvider;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class SocialAccountResponse {

    private OAuthProvider provider;
    private String socialEmail;
}
