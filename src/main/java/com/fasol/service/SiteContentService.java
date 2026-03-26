package com.fasol.service;

import com.fasol.domain.entity.SiteContent;
import java.util.List;

public interface SiteContentService {
    List<SiteContent> getActiveContent();
    SiteContent upsert(String sectionKey, String title, String description, String imageUrl, String content);
}
