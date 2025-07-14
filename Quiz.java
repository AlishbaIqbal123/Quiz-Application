import java.util.*;
import java.util.stream.Collectors;

public class Quiz {
    // === Constants ===
    private static final int DEFAULT_DURATION_MINUTES = 30;
    private static final Difficulty DEFAULT_DIFFICULTY = Difficulty.MEDIUM;

    // === Attributes ===
    private final String quizId;
    private String title;
    private String category;
    private final List<Question> questions;
    private Difficulty overallDifficulty;
    private int durationMinutes;
    private final Map<String, Integer> questionScores; // QuestionID -> Score
    private final Set<String> questionIdSet; // For faster lookups

    // === Difficulty Enum ===
    public enum Difficulty {
        EASY, MEDIUM, HARD
    }

    // === Constructor ===
    public Quiz(String quizId, String title, String category) {
        this.quizId = Objects.requireNonNull(quizId, "Quiz ID cannot be null");
        this.title = Objects.requireNonNull(title, "Title cannot be null");
        this.category = Objects.requireNonNull(category, "Category cannot be null");
        this.questions = new ArrayList<>();
        this.questionScores = new HashMap<>();
        this.questionIdSet = new HashSet<>();
        this.overallDifficulty = DEFAULT_DIFFICULTY;
        this.durationMinutes = DEFAULT_DURATION_MINUTES;
    }

    // === Core Methods ===

    /**
     * Adds a question to the quiz with specified score
     * @param question The question to add
     * @param score The point value for this question
     * @throws IllegalArgumentException if question is null or already exists in quiz
     */
    public void addQuestion(Question question, int score) {
        Objects.requireNonNull(question, "Question cannot be null");
        if (score <= 0) {
            throw new IllegalArgumentException("Score must be positive");
        }
        if (questionIdSet.contains(question.getQuestionId())) {
            throw new IllegalArgumentException("Question already exists in quiz");
        }

        questions.add(question);
        questionScores.put(question.getQuestionId(), score);
        questionIdSet.add(question.getQuestionId());
        updateOverallDifficulty();
    }

    /**
     * Removes a question from the quiz
     * @param questionId ID of the question to remove
     * @return true if question was removed, false if not found
     */
    public boolean removeQuestion(String questionId) {
        Objects.requireNonNull(questionId, "Question ID cannot be null");
        
        boolean removed = questions.removeIf(q -> q.getQuestionId().equals(questionId));
        if (removed) {
            questionScores.remove(questionId);
            questionIdSet.remove(questionId);
            updateOverallDifficulty();
        }
        return removed;
    }

    /**
     * Generates a randomized variant of the quiz
     * @param numQuestions Number of questions to include
     * @return A new Quiz instance with random questions
     * @throws IllegalArgumentException if numQuestions is invalid
     */
    public Quiz generateRandomVariant(int numQuestions) {
        if (numQuestions <= 0 || numQuestions > questions.size()) {
            throw new IllegalArgumentException(
                "Number of questions must be between 1 and " + questions.size());
        }

        Quiz variant = new Quiz(
            quizId + "-" + UUID.randomUUID().toString().substring(0, 8),
            title + " (Variant)", 
            category
        );
        
        List<Question> shuffled = new ArrayList<>(questions);
        Collections.shuffle(shuffled);
        
        shuffled.stream()
            .limit(numQuestions)
            .forEach(q -> variant.addQuestion(q, questionScores.get(q.getQuestionId())));
            
        return variant;
    }

    /**
     * Calculates the total possible score for this quiz
     * @return Sum of all question scores
     */
    public int getTotalScore() {
        return questionScores.values().stream()
            .mapToInt(Integer::intValue)
            .sum();
    }

    // === Helper Methods ===
    private void updateOverallDifficulty() {
        if (questions.isEmpty()) {
            overallDifficulty = DEFAULT_DIFFICULTY;
            return;
        }
        
        double avg = questions.stream()
            .mapToInt(q -> q.getDifficulty().ordinal())
            .average()
            .orElse(DEFAULT_DIFFICULTY.ordinal());
            
        this.overallDifficulty = Difficulty.values()[(int) Math.round(avg)];
    }

    // === Builder Pattern ===
    public static class Builder {
        private final Quiz quiz;
        
        public Builder(String quizId, String title) {
            this.quiz = new Quiz(
                Objects.requireNonNull(quizId, "Quiz ID cannot be null"),
                Objects.requireNonNull(title, "Title cannot be null"),
                "General"
            );
        }
        
        public Builder withCategory(String category) {
            quiz.category = Objects.requireNonNull(category, "Category cannot be null");
            return this;
        }
        
        public Builder withDuration(int minutes) {
            if (minutes <= 0) {
                throw new IllegalArgumentException("Duration must be positive");
            }
            quiz.durationMinutes = minutes;
            return this;
        }
        
        public Builder addQuestion(Question question, int score) {
            quiz.addQuestion(question, score);
            return this;
        }
        
        public Quiz build() {
            return quiz;
        }
    }

    // === Getters ===
    public String getQuizId() { return quizId; }
    public String getTitle() { return title; }
    public String getCategory() { return category; }
    public int getDurationMinutes() { return durationMinutes; }
    public Difficulty getOverallDifficulty() { return overallDifficulty; }
    
    public List<Question> getQuestions() { 
        return Collections.unmodifiableList(questions); 
    }
    
    public int getQuestionScore(Question question) {
        Objects.requireNonNull(question, "Question cannot be null");
        return questionScores.getOrDefault(question.getQuestionId(), 0);
    }
    
    public int getQuestionCount() {
        return questions.size();
    }
    
    public boolean containsQuestion(String questionId) {
        return questionIdSet.contains(questionId);
    }

    // === Setters ===
    public void setTitle(String title) {
        this.title = Objects.requireNonNull(title, "Title cannot be null");
    }
    
    public void setCategory(String category) {
        this.category = Objects.requireNonNull(category, "Category cannot be null");
    }
    
    public void setDuration(int minutes) {
        if (minutes <= 0) {
            throw new IllegalArgumentException("Duration must be positive");
        }
        this.durationMinutes = minutes;
    }

    @Override
    public String toString() {
        return String.format(
            "Quiz[ID=%s, Title=%s, Category=%s, Questions=%d, TotalScore=%d]",
            quizId, title, category, questions.size(), getTotalScore()
        );
    }
}