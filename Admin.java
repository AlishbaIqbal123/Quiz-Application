import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class Admin extends User {
    private final String adminID;
    private final QuestionBank questionBank;
    private final Map<String, Quiz> quizzes; // Using interface Map instead of concrete HashMap
    private String password;

    public Admin(String userID, String name, String email, String password,
                 String adminID, QuestionBank questionBank) {
        super(userID, name, email, password);
        this.adminID = Objects.requireNonNull(adminID, "Admin ID cannot be null");
        this.questionBank = Objects.requireNonNull(questionBank, "QuestionBank cannot be null");
        this.quizzes = new HashMap<>();
        this.password = validatePassword(password);
    }

    // === Improved Core Methods ===

    public void addQuestion(Question question) {
        Objects.requireNonNull(question, "Question cannot be null");
        questionBank.addQuestion(question);
        System.out.printf("Successfully added question [ID: %s]%n", question.getQuestionId());
    }

    public void editQuestion(String questionID, String newText, List<String> newOptions, String newAnswer) {
        Objects.requireNonNull(questionID, "Question ID cannot be null");
        
        Question q = questionBank.getQuestion(questionID);
        if (q == null) {
            System.out.println("Error: Question not found with ID: " + questionID);
            return;
        }

        try {
            q.setText(newText);
            q.setOptions(newOptions);
            q.setCorrectAnswer(newAnswer);
            System.out.printf("Successfully updated question [ID: %s]%n", questionID);
        } catch (IllegalArgumentException e) {
            System.out.println("Error updating question: " + e.getMessage());
        }
    }

    public boolean deleteQuestion(String questionID) {
        Objects.requireNonNull(questionID, "Question ID cannot be null");
        
        if (!questionBank.removeQuestion(questionID)) {
            System.out.println("Error: Question not found with ID: " + questionID);
            return false;
        }

        // Remove from all quizzes
        quizzes.values().forEach(quiz -> quiz.removeQuestion(questionID));
        System.out.printf("Successfully deleted question [ID: %s]%n", questionID);
        return true;
    }

    public Quiz createQuiz(String quizID, String title, String category) {
        Objects.requireNonNull(quizID, "Quiz ID cannot be null");
        Objects.requireNonNull(title, "Title cannot be null");
        Objects.requireNonNull(category, "Category cannot be null");
        
        if (quizzes.containsKey(quizID)) {
            throw new IllegalArgumentException("Quiz ID already exists: " + quizID);
        }

        Quiz quiz = new Quiz(quizID, title, category);
        quizzes.put(quizID, quiz);
        System.out.printf("Successfully created quiz [ID: %s, Title: %s]%n", quizID, title);
        return quiz;
    }

    @Override
    public void displayDashboard() {
        System.out.println("\n=== ADMIN DASHBOARD ===");
        System.out.printf("Admin ID: %s%n", adminID);
        System.out.printf("Name: %s%n", getName());
        System.out.printf("Questions in bank: %d%n", questionBank.getTotalQuestions());
        System.out.printf("Active quizzes: %d%n", quizzes.size());
        
        // Show quiz summary
        if (!quizzes.isEmpty()) {
            System.out.println("\nQuiz Summary:");
            quizzes.forEach((id, quiz) -> 
                System.out.printf("- %s: %s (%d questions)%n", 
                    id, quiz.getTitle(), quiz.getQuestions().size()));
        }
    }

    // === Security Improvements ===
    
    private String validatePassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
        if (password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }
        return password;
    }

    @Override
    public String getpassword() {
        return password; // In real application, return encrypted/hashed version
    }

    public void setPassword(String newPassword) {
        this.password = validatePassword(newPassword);
        System.out.println("Password updated successfully");
    }

    // === Additional Utility Methods ===
    
    public boolean containsQuiz(String quizID) {
        return quizzes.containsKey(quizID);
    }

    public Quiz getQuiz(String quizID) {
        return quizzes.get(quizID);
    }

    // === Getters ===
    
    public String getAdminID() { 
        return adminID; 
    }

    public List<Quiz> getAllQuizzes() {
        return List.copyOf(quizzes.values()); // Return immutable copy
    }
}