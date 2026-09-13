package com.thinkerscave.academics.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LookupDTO {
    private Long id;
    private String name;
    /** Optional academic-year status (e.g. CURRENT) when returned from year lookup. */
    private String status;

    public LookupDTO(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public LookupDTO(Long id, String name, String status) {
        this.id = id;
        this.name = name;
        this.status = status;
    }
}
