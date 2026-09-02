package com.thinkerscave.admission.dto.response;

import com.thinkerscave.academics.dto.response.LookupDTO;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PublicAdmissionsFormConfig {

    private Long defaultAcademicYearId;
    private List<LookupDTO> academicYears;
    private List<LookupDTO> classes;
}
