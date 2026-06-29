package fr.mossaab.security.dto.user;

import fr.mossaab.security.dto.FileDataDto;
import fr.mossaab.security.dto.social.SocialAccountResponse;
import lombok.*;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class AllUserInfoResponse {

    private String userName;
    private String email;
    private String phone;
    private List<SocialAccountResponse> socialAccounts;
    private FileDataDto fileData;

}
