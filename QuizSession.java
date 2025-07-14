import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class QuizSession {
    // === Constants ===
    private static final int DEFAULT_BASE_TIME_PER_QUESTION = 90; // 1.5 minutes
    private static final double INITIAL_DIFFICULTY_LEVEL = 0.5;
    private static final double DIFFICULTY_INCREMENT = 0.1;
    private static final double DIFFICULTY_DECREMENT = 0.15;

    // === Core Session Properties ===
    private final String sessionId;
    private final Quiz quiz;
    private final Student student;
    private final Instant startTime;
    private Instant endTime;
    private final AtomicReference<QuizStatus> status;
    
    // === Question Tracking ===
    private final Queue<Question> questionQueue;
    private final Map<Question, String> answers;
    private final Map<Question, Duration> responseTimes;
    private final AtomicReference<Question> currentQuestion;
    private Instant questionStartTime;
    private ScheduledExecutorService timeoutExecutor;
    
    // === Adaptive Testing ===
    private DifficultyAdjustmentStrategy difficultyStrategy;
    private final int baseTimePerQuestion;

    // === Status Enum ===
    public enum QuizStatus {
        NOT_STARTED, IN_PROGRESS, PAUSED, COMPLETED, TIMED_OUT
    }

    // === Constructor ===
    public QuizSession(Quiz quiz, Student student) {
        this.sessionId = UUID.randomUUID().toString();
        this.quiz = Objects.requireNonNull(quiz, "Quiz cannot be null");
        this.student = Objects.requireNonNull(student, "Student cannot be null");
        this.startTime = Instant.now();
        this.status = new AtomicReference<>(QuizStatus.NOT_STARTED);
        this.questionQueue = new ConcurrentLinkedQueue<>(quiz.getQuestions());
        this.answers = new ConcurrentHashMap<>();
        this.responseTimes = new ConcurrentHashMap<>();
        this.currentQuestion = new AtomicReference<>();
        this.baseTimePerQuestion = DEFAULT_BASE_TIME_PER_QUESTION;
        this.difficultyStrategy = new DefaultDifficultyAdjustment();
        this.timeoutExecutor = Executors.newSingleThreadScheduledExecutor();
    }

    // === Core Session Methods ===
    
    /**
     * Starts the quiz session
     * @throws IllegalStateException if session was already started
     */
    public synchronized void startSession() {
        if (!status.compareAndSet(QuizStatus.NOT_STARTED, QuizStatus.IN_PROGRESS)) {
            throw new IllegalStateException("Session already started");
        }
        nextQuestion();
    }
    
    /**
     * Submits an answer for the current question
     * @param answer The answer to submit
     * @throws IllegalStateException if no active question or session not in progress
     */
    public synchronized void submitAnswer(String answer) {
        if (currentQuestion.get() == null || status.get() != QuizStatus.IN_PROGRESS) {
            throw new IllegalStateException("No active question");
        }
        
        Question question = currentQuestion.get();
        answers.put(question, Objects.requireNonNull(answer, "Answer cannot be null"));
        responseTimes.put(question, Duration.between(questionStartTime, Instant.now()));
        
        // Update difficulty strategy
        difficultyStrategy.updateDifficulty(
            question.isCorrect(answer),
            responseTimes.get(question)
        );
        
        nextQuestion();
    }
    
    /**
     * Moves to the next question or completes session if finished
     */
    private void nextQuestion() {
        cancelPendingTimeout();
        
        if (questionQueue.isEmpty()) {
            completeSession();
            return;
        }
        
        Question nextQuestion = difficultyStrategy.selectNextQuestion(questionQueue, answers);
        questionQueue.remove(nextQuestion);
        currentQuestion.set(nextQuestion);
        questionStartTime = Instant.now();
        
        scheduleTimeout();
    }
    
    // === Timeout Management ===
    
    private void scheduleTimeout() {
        int timeoutSeconds = difficultyStrategy.getTimeoutDuration(baseTimePerQuestion);
        timeoutExecutor.schedule(
            this::handleTimeout,
            timeoutSeconds,
            TimeUnit.SECONDS
        );
    }
    
    private synchronized void handleTimeout() {
        if (currentQuestion.get() != null && status.get() == QuizStatus.IN_PROGRESS) {
            submitAnswer(""); // Submit blank answer on timeout
            status.set(QuizStatus.TIMED_OUT);
        }
    }
    
    private void cancelPendingTimeout() {
        timeoutExecutor.shutdownNow();
        timeoutExecutor = Executors.newSingleThreadScheduledExecutor();
    }
    
    // === Session Completion ===
    
    private synchronized void completeSession() {
        status.set(QuizStatus.COMPLETED);
        endTime = Instant.now();
        currentQuestion.set(null);
        timeoutExecutor.shutdown();
    }
    
    /**
     * Saves the quiz attempt with all recorded answers
     * @return The created QuizAttempt
     */
    public QuizAttempt saveAttempt() {
        QuizAttempt attempt = new QuizAttempt(student, quiz);
        answers.forEach((q, a) -> 
            attempt.recordAnswer(q, a, responseTimes.getOrDefault(q, Duration.ZERO)));
        attempt.finalizeAttempt();
        return attempt;
    }

    // === Adaptive Testing Strategy ===
    
    public interface DifficultyAdjustmentStrategy {
        Question selectNextQuestion(Queue<Question> queue, Map<Question, String> answers);
        void updateDifficulty(boolean wasCorrect, Duration responseTime);
        int getTimeoutDuration(int baseDuration);
    }
    
    private static class DefaultDifficultyAdjustment implements DifficultyAdjustmentStrategy {
        private double currentDifficulty = INITIAL_DIFFICULTY_LEVEL;
        
        @Override
        public Question selectNextQuestion(Queue<Question> queue, Map<Question, String> answers) {
            return queue.stream()
                .min(Comparator.comparingDouble(q -> 
                    Math.abs(normalizedDifficulty(q) - currentDifficulty)))
                .orElse(queue.peek());
        }
        
        private double normalizedDifficulty(Question q) {
            return q.getDifficulty().getWeight() / 3.0; // Convert to 0-1 scale
        }
        
        @Override
        public void updateDifficulty(boolean wasCorrect, Duration responseTime) {
            double responseFactor = Math.min(1.0, responseTime.toMillis() / 60000.0);
            
            if (wasCorrect) {
                currentDifficulty = Math.min(1.0, 
                    currentDifficulty + DIFFICULTY_INCREMENT * responseFactor);
            } else {
                currentDifficulty = Math.max(0.0, 
                    currentDifficulty - DIFFICULTY_DECREMENT * (1 - responseFactor));
            }
        }
        
        @Override
        public int getTimeoutDuration(int baseDuration) {
            return (int)(baseDuration * (2 - currentDifficulty));
        }
    }

    // === Getters ===
    public String getSessionId() { return sessionId; }
    public Question getCurrentQuestion() { return currentQuestion.get(); }
    public Duration getCurrentQuestionElapsedTime() {
        return questionStartTime != null ? 
            Duration.between(questionStartTime, Instant.now()) : Duration.ZERO;
    }
    public QuizStatus getStatus() { return status.get(); }
    public int getQuestionsRemaining() { return questionQueue.size(); }
    public double getEstimatedSkillLevel() {
        return difficultyStrategy instanceof DefaultDifficultyAdjustment ?
            ((DefaultDifficultyAdjustment)difficultyStrategy).currentDifficulty : 0.5;
    }
    
    // === Cleanup ===
    @Override
    protected void finalize() throws Throwable {
        timeoutExecutor.shutdownNow();
        super.finalize();
    }
}