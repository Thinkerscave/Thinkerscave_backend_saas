package com.thinkerscave.common.orgm.service;


import com.thinkerscave.common.orgm.domain.Organisation;
import com.thinkerscave.common.orgm.dto.OrgRequestDTO;
import com.thinkerscave.common.orgm.dto.OrgResponseDTO;
import com.thinkerscave.common.orgm.dto.OrganisationListDTO;
import com.thinkerscave.common.orgm.dto.OwnerDTO;

import java.util.List;

/**
 * Service interface for managing organizations.
 * Provides methods for create/update, fetch, and soft-delete operations.
 *
 * @author Sandeep
 */
public interface OrganizationService {

    /** Saves or updates organization details. */
    OrgResponseDTO saveOrganization(OrgRequestDTO request);

    /** Returns a list of all organizations. */
//    List<Organisation> getAllOrgs();
     List<OrganisationListDTO> getAllOrgsAsDTO() ;
    /** Performs a soft delete on an organization by org code. */
    String softDeleteOrg(String orgCode);

    /** Updates owner details using owner code. */
    void updateOwnerDetailsWithUser(OwnerDTO dto);
}
