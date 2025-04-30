package fr.mossaab.security.dto.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GetAllUsersResponse {

    private String status;
    private String notify;
    private List<GetUsersDto> users;
    private int offset;
    private int pageNumber;
    private long totalElements;
    private int totalPages;
    private int pageSize;
    private boolean last;
    private boolean first;

}