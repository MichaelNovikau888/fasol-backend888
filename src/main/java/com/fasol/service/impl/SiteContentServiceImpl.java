package com.fasol.service.impl;

import com.fasol.domain.entity.SiteContent;
import com.fasol.repository.SiteContentRepository;
import com.fasol.service.SiteContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SiteContentServiceImpl implements SiteContentService {

    private final SiteContentRepository repository;

    @Override
    @Transactional(readOnly = true)
    public List<SiteContent> getActiveContent() {
        return repository.findByActiveTrue();
    }

    @Override
    @Transactional
    public SiteContent upsert(String sectionKey, String title, String description,
                               String imageUrl, String content) {
        SiteContent sc = repository.findBySectionKey(sectionKey)
                .orElse(SiteContent.builder().sectionKey(sectionKey).build());
        sc.setTitle(title);
        sc.setDescription(description);
        sc.setImageUrl(imageUrl);
        if (content != null) sc.setContent(content);
        return repository.save(sc);
    }
}
