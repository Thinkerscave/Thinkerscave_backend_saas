package com.thinkerscave.staff.service;

import com.thinkerscave.staff.dto.request.ResponsibilityRequest;
import com.thinkerscave.staff.dto.response.ResponsibilityResponse;

import java.util.List;

public interface ResponsibilityService {

    Long createResponsibility(ResponsibilityRequest request);

    void updateResponsibility(Long id, ResponsibilityRequest request);

    List<ResponsibilityResponse> getResponsibilityList(boolean includeInactive);

    ResponsibilityResponse getResponsibilityById(Long id);

    void activateResponsibility(Long id);

    void deactivateResponsibility(Long id);
}
