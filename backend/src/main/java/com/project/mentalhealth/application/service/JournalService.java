package com.project.mentalhealth.application.service;

import com.project.mentalhealth.application.ports.in.JournalUseCase;
import com.project.mentalhealth.domain.model.JournalEntry;
import com.project.mentalhealth.domain.model.User;
import com.project.mentalhealth.domain.repository.JournalEntryRepository;
import com.project.mentalhealth.domain.repository.UserRepository;
import com.project.mentalhealth.interfaces.api.v1.common.PageResponse;
import com.project.mentalhealth.interfaces.api.v1.journal.dto.JournalEntryRequest;
import com.project.mentalhealth.interfaces.api.v1.journal.dto.JournalEntryResponse;
import com.project.mentalhealth.shared.exception.ApiException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JournalService implements JournalUseCase {

    private final JournalEntryRepository journalRepository;
    private final UserRepository userRepository;

    public JournalService(JournalEntryRepository journalRepository, UserRepository userRepository) {
        this.journalRepository = journalRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public JournalEntryResponse create(String userEmail, JournalEntryRequest request) {
        User user = requireUser(userEmail);
        JournalEntry entry = new JournalEntry();
        entry.setUser(user);
        entry.setTitle(request.getTitle());
        entry.setContent(request.getContent());
        entry.setMood(request.getMood());
        return JournalEntryResponse.from(journalRepository.save(entry));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<JournalEntryResponse> list(String userEmail, int page, int size) {
        User user = requireUser(userEmail);
        Pageable pageable = PageRequest.of(Math.max(page, 0), size < 1 ? 10 : size);
        Page<JournalEntry> result = journalRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);
        return PageResponse.<JournalEntryResponse>builder()
                .content(result.map(JournalEntryResponse::from).getContent())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .first(result.isFirst())
                .last(result.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public JournalEntryResponse get(String userEmail, Long id) {
        User user = requireUser(userEmail);
        return JournalEntryResponse.from(requireEntry(id, user.getId()));
    }

    @Override
    @Transactional
    public JournalEntryResponse update(String userEmail, Long id, JournalEntryRequest request) {
        User user = requireUser(userEmail);
        JournalEntry entry = requireEntry(id, user.getId());
        entry.setTitle(request.getTitle());
        entry.setContent(request.getContent());
        entry.setMood(request.getMood());
        return JournalEntryResponse.from(journalRepository.save(entry));
    }

    @Override
    @Transactional
    public void delete(String userEmail, Long id) {
        User user = requireUser(userEmail);
        JournalEntry entry = requireEntry(id, user.getId());
        journalRepository.delete(entry);
    }

    private User requireUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.UNAUTHORIZED));
    }

    private JournalEntry requireEntry(Long id, Long userId) {
        return journalRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApiException("Journal entry not found", HttpStatus.NOT_FOUND));
    }
}
