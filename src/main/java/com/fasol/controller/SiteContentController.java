package com.fasol.controller;

import com.fasol.domain.entity.SiteContent;
import com.fasol.service.SiteContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/site-content")
@RequiredArgsConstructor
public class SiteContentController {

    private final SiteContentService siteContentService;

    @GetMapping
    public ResponseEntity<List<SiteContent>> getActive() {
        return ResponseEntity.ok(siteContentService.getActiveContent());
    }
}
