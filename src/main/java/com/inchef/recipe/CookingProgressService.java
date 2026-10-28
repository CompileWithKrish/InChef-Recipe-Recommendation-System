package com.inchef.recipe;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class CookingProgressService {

    private final UserRecipeProgressRepository progressRepository;

    public CookingProgressService(UserRecipeProgressRepository progressRepository) {
        this.progressRepository = progressRepository;
    }

    /**
     * Parse raw instructions text into distinct, numbered step strings without mutating stored text.
     */
    public static List<String> parseInstructionSteps(String rawInstructions) {
        if (rawInstructions == null || rawInstructions.trim().isEmpty()) {
            return Collections.emptyList();
        }

        // Split by lines or numbered items e.g. "1. ", "Step 1:", or periods followed by uppercase
        String[] lines = rawInstructions.split("\\r?\\n");
        List<String> steps = new ArrayList<>();

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            // If a single line contains multiple sentences with period and space, check length
            if (lines.length == 1 && trimmed.contains(". ")) {
                String[] sentenceParts = trimmed.split("\\.\\s+(?=[A-Z0-9])");
                for (String part : sentenceParts) {
                    String p = part.trim();
                    if (!p.isEmpty()) {
                        if (!p.endsWith(".")) p = p + ".";
                        steps.add(cleanStepNumbering(p));
                    }
                }
            } else {
                steps.add(cleanStepNumbering(trimmed));
            }
        }

        if (steps.isEmpty()) {
            steps.add(rawInstructions.trim());
        }

        return steps;
    }

    private static String cleanStepNumbering(String text) {
        // Strip leading numbering like "1. ", "1 - ", "Step 1: " so UI can render clean badge
        return text.replaceAll("^(?:Step\\s*)?\\d+[.:\\-\\)]\\s*", "").trim();
    }

    @Transactional(readOnly = true)
    public CookingProgressResponse getProgress(Long userId, Long recipeId, int totalSteps) {
        if (userId == null) {
            return CookingProgressResponse.builder()
                    .recipeId(recipeId)
                    .userId(null)
                    .completedStepIndices(Collections.emptyList())
                    .completedStepsCount(0)
                    .totalSteps(totalSteps)
                    .progressPercentage(0.0)
                    .isCompleted(false)
                    .updatedAt(LocalDateTime.now())
                    .build();
        }

        Optional<UserRecipeProgress> opt = progressRepository.findByUserIdAndRecipeId(userId, recipeId);
        if (opt.isEmpty()) {
            return CookingProgressResponse.builder()
                    .recipeId(recipeId)
                    .userId(userId)
                    .completedStepIndices(Collections.emptyList())
                    .completedStepsCount(0)
                    .totalSteps(totalSteps)
                    .progressPercentage(0.0)
                    .isCompleted(false)
                    .updatedAt(LocalDateTime.now())
                    .build();
        }

        UserRecipeProgress progress = opt.get();
        List<Integer> completedIndices = parseIndices(progress.getCompletedStepIndices());
        int count = completedIndices.size();
        int safeTotal = (totalSteps > 0) ? totalSteps : (progress.getTotalSteps() != null ? progress.getTotalSteps() : count);
        double pct = (safeTotal > 0) ? Math.min(100.0, Math.round(((double) count / safeTotal) * 1000.0) / 10.0) : 0.0;
        boolean completed = (safeTotal > 0 && count >= safeTotal) || Boolean.TRUE.equals(progress.getIsCompleted());

        return CookingProgressResponse.builder()
                .recipeId(recipeId)
                .userId(userId)
                .completedStepIndices(completedIndices)
                .completedStepsCount(count)
                .totalSteps(safeTotal)
                .progressPercentage(pct)
                .isCompleted(completed)
                .updatedAt(progress.getUpdatedAt())
                .build();
    }

    public CookingProgressResponse updateStep(Long userId, Long recipeId, int stepIndex, boolean isStepCompleted, int totalSteps) {
        if (userId == null) {
            return getProgress(null, recipeId, totalSteps);
        }

        UserRecipeProgress progress = progressRepository.findByUserIdAndRecipeId(userId, recipeId)
                .orElseGet(() -> new UserRecipeProgress(userId, recipeId, "", totalSteps, false));

        Set<Integer> indexSet = new HashSet<>(parseIndices(progress.getCompletedStepIndices()));
        if (isStepCompleted) {
            indexSet.add(stepIndex);
        } else {
            indexSet.remove(stepIndex);
        }

        List<Integer> sorted = new ArrayList<>(indexSet);
        Collections.sort(sorted);
        progress.setCompletedStepIndices(serializeIndices(sorted));
        if (totalSteps > 0) {
            progress.setTotalSteps(totalSteps);
        }
        int effectiveTotal = (progress.getTotalSteps() != null && progress.getTotalSteps() > 0) ? progress.getTotalSteps() : totalSteps;
        boolean allDone = (effectiveTotal > 0 && sorted.size() >= effectiveTotal);
        progress.setIsCompleted(allDone);
        progress.setUpdatedAt(LocalDateTime.now());

        UserRecipeProgress saved = progressRepository.save(progress);

        double pct = (effectiveTotal > 0) ? Math.min(100.0, Math.round(((double) sorted.size() / effectiveTotal) * 1000.0) / 10.0) : 0.0;

        return CookingProgressResponse.builder()
                .recipeId(recipeId)
                .userId(userId)
                .completedStepIndices(sorted)
                .completedStepsCount(sorted.size())
                .totalSteps(effectiveTotal)
                .progressPercentage(pct)
                .isCompleted(saved.getIsCompleted())
                .updatedAt(saved.getUpdatedAt())
                .build();
    }

    public CookingProgressResponse setAllSteps(Long userId, Long recipeId, List<Integer> stepIndices, int totalSteps) {
        if (userId == null) {
            return getProgress(null, recipeId, totalSteps);
        }

        UserRecipeProgress progress = progressRepository.findByUserIdAndRecipeId(userId, recipeId)
                .orElseGet(() -> new UserRecipeProgress(userId, recipeId, "", totalSteps, false));

        Set<Integer> indexSet = (stepIndices != null) ? new HashSet<>(stepIndices) : new HashSet<>();
        List<Integer> sorted = new ArrayList<>(indexSet);
        Collections.sort(sorted);

        progress.setCompletedStepIndices(serializeIndices(sorted));
        if (totalSteps > 0) {
            progress.setTotalSteps(totalSteps);
        }
        int effectiveTotal = (progress.getTotalSteps() != null && progress.getTotalSteps() > 0) ? progress.getTotalSteps() : totalSteps;
        boolean allDone = (effectiveTotal > 0 && sorted.size() >= effectiveTotal);
        progress.setIsCompleted(allDone);
        progress.setUpdatedAt(LocalDateTime.now());

        UserRecipeProgress saved = progressRepository.save(progress);
        double pct = (effectiveTotal > 0) ? Math.min(100.0, Math.round(((double) sorted.size() / effectiveTotal) * 1000.0) / 10.0) : 0.0;

        return CookingProgressResponse.builder()
                .recipeId(recipeId)
                .userId(userId)
                .completedStepIndices(sorted)
                .completedStepsCount(sorted.size())
                .totalSteps(effectiveTotal)
                .progressPercentage(pct)
                .isCompleted(saved.getIsCompleted())
                .updatedAt(saved.getUpdatedAt())
                .build();
    }

    public CookingProgressResponse resetProgress(Long userId, Long recipeId, int totalSteps) {
        if (userId != null) {
            progressRepository.deleteByUserIdAndRecipeId(userId, recipeId);
        }
        return CookingProgressResponse.builder()
                .recipeId(recipeId)
                .userId(userId)
                .completedStepIndices(Collections.emptyList())
                .completedStepsCount(0)
                .totalSteps(totalSteps)
                .progressPercentage(0.0)
                .isCompleted(false)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private List<Integer> parseIndices(String csv) {
        if (csv == null || csv.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> {
                    try {
                        return Integer.parseInt(s);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    private String serializeIndices(List<Integer> indices) {
        if (indices == null || indices.isEmpty()) {
            return "";
        }
        return indices.stream().map(String::valueOf).collect(Collectors.joining(","));
    }
}
