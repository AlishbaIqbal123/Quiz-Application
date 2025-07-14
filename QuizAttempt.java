import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class QuizAttempt {
    // === Constants ===
    private static final double TIME_PENALTY_THRESHOLD = 2.0; // Flag if >2x average time
    private static final double MAX_TIME_PENALTY = 0.3; // Maximum 30% time penalty

    // === Core Attributes ===
    private final String attemptId;
    private final Student student;
    private final Quiz quiz;
    private final LocalDateTime startTime;
    private LocalDateTime endTime;
    private final AtomicInteger rawScore = new AtomicInteger(0);
    private final AtomicReference<Double> timeWeightedScore = new AtomicReference<>(0.0);
    private final AtomicReference<Question> hardestQuestion = new AtomicReference<>();

    // === Thread-safe Tracking Structures ===
    private final Map<Question, String> answers; // Preserves insertion order
    private final Map<Question, Duration> timePerQuestion;
    private final Set<Question> flaggedQuestions;

    // === Constructor ===
    public QuizAttempt(Student student, Quiz quiz) {
        this.attemptId = UUID.randomUUID().toString();
        this.student = Objects.requireNonNull(student, "Student cannot be null");
        this.quiz = Objects.requireNonNull(quiz, "Quiz cannot be null");
        this.startTime = LocalDateTime.now();
        this.answers = new LinkedHashMap<>(); // Preserves question order
        this.timePerQuestion = new ConcurrentHashMap<>();
        this.flaggedQuestions = ConcurrentHashMap.newKeySet();
    }

    // === Core Methods ===

    /**
     * Records a student's answer for a question
     * @param question The question being answered
     * @param answer The chosen answer
     * @param timeTaken Duration spent on this question
     * @throws IllegalArgumentException if parameters are invalid
     */
    public void recordAnswer(Question question, String answer, Duration timeTaken) {
        Objects.requireNonNull(question, "Question cannot be null");
        Objects.requireNonNull(answer, "Answer cannot be null");
        Objects.requireNonNull(timeTaken, "Time taken cannot be null");
        
        if (timeTaken.isNegative()) {
            throw new IllegalArgumentException("Time taken cannot be negative");
        }

        answers.put(question, answer);
        timePerQuestion.put(question, timeTaken);

        // Auto-flag if took significantly longer than average
        double avgTime = getAverageTimePerQuestion();
        if (avgTime > 0 && timeTaken.toMillis() > TIME_PENALTY_THRESHOLD * avgTime) {
            flaggedQuestions.add(question);
        }
    }

    /**
     * Finalizes the attempt and calculates all metrics
     */
    public void finalizeAttempt() {
        this.endTime = LocalDateTime.now();
        calculateScores();
    }

    // === Score Calculation ===

    private void calculateScores() {
        // Calculate raw score
        int score = answers.entrySet().stream()
            .filter(e -> e.getKey().isCorrect(e.getValue()))
            .mapToInt(e -> quiz.getQuestionScore(e.getKey()))
            .sum();
        rawScore.set(score);

        // Calculate weighted score
        timeWeightedScore.set(calculateTimeWeightedScore());

        // Identify hardest question
        hardestQuestion.set(identifyHardestQuestion());
    }

    private double calculateTimeWeightedScore() {
        if (answers.isEmpty()) return 0.0;

        double maxTime = timePerQuestion.values().stream()
            .mapToDouble(Duration::toMillis)
            .max()
            .orElse(1.0);

        double totalWeightedScore = answers.entrySet().stream()
            .mapToDouble(e -> {
                Question q = e.getKey();
                boolean correct = q.isCorrect(e.getValue());
                if (!correct) return 0.0;

                double timeRatio = timePerQuestion.get(q).toMillis() / maxTime;
                double timeWeight = 1 - Math.min(timeRatio, MAX_TIME_PENALTY);
                return quiz.getQuestionScore(q) * timeWeight;
            })
            .sum();

        return totalWeightedScore;
    }

    // === Analytics Methods ===

    public Map<String, Object> getAnalytics() {
        Map<String, Object> analytics = new HashMap<>();
        analytics.put("correctCount", getCorrectCount());
        analytics.put("incorrectCount", answers.size() - getCorrectCount());
        analytics.put("averageTimeMillis", getAverageTimePerQuestion());
        analytics.put("accuracyByCategory", getAccuracyByCategory());
        analytics.put("flaggedQuestions", flaggedQuestions.size());
        return Collections.unmodifiableMap(analytics);
    }

    private int getCorrectCount() {
        return (int) answers.entrySet().stream()
            .filter(e -> e.getKey().isCorrect(e.getValue()))
            .count();
    }

    private Question identifyHardestQuestion() {
        return answers.entrySet().stream()
            .filter(e -> !e.getKey().isCorrect(e.getValue()))
            .max(Comparator.comparingDouble(e -> 
                timePerQuestion.getOrDefault(e.getKey(), Duration.ZERO).toMillis()))
            .map(Map.Entry::getKey)
            .orElse(null);
    }

    private Map<String, Double> getAccuracyByCategory() {
        return answers.entrySet().stream()
            .collect(Collectors.groupingBy(
                e -> e.getKey().getCategory(),
                Collectors.collectingAndThen(
                    Collectors.summarizingInt(e -> 
                        e.getKey().isCorrect(e.getValue()) ? 1 : 0),
                    stats -> stats.getAverage()
                )
            ));
    }

    // === Getters ===

    public String getAttemptId() { return attemptId; }
    public Student getStudent() { return student; }
    public Quiz getQuiz() { return quiz; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public int getRawScore() { return rawScore.get(); }
    public double getWeightedScore() { return timeWeightedScore.get(); }
    public Question getHardestQuestion() { return hardestQuestion.get(); }
    public Duration getTotalDuration() { 
        return Duration.between(startTime, endTime != null ? endTime : LocalDateTime.now());
    }

    public Map<Question, String> getAnswers() {
        return Collections.unmodifiableMap(answers);
    }

    public List<Question> getIncorrectQuestions() {
        return answers.entrySet().stream()
            .filter(e -> !e.getKey().isCorrect(e.getValue()))
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    public double getAverageTimePerQuestion() {
        return timePerQuestion.values().stream()
            .mapToDouble(Duration::toMillis)
            .average()
            .orElse(0.0);
    }

    public Set<Question> getFlaggedQuestions() {
        return Collections.unmodifiableSet(flaggedQuestions);
    }

    // === Utility Methods ===

    public void flagQuestion(Question question) {
        if (answers.containsKey(question)) {
            flaggedQuestions.add(question);
        }
    }

    public void unflagQuestion(Question question) {
        flaggedQuestions.remove(question);
    }

    @Override
    public String toString() {
        return String.format(
            "QuizAttempt[ID=%s, Student=%s, Quiz=%s, Score=%d/%d]",
            attemptId, student.getName(), quiz.getTitle(), 
            rawScore.get(), quiz.getTotalScore()
        );
    }
}