package com.thinkerscave.common.orgm.repository;


import com.thinkerscave.common.orgm.domain.OwnerDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OwnerDetailsRepository extends JpaRepository<OwnerDetails,Long> {

}
