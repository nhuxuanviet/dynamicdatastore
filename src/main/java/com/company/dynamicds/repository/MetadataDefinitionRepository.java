package com.company.dynamicds.repository;

import com.company.dynamicds.dynamicds.entity.MetadataDefinition;
import io.jmix.core.repository.JmixDataRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MetadataDefinitionRepository extends JmixDataRepository<MetadataDefinition, UUID> {
    Optional<MetadataDefinition> findByNameAndStoreName(String entityName, String dataStoreName);
}