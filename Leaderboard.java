import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.concurrent.atomic.AtomicInteger;

public class Leaderboard {
    // === Data Structures ===
    private final NavigableMap<Double, List<QuizAttempt>> scoreMap;  // Sorted descending
    private final Map<String, Double> studentBestScores;  // StudentID -> BestScore
    private final PriorityQueue<QuizAttempt> recentHighScores;  // Max-heap of recent attempts
    private final Map<String, Integer> studentRankCache;  // StudentID -> Rank (cached)
    
    // === Configuration ===
    private static final int TOP_N = 10;
    private static final int RECENT_SCORES_LIMIT = 10;
    private final ScoreWeightingStrategy weightingStrategy;
    
    // === Constructor ===
    public Leaderboard(ScoreWeightingStrategy weightingStrategy) {
        this.scoreMap = new TreeMap<>(Collections.reverseOrder());
        this.studentBestScores = new ConcurrentHashMap<>();
        this.recentHighScores = new PriorityQueue<>(
            Comparator.comparingDouble(QuizAttempt::getWeightedScore)
        );
        this.studentRankCache = new ConcurrentHashMap<>();
        this.weightingStrategy = Objects.requireNonNull(weightingStrategy, 
            "Weighting strategy cannot be null");
    }

    // === Core Methods ===

    /**
     * Adds a quiz attempt to the leaderboard
     * @param attempt The quiz attempt to add
     * @throws IllegalArgumentException if attempt is null
     */
    public synchronized void addAttempt(QuizAttempt attempt) {
        Objects.requireNonNull(attempt, "Quiz attempt cannot be null");
        
        double weightedScore = weightingStrategy.calculateWeightedScore(attempt);
        String studentId = attempt.getStudent().getStudentID();
        
        // Update best score tracking
        studentBestScores.merge(
            studentId,
            weightedScore,
            Math::max
        );
        
        // Update score map
        scoreMap.computeIfAbsent(weightedScore, k -> new ArrayList<>())
               .add(attempt);
        
        // Update recent high scores
        updateRecentScores(attempt, weightedScore);
        
        // Invalidate rank cache for this student
        studentRankCache.remove(studentId);
    }

    private void updateRecentScores(QuizAttempt attempt, double weightedScore) {
        if (recentHighScores.size() < RECENT_SCORES_LIMIT) {
            recentHighScores.offer(attempt);
        } else if (weightedScore > recentHighScores.peek().getWeightedScore()) {
            recentHighScores.poll();
            recentHighScores.offer(attempt);
        }
    }

    /**
     * Gets top N attempts ordered by weighted score
     * @param n number of attempts to return
     * @return List of top attempts
     */
    public List<QuizAttempt> getTopN(int n) {
        if (n <= 0) {
            return Collections.emptyList();
        }
        
        return scoreMap.values().stream()
            .flatMap(List::stream)
            .limit(n)
            .collect(Collectors.toList());
    }

    /**
     * Gets student's current rank
     * @param studentId ID of the student
     * @return rank (1-based) or -1 if not found
     */
    public int getStudentRank(String studentId) {
        Objects.requireNonNull(studentId, "Student ID cannot be null");
        
        // Check cache first
        Integer cachedRank = studentRankCache.get(studentId);
        if (cachedRank != null) {
            return cachedRank;
        }
        
        Double bestScore = studentBestScores.get(studentId);
        if (bestScore == null) {
            return -1;  // Student not found
        }
        
        // Calculate rank by summing counts of higher scores
        int rank = scoreMap.headMap(bestScore).values().stream()
            .mapToInt(List::size)
            .sum() + 1;
        
        // Update cache
        studentRankCache.put(studentId, rank);
        return rank;
    }

    /**
     * Gets recent high score attempts
     * @return List of recent attempts ordered by score
     */
    public List<QuizAttempt> getRecentHighScores() {
        return recentHighScores.stream()
            .sorted(Comparator.comparingDouble(QuizAttempt::getWeightedScore).reversed())
            .collect(Collectors.toList());
    }

    // === Strategy Interface ===
    public interface ScoreWeightingStrategy {
        double calculateWeightedScore(QuizAttempt attempt);
    }

    // === Built-in Strategies ===
    
    /**
     * Strategy that weights scores based on completion time
     */
    public static class TimeWeightedStrategy implements ScoreWeightingStrategy {
        private static final double MAX_TIME_PENALTY = 0.3; // 30% maximum penalty
        
        @Override
        public double calculateWeightedScore(QuizAttempt attempt) {
            double baseScore = attempt.getRawScore();
            double timeFactor = 1.0 - Math.min(
                attempt.getTotalDuration().toMinutes(), 
                MAX_TIME_PENALTY
            );
            return baseScore * timeFactor;
        }
    }

    /**
     * Strategy that weights scores based on category performance
     */
    public static class CategoryWeightedStrategy implements ScoreWeightingStrategy {
        private final String prioritizedCategory;
        private static final double CATEGORY_BONUS = 0.25; // 25% bonus
        
        public CategoryWeightedStrategy(String category) {
            this.prioritizedCategory = Objects.requireNonNull(category, 
                "Category cannot be null");
        }
        
        @Override
        public double calculateWeightedScore(QuizAttempt attempt) {
            double baseScore = attempt.getRawScore();
            long categoryCorrect = attempt.getAnswers().entrySet().stream()
            .filter(e -> e.getKey().getCategory().equals(prioritizedCategory))
            .filter(e -> e.getKey().isCorrect(e.getValue()))
            .count();

            return baseScore * (1 + CATEGORY_BONUS * categoryCorrect);
        }
    }
}