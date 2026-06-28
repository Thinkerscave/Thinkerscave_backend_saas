package com.thinkerscave.platform.repository;

import com.thinkerscave.platform.entity.Feature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeatureRepository extends JpaRepository<Feature, Long> {

    boolean existsByFeatureCode(String featureCode);

    boolean existsByFeatureKey(String featureKey);

    boolean existsByFeatureCodeAndIdNot(String featureCode, Long id);

    boolean existsByFeatureKeyAndIdNot(String featureKey, Long id);

    Optional<Feature> findByFeatureCode(String featureCode);

    Optional<Feature> findByFeatureKey(String featureKey);

    List<Feature> findByActiveTrueOrderByModuleAscDisplayOrderAsc();

    List<Feature> findByModuleAndActiveTrueOrderByDisplayOrderAsc(String module);
}
