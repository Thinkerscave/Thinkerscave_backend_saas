package com.thinkerscave.common.orgm.service;


import com.thinkerscave.common.orgm.dto.*;

import java.util.List;

/**
 * Service interface for managing organizations.
 * Provides methods for create/update, fetch, and soft-delete operations.
 *
 * @author Sandeep
 */
public interface OrganizationService {

    /**
     * Saves or updates organization details.
     */
    OrgResponseDTO saveOrganization(OrgRequestDTO request, String schema);

    /**
     * Returns a list of all organizations.
     */
//    List<Organisation> getAllOrgs();
    List<OrganisationListDTO> getAllOrgsAsDTO();

    /**
     * Performs a soft delete on an organization by org code.
     */
    String softDeleteOrg(String orgCode);

    /**
     * Updates owner details using owner code.
     */
    void updateOwnerDetailsWithUser(OwnerDTO dto);

    /**
     * update organization details
     **/
    OrgResponseDTO updateOrganization(Long orgId, OrgUpdateDTO dto);

    /**
     * get only parent organization list
     **/
    List<ParentOrgDTO> getParentOrganizations();
}
