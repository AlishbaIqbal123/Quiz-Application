import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class QuestionBank {
    // === Thread-safe Data Structures ===
    private final ConcurrentMap<String, Question> questionMap;  // O(1) lookup by ID
    private final ConcurrentMap<String, List<Question>> questionsByCategory;  // Category -> Questions
    private final ConcurrentSkipListMap<String, Question> questionsByDifficulty;  // Sorted by difficulty
    
    // === Constructor ===
    public QuestionBank() {
        this.questionMap = new ConcurrentHashMap<>();
        this.questionsByCategory = new ConcurrentHashMap<>();
        this.questionsByDifficulty = new ConcurrentSkipListMap<>(new DifficultyComparator());
    }

    // === Core Methods ===

    /**
     * Adds a question to the question bank
     * @param question The question to add
     * @throws IllegalArgumentException if question is null or already exists
     */
    public void addQuestion(Question question) {
        Objects.requireNonNull(question, "Question cannot be null");
        
        // Atomic operation to check existence and add
        Question existing = questionMap.putIfAbsent(question.getQuestionId(), question);
        if (existing != null) {
            throw new IllegalArgumentException("Question with ID " + question.getQuestionId() + " already exists");
        }
        
        // Add to category index (thread-safe)
        questionsByCategory.compute(question.getCategory(), (k, v) -> {
            List<Question> list = (v == null) ? new CopyOnWriteArrayList<>() : v;
            list.add(question);
            return list;
        });
        
        // Add to difficulty index
        questionsByDifficulty.put(
            createDifficultyKey(question.getDifficulty(), question.getQuestionId()), 
            question
        );
    }

    /**
     * Gets random questions from a category
     * @param count Number of questions to return
     * @param category Category to filter by (null for all categories)
     * @return List of random questions
     */
    public List<Question> getRandomQuestions(int count, String category) {
        if (count <= 0) return Collections.emptyList();
        
        List<Question> pool = (category == null) 
            ? new ArrayList<>(questionMap.values())
            : questionsByCategory.getOrDefault(category, Collections.emptyList());
            
        if (pool.isEmpty()) return Collections.emptyList();
        
        // Thread-safe shuffle using Fisher-Yates
        List<Question> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled, ThreadLocalRandom.current());
        
        return shuffled.subList(0, Math.min(count, shuffled.size()));
    }

    /**
     * Gets questions by difficulty range (inclusive)
     * @param min Minimum difficulty level
     * @param max Maximum difficulty level
     * @return List of questions in difficulty range
     */
    public List<Question> getQuestionsByDifficulty(Question.DifficultyLevel min, 
                                                 Question.DifficultyLevel max) {
        Objects.requireNonNull(min, "Minimum difficulty cannot be null");
        Objects.requireNonNull(max, "Maximum difficulty cannot be null");
        
        return questionsByDifficulty.subMap(
            createDifficultyKey(min, ""), true,
            createDifficultyKey(max, "\uffff"), true
        ).values().stream().collect(Collectors.toList());
    }

    /**
     * Gets all questions in the bank
     * @return Unmodifiable list of all questions
     */
    public List<Question> getAllQuestions() {
        return Collections.unmodifiableList(new ArrayList<>(questionMap.values()));
    }

    /**
     * Removes a question from the bank
     * @param questionId ID of question to remove
     * @return true if question was removed, false if not found
     */
    public boolean removeQuestion(String questionId) {
        Objects.requireNonNull(questionId, "Question ID cannot be null");
        
        Question question = questionMap.remove(questionId);
        if (question == null) return false;
        
        // Remove from category index
        questionsByCategory.computeIfPresent(question.getCategory(), (k, v) -> {
            v.remove(question);
            return v.isEmpty() ? null : v;
        });
        
        // Remove from difficulty index
        questionsByDifficulty.remove(createDifficultyKey(question.getDifficulty(), questionId));
        return true;
    }

    // === Helper Methods ===
    private String createDifficultyKey(Question.DifficultyLevel difficulty, String questionId) {
        return difficulty.name() + "|" + questionId;
    }

    // === Comparator ===
    private static class DifficultyComparator implements Comparator<String> {
        @Override
        public int compare(String a, String b) {
            // Split into difficulty level and question ID parts
            String[] partsA = a.split("\\|", 2);
            String[] partsB = b.split("\\|", 2);
            
            // First compare by difficulty level
            int diffCompare = partsA[0].compareTo(partsB[0]);
            if (diffCompare != 0) return diffCompare;
            
            // Then by question ID if same difficulty
            return partsA[1].compareTo(partsB[1]);
        }
    }

    // === Getters ===
    public int getTotalQuestions() {
        return questionMap.size();
    }

    public Question getQuestion(String id) {
        return questionMap.get(id);
    }

    public List<Question> getQuestionsByCategory(String category) {
        return Collections.unmodifiableList(
            questionsByCategory.getOrDefault(category, Collections.emptyList())
        );
    }
}