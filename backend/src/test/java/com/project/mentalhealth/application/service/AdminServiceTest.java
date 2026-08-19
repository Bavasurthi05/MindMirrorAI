package com.project.mentalhealth.application.service;

import com.project.mentalhealth.application.ports.in.FeedbackUseCase;
import com.project.mentalhealth.application.ports.out.MlAnalysisPort;
import com.project.mentalhealth.domain.model.TriggerConfirmation;
import com.project.mentalhealth.domain.model.TriggerEntry;
import com.project.mentalhealth.domain.model.User;
import com.project.mentalhealth.domain.repository.AssessmentSubmissionRepository;
import com.project.mentalhealth.domain.repository.JournalEntryRepository;
import com.project.mentalhealth.domain.repository.MoodEntryRepository;
import com.project.mentalhealth.domain.repository.RecoveryActionRepository;
import com.project.mentalhealth.domain.repository.TriggerEntryRepository;
import com.project.mentalhealth.domain.repository.UserRepository;
import com.project.mentalhealth.interfaces.api.v1.admin.dto.AdminOverviewResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private JournalEntryRepository journalRepository;
    @Mock private MoodEntryRepository moodRepository;
    @Mock private AssessmentSubmissionRepository assessmentRepository;
    @Mock private TriggerEntryRepository triggerRepository;
    @Mock private RecoveryActionRepository recoveryRepository;
    @Mock private FeedbackUseCase feedbackUseCase;
    @Mock private MlAnalysisPort mlAnalysisPort;

    private AdminService service;

    @BeforeEach
    void setUp() {
        service = new AdminService(userRepository, journalRepository, moodRepository,
                assessmentRepository, triggerRepository, recoveryRepository, feedbackUseCase, mlAnalysisPort);
        given(userRepository.findAll()).willReturn(List.of());
        given(triggerRepository.findAll()).willReturn(List.of());
    }

    private User userJoined(int daysAgo) {
        User user = new User();
        user.setCreatedAt(Instant.now().minus(daysAgo, ChronoUnit.DAYS));
        return user;
    }

    private TriggerEntry trigger(String category, TriggerConfirmation confirmation) {
        TriggerEntry entry = new TriggerEntry();
        entry.setCategory(category);
        entry.setIntensity(5);
        entry.setOccurredAt(Instant.now());
        entry.setConfirmation(confirmation);
        return entry;
    }

    @Test
    void growthAlwaysCoversSixMonthsSoTheAxisIsEven() {
        AdminOverviewResponse response = service.overview();

        assertThat(response.getUserGrowth()).hasSize(6);
        assertThat(response.getUserGrowth()).allSatisfy(point -> {
            assertThat(point.getNewUsers()).isZero();
            assertThat(point.getLabel()).isNotBlank();
        });
    }

    @Test
    void growthLabelsRunOldestToNewestEndingThisMonth() {
        List<AdminOverviewResponse.GrowthPoint> growth = service.overview().getUserGrowth();

        String thisMonth = YearMonth.now(ZoneOffset.UTC)
                .format(java.time.format.DateTimeFormatter.ofPattern("MMM yyyy", java.util.Locale.ENGLISH));
        assertThat(growth.get(growth.size() - 1).getLabel()).isEqualTo(thisMonth);
    }

    @Test
    void thisMonthsSignupsAreCounted() {
        given(userRepository.findAll()).willReturn(List.of(userJoined(0), userJoined(1)));

        List<AdminOverviewResponse.GrowthPoint> growth = service.overview().getUserGrowth();

        assertThat(growth.get(growth.size() - 1).getNewUsers()).isEqualTo(2);
    }

    @Test
    void usersWhoJoinedBeforeTheWindowStillCountTowardTheTotal() {
        // A user from two years ago must not make the running total look like zero.
        given(userRepository.findAll()).willReturn(List.of(userJoined(730), userJoined(0)));

        List<AdminOverviewResponse.GrowthPoint> growth = service.overview().getUserGrowth();

        assertThat(growth.get(0).getCumulativeUsers()).isEqualTo(1);
        assertThat(growth.get(growth.size() - 1).getCumulativeUsers()).isEqualTo(2);
    }

    @Test
    void cumulativeGrowthNeverDecreases() {
        given(userRepository.findAll()).willReturn(List.of(userJoined(0), userJoined(40), userJoined(100)));

        List<AdminOverviewResponse.GrowthPoint> growth = service.overview().getUserGrowth();

        for (int i = 1; i < growth.size(); i++) {
            assertThat(growth.get(i).getCumulativeUsers())
                    .isGreaterThanOrEqualTo(growth.get(i - 1).getCumulativeUsers());
        }
    }

    @Test
    void triggerDistributionCountsRealCategoriesMostCommonFirst() {
        given(triggerRepository.findAll()).willReturn(List.of(
                trigger("Workload", TriggerConfirmation.NOT_REQUIRED),
                trigger("Workload", TriggerConfirmation.CONFIRMED),
                trigger("Sleep", TriggerConfirmation.PENDING)));

        List<AdminOverviewResponse.CategoryCount> distribution = service.overview().getTriggerDistribution();

        assertThat(distribution).hasSize(2);
        assertThat(distribution.get(0).getCategory()).isEqualTo("Workload");
        assertThat(distribution.get(0).getCount()).isEqualTo(2);
        assertThat(distribution.get(1).getCategory()).isEqualTo("Sleep");
    }

    @Test
    void dismissedTriggersAreExcluded() {
        given(triggerRepository.findAll()).willReturn(List.of(
                trigger("Workload", TriggerConfirmation.NOT_REQUIRED),
                trigger("Sleep", TriggerConfirmation.DISMISSED)));

        List<AdminOverviewResponse.CategoryCount> distribution = service.overview().getTriggerDistribution();

        assertThat(distribution).hasSize(1);
        assertThat(distribution.get(0).getCategory()).isEqualTo("Workload");
    }

    @Test
    void anEmptyPlatformReportsNoDistributionRatherThanPlaceholders() {
        assertThat(service.overview().getTriggerDistribution()).isEmpty();
    }
}
