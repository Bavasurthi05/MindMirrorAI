package com.project.mentalhealth.application.service;

import com.project.mentalhealth.application.ports.in.FeedbackUseCase;
import com.project.mentalhealth.application.ports.out.MlAnalysisPort;
import com.project.mentalhealth.domain.model.AdminAuditLog;
import com.project.mentalhealth.domain.model.Role;
import com.project.mentalhealth.domain.model.User;
import com.project.mentalhealth.domain.repository.AdminAuditLogRepository;
import com.project.mentalhealth.domain.repository.AssessmentSubmissionRepository;
import com.project.mentalhealth.domain.repository.JournalEntryRepository;
import com.project.mentalhealth.domain.repository.MoodEntryRepository;
import com.project.mentalhealth.domain.repository.RecoveryActionRepository;
import com.project.mentalhealth.domain.repository.RoleRepository;
import com.project.mentalhealth.domain.repository.TriggerEntryRepository;
import com.project.mentalhealth.domain.repository.UserRepository;
import com.project.mentalhealth.interfaces.api.v1.admin.dto.AdminUserResponse;
import com.project.mentalhealth.shared.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * The guards matter more than the happy path here: a wrong answer locks every
 * administrator out of the deployment.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminRoleManagementTest {

    private static final String ADMIN = "ROLE_ADMIN";
    private static final String USER = "ROLE_USER";

    @Mock private UserRepository userRepository;
    @Mock private JournalEntryRepository journalRepository;
    @Mock private MoodEntryRepository moodRepository;
    @Mock private AssessmentSubmissionRepository assessmentRepository;
    @Mock private TriggerEntryRepository triggerRepository;
    @Mock private RecoveryActionRepository recoveryRepository;
    @Mock private FeedbackUseCase feedbackUseCase;
    @Mock private MlAnalysisPort mlAnalysisPort;
    @Mock private RoleRepository roleRepository;
    @Mock private AdminAuditLogRepository auditLogRepository;

    private AdminService service;

    @BeforeEach
    void setUp() {
        service = new AdminService(userRepository, journalRepository, moodRepository,
                assessmentRepository, triggerRepository, recoveryRepository, feedbackUseCase,
                mlAnalysisPort, roleRepository, auditLogRepository);

        given(roleRepository.findByName(ADMIN)).willReturn(Optional.of(role(ADMIN)));
        given(roleRepository.findByName(USER)).willReturn(Optional.of(role(USER)));
        given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));
        // Plenty of admins by default; individual tests narrow this.
        given(userRepository.countByRoleNameAndEnabledTrue(ADMIN)).willReturn(3L);
    }

    private Role role(String name) {
        Role role = new Role();
        role.setName(name);
        return role;
    }

    private User user(long id, String email, String roleName, boolean enabled) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEnabled(enabled);
        user.setRole(role(roleName));
        given(userRepository.findById(id)).willReturn(Optional.of(user));
        return user;
    }

    // --- Promotion / demotion ------------------------------------------------------------

    @Test
    void anAdminCanPromoteAnotherUser() {
        user(2, "user@example.com", USER, true);

        AdminUserResponse response = service.setUserRole("admin@example.com", 2L, ADMIN);

        assertThat(response.getRole()).isEqualTo(ADMIN);
    }

    @Test
    void anAdminCanDemoteAnotherAdmin() {
        user(2, "other@example.com", ADMIN, true);

        assertThat(service.setUserRole("admin@example.com", 2L, USER).getRole()).isEqualTo(USER);
    }

    @Test
    void roleNamesAreAcceptedCaseInsensitively() {
        user(2, "user@example.com", USER, true);

        assertThat(service.setUserRole("admin@example.com", 2L, "role_admin").getRole()).isEqualTo(ADMIN);
    }

    @Test
    void anUnknownRoleIsRejected() {
        user(2, "user@example.com", USER, true);

        assertThatThrownBy(() -> service.setUserRole("admin@example.com", 2L, "ROLE_SUPERUSER"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Unknown role");
    }

    @Test
    void reassigningTheSameRoleIsANoOp() {
        user(2, "user@example.com", USER, true);

        service.setUserRole("admin@example.com", 2L, USER);

        verify(userRepository, never()).save(any(User.class));
        verify(auditLogRepository, never()).save(any(AdminAuditLog.class));
    }

    // --- Lockout guards -------------------------------------------------------------------

    @Test
    void anAdminCannotChangeTheirOwnRole() {
        user(1, "admin@example.com", ADMIN, true);

        assertThatThrownBy(() -> service.setUserRole("admin@example.com", 1L, USER))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("cannot change your own role");
    }

    @Test
    void theSelfCheckIgnoresEmailCasing() {
        user(1, "Admin@Example.com", ADMIN, true);

        assertThatThrownBy(() -> service.setUserRole("admin@example.com", 1L, USER))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("cannot change your own role");
    }

    @Test
    void theLastActiveAdminCannotBeDemoted() {
        user(2, "other@example.com", ADMIN, true);
        given(userRepository.countByRoleNameAndEnabledTrue(ADMIN)).willReturn(1L);

        assertThatThrownBy(() -> service.setUserRole("admin@example.com", 2L, USER))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("last active administrator");
    }

    @Test
    void demotingAnAdminIsFineWhileOthersRemain() {
        user(2, "other@example.com", ADMIN, true);
        given(userRepository.countByRoleNameAndEnabledTrue(ADMIN)).willReturn(2L);

        assertThat(service.setUserRole("admin@example.com", 2L, USER).getRole()).isEqualTo(USER);
    }

    @Test
    void promotingIsNeverBlockedByTheLastAdminRule() {
        user(2, "user@example.com", USER, true);
        given(userRepository.countByRoleNameAndEnabledTrue(ADMIN)).willReturn(1L);

        assertThat(service.setUserRole("admin@example.com", 2L, ADMIN).getRole()).isEqualTo(ADMIN);
    }

    @Test
    void anAlreadyDisabledAdminDoesNotCountAsTheLastOne() {
        // They cannot sign in, so demoting them cannot lock anyone out.
        user(2, "dormant@example.com", ADMIN, false);
        given(userRepository.countByRoleNameAndEnabledTrue(ADMIN)).willReturn(1L);

        assertThat(service.setUserRole("admin@example.com", 2L, USER).getRole()).isEqualTo(USER);
    }

    // --- Enable / disable -----------------------------------------------------------------

    @Test
    void anAdminCannotDisableTheirOwnAccount() {
        user(1, "admin@example.com", ADMIN, true);

        assertThatThrownBy(() -> service.setUserEnabled("admin@example.com", 1L, false))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("cannot disable your own account");
    }

    @Test
    void theLastActiveAdminCannotBeDisabled() {
        user(2, "other@example.com", ADMIN, true);
        given(userRepository.countByRoleNameAndEnabledTrue(ADMIN)).willReturn(1L);

        assertThatThrownBy(() -> service.setUserEnabled("admin@example.com", 2L, false))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("no active administrator");
    }

    @Test
    void disablingAnOrdinaryUserIsAllowed() {
        user(2, "user@example.com", USER, true);

        assertThat(service.setUserEnabled("admin@example.com", 2L, false).isEnabled()).isFalse();
    }

    @Test
    void reEnablingIsNeverGuarded() {
        user(1, "admin@example.com", ADMIN, false);

        assertThat(service.setUserEnabled("admin@example.com", 1L, true).isEnabled()).isTrue();
    }

    // --- Audit ----------------------------------------------------------------------------

    @Test
    void aRoleChangeIsAudited() {
        user(2, "user@example.com", USER, true);

        service.setUserRole("admin@example.com", 2L, ADMIN);

        ArgumentCaptor<AdminAuditLog> captor = ArgumentCaptor.forClass(AdminAuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AdminAuditLog entry = captor.getValue();
        assertThat(entry.getAction()).isEqualTo(AdminAuditLog.ACTION_ROLE_CHANGED);
        assertThat(entry.getActorEmail()).isEqualTo("admin@example.com");
        assertThat(entry.getTargetEmail()).isEqualTo("user@example.com");
        assertThat(entry.getPreviousValue()).isEqualTo(USER);
        assertThat(entry.getNewValue()).isEqualTo(ADMIN);
    }

    @Test
    void anAccessChangeIsAudited() {
        user(2, "user@example.com", USER, true);

        service.setUserEnabled("admin@example.com", 2L, false);

        ArgumentCaptor<AdminAuditLog> captor = ArgumentCaptor.forClass(AdminAuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo(AdminAuditLog.ACTION_ACCESS_CHANGED);
        assertThat(captor.getValue().getPreviousValue()).isEqualTo("enabled");
        assertThat(captor.getValue().getNewValue()).isEqualTo("disabled");
    }

    @Test
    void aRefusedChangeIsNotAudited() {
        user(1, "admin@example.com", ADMIN, true);

        assertThatThrownBy(() -> service.setUserRole("admin@example.com", 1L, USER))
                .isInstanceOf(ApiException.class);

        verify(auditLogRepository, never()).save(any(AdminAuditLog.class));
    }

    @Test
    void changingTheRoleOfAMissingUserIsNotFound() {
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.setUserRole("admin@example.com", 99L, ADMIN))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("not found");
    }
}
