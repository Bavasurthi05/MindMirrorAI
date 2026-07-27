package com.project.mentalhealth.application.service;

import com.project.mentalhealth.application.ports.in.GoalUseCase;
import com.project.mentalhealth.domain.model.User;
import com.project.mentalhealth.domain.model.WellnessGoal;
import com.project.mentalhealth.domain.repository.UserRepository;
import com.project.mentalhealth.domain.repository.WellnessGoalRepository;
import com.project.mentalhealth.interfaces.api.v1.goal.dto.GoalRequest;
import com.project.mentalhealth.interfaces.api.v1.goal.dto.GoalResponse;
import com.project.mentalhealth.shared.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class GoalService implements GoalUseCase {

    private final WellnessGoalRepository goalRepository;
    private final UserRepository userRepository;

    public GoalService(WellnessGoalRepository goalRepository, UserRepository userRepository) {
        this.goalRepository = goalRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public List<GoalResponse> list(String userEmail) {
        User user = requireUser(userEmail);
        if (goalRepository.countByUserId(user.getId()) == 0) {
            seedGoals(user);
        }
        return goalRepository.findByUserIdOrderByIdAsc(user.getId()).stream()
                .map(GoalResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public GoalResponse create(String userEmail, GoalRequest request) {
        User user = requireUser(userEmail);
        WellnessGoal goal = new WellnessGoal();
        goal.setUser(user);
        goal.setTitle(request.getTitle());
        goal.setTarget(Math.max(1, request.getTarget()));
        goal.setPeriod(request.getPeriod() == null ? "weekly" : request.getPeriod());
        goal.setProgress(0);
        goal.setCompleted(false);
        return GoalResponse.from(goalRepository.save(goal));
    }

    @Override
    @Transactional
    public GoalResponse incrementProgress(String userEmail, Long goalId) {
        User user = requireUser(userEmail);
        WellnessGoal goal = goalRepository.findByIdAndUserId(goalId, user.getId())
                .orElseThrow(() -> new ApiException("Goal not found", HttpStatus.NOT_FOUND));
        int next = Math.min(goal.getTarget(), goal.getProgress() + 1);
        goal.setProgress(next);
        goal.setCompleted(next >= goal.getTarget());
        return GoalResponse.from(goalRepository.save(goal));
    }

    @Override
    @Transactional
    public void delete(String userEmail, Long goalId) {
        User user = requireUser(userEmail);
        WellnessGoal goal = goalRepository.findByIdAndUserId(goalId, user.getId())
                .orElseThrow(() -> new ApiException("Goal not found", HttpStatus.NOT_FOUND));
        goalRepository.delete(goal);
    }

    private void seedGoals(User user) {
        List<WellnessGoal> goals = new ArrayList<>();
        goals.add(goal(user, "Complete 5 journal entries", 5));
        goals.add(goal(user, "Meditate 3 times", 3));
        goals.add(goal(user, "Log your mood every day", 7));
        goalRepository.saveAll(goals);
    }

    private WellnessGoal goal(User user, String title, int target) {
        WellnessGoal goal = new WellnessGoal();
        goal.setUser(user);
        goal.setTitle(title);
        goal.setTarget(target);
        goal.setPeriod("weekly");
        goal.setProgress(0);
        goal.setCompleted(false);
        return goal;
    }

    private User requireUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.UNAUTHORIZED));
    }
}
