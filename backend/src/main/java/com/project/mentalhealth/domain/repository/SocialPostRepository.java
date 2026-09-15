package com.project.mentalhealth.domain.repository;

import com.project.mentalhealth.domain.model.SocialPost;
import com.project.mentalhealth.domain.model.SocialProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SocialPostRepository extends JpaRepository<SocialPost, Long> {

    /** Every post id this user already has from a provider, for de-duplicating re-uploads. */
    @Query("select p.externalPostId from SocialPost p where p.user.id = :userId and p.provider = :provider")
    List<String> findExternalPostIds(@Param("userId") Long userId, @Param("provider") SocialProvider provider);

    List<SocialPost> findBySocialImportIdAndAnalysisResultIdIsNullOrderByIdAsc(Long importId);

    List<SocialPost> findBySocialImportId(Long importId);

    List<SocialPost> findByUserIdOrderByPostedAtDesc(Long userId);

    long countBySocialImportIdAndAnalysisResultIdIsNull(Long importId);

    /** Analysis outcomes for one import's posts, as (status, count) pairs. */
    @Query("select a.status, count(a) from SocialPost p, AnalysisResult a "
            + "where a.id = p.analysisResultId and p.socialImport.id = :importId group by a.status")
    List<Object[]> countAnalysisStatuses(@Param("importId") Long importId);
}
