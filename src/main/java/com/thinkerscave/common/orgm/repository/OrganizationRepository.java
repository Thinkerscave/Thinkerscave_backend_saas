package com.thinkerscave.common.orgm.repository;


import com.thinkerscave.common.orgm.domain.Organisation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrganizationRepository extends JpaRepository<Organisation,Long> {
}
