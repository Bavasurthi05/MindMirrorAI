package com.project.mentalhealth.interfaces.api.v1.socialimport;

import com.project.mentalhealth.application.service.SocialImportService;
import com.project.mentalhealth.interfaces.api.v1.common.ApiResponse;
import com.project.mentalhealth.interfaces.api.v1.socialimport.dto.SocialImportResponse;
import com.project.mentalhealth.interfaces.api.v1.socialimport.dto.SocialInsightsResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Upload a social media data export, follow its analysis, and remove it again.
 *
 * <p>Replaces live account connection, which the platforms' API terms rule out for
 * mental-health analysis.
 */
@RestController
@RequestMapping("${app.api.base-path}/social-imports")
public class SocialImportController {

    private final SocialImportService socialImportService;

    public SocialImportController(SocialImportService socialImportService) {
        this.socialImportService = socialImportService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<SocialImportResponse> upload(Authentication authentication,
                                                    @RequestParam String provider,
                                                    @RequestPart("file") MultipartFile file,
                                                    @RequestParam(defaultValue = "false") boolean acknowledged) {
        return ApiResponse.success(
                socialImportService.importExport(authentication.getName(), provider, file, acknowledged),
                "Upload received — analyzing your posts");
    }

    @GetMapping
    public ApiResponse<List<SocialImportResponse>> list(Authentication authentication) {
        return ApiResponse.success(socialImportService.list(authentication.getName()));
    }

    @GetMapping("/insights")
    public ApiResponse<SocialInsightsResponse> insights(Authentication authentication) {
        return ApiResponse.success(socialImportService.insights(authentication.getName()));
    }

    @GetMapping("/{id}")
    public ApiResponse<SocialImportResponse> get(Authentication authentication, @PathVariable Long id) {
        return ApiResponse.success(socialImportService.get(authentication.getName(), id));
    }

    /** Deletes the import, its posts and their analyses. */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(Authentication authentication, @PathVariable Long id) {
        socialImportService.delete(authentication.getName(), id);
        return ApiResponse.success(null, "Import and its analysis deleted");
    }
}
