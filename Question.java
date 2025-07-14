import java.util.List;
import java.util.Objects;
import java.util.Collections;
import java.util.ArrayList;

public class Question {
    // === Core Attributes ===
    private final String questionId;
    private String text;
    private List<String> options;
    private String correctAnswer;
    private DifficultyLevel difficulty;
    private String category;
    private String explanation;
    private int baseScore;
    private List<String> tags;
    
    // === Difficulty Enum ===
    public enum DifficultyLevel {
        EASY(1, "Easy"), 
        MEDIUM(2, "Medium"), 
        HARD(3, "Hard");
        
        private final int weight;
        private final String displayName;
        
        DifficultyLevel(int weight, String displayName) {
            this.weight = weight;
            this.displayName = displayName;
        }
        
        public int getWeight() {
            return weight;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }

    // === Builder Pattern ===
    public static class Builder {
        private final String questionId;
        private String text;
        private List<String> options;
        private String correctAnswer;
        private DifficultyLevel difficulty = DifficultyLevel.MEDIUM;
        private String category = "General";
        private String explanation = "";
        private int baseScore = 1;
        private List<String> tags = Collections.emptyList();

        public Builder(String questionId) {
            this.questionId = Objects.requireNonNull(questionId, "Question ID cannot be null");
        }

        public Builder withText(String text) {
            this.text = Objects.requireNonNull(text, "Question text cannot be null");
            return this;
        }

        public Builder withOptions(List<String> options) {
            this.options = new ArrayList<>(Objects.requireNonNull(options, "Options cannot be null"));
            return this;
        }

        public Builder withCorrectAnswer(String correctAnswer) {
            this.correctAnswer = Objects.requireNonNull(correctAnswer, "Correct answer cannot be null");
            return this;
        }

        public Builder withDifficulty(DifficultyLevel difficulty) {
            this.difficulty = Objects.requireNonNull(difficulty, "Difficulty cannot be null");
            return this;
        }

        public Builder withCategory(String category) {
            this.category = Objects.requireNonNull(category, "Category cannot be null");
            return this;
        }

        public Builder withExplanation(String explanation) {
            this.explanation = Objects.requireNonNull(explanation, "Explanation cannot be null");
            return this;
        }

        public Builder withBaseScore(int baseScore) {
            if (baseScore <= 0) {
                throw new IllegalArgumentException("Base score must be positive");
            }
            this.baseScore = baseScore;
            return this;
        }

        public Builder withTags(List<String> tags) {
            this.tags = new ArrayList<>(Objects.requireNonNull(tags, "Tags cannot be null"));
            return this;
        }

        public Question build() {
            return new Question(this);
        }
    }

    // === Constructor (Private - use Builder) ===
    private Question(Builder builder) {
        this.questionId = builder.questionId;
        this.text = builder.text;
        this.options = Collections.unmodifiableList(new ArrayList<>(builder.options));
        this.correctAnswer = builder.correctAnswer;
        this.difficulty = builder.difficulty;
        this.category = builder.category;
        this.explanation = builder.explanation;
        this.baseScore = builder.baseScore;
        this.tags = Collections.unmodifiableList(new ArrayList<>(builder.tags));
        validateQuestion();
    }

    // === Validation ===
    private void validateQuestion() {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalStateException("Question text cannot be empty");
        }
        if (options.size() < 2) {
            throw new IllegalStateException("At least 2 options required");
        }
        if (!options.contains(correctAnswer)) {
            throw new IllegalStateException("Correct answer must be in options");
        }
    }

    // === Core Methods ===
    public boolean isCorrect(String answer) {
        return correctAnswer.equals(answer);
    }

    public double calculatePartialCredit(String answer) {
        if (isCorrect(answer)) return 1.0;
        // Implement more sophisticated partial credit logic if needed
        return 0.0;
    }

    // === Getters ===
    public String getQuestionId() { return questionId; }
    public String getText() { return text; }
    public List<String> getOptions() { return options; }
    public String getCorrectAnswer() { return correctAnswer; }
    public DifficultyLevel getDifficulty() { return difficulty; }
    public String getCategory() { return category; }
    public String getExplanation() { return explanation; }
    public int getBaseScore() { return baseScore; }
    public List<String> getTags() { return tags; }

    // === Setters ===
    public void setText(String text) {
        this.text = Objects.requireNonNull(text, "Question text cannot be null");
    }

    public void setOptions(List<String> options) {
        this.options = Collections.unmodifiableList(new ArrayList<>(
            Objects.requireNonNull(options, "Options cannot be null")));
        validateQuestion();
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = Objects.requireNonNull(correctAnswer, "Correct answer cannot be null");
        if (!options.contains(correctAnswer)) {
            throw new IllegalArgumentException("Correct answer must be in options");
        }
    }

    // === Display Methods ===
    public String toFormattedString() {
        StringBuilder sb = new StringBuilder();
        sb.append(text).append("\n");
        for (int i = 0; i < options.size(); i++) {
            sb.append((char)('A' + i)).append(") ").append(options.get(i)).append("\n");
        }
        sb.append("Difficulty: ").append(difficulty.getDisplayName()).append("\n");
        sb.append("Category: ").append(category);
        return sb.toString();
    }

    // === Equality (Based on ID only) ===
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Question question = (Question) o;
        return questionId.equals(question.questionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(questionId);
    }

    @Override
    public String toString() {
        return "Question{" +
                "id='" + questionId + '\'' +
                ", text='" + text + '\'' +
                ", category='" + category + '\'' +
                ", difficulty=" + difficulty +
                '}';
    }
}