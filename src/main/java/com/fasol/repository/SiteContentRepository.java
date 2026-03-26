package com.fasol.repository;

import com.fasol.domain.entity.SiteContent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SiteContentRepository extends JpaRepository<SiteContent, UUID> {

    List<SiteContent> findByActiveTrue();

    Optional<SiteContent> findBySectionKey(String sectionKey);
}
