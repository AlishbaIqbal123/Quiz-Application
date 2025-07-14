import java.io.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.lang.Integer;


public class Student extends User {
    // === Constants ===
    private static final int RECENT_ATTEMPTS_TO_SHOW = 5;
    private static final String PROGRESS_REPORT_FILENAME = "ProgressReport_%s.txt";
    
    // === Core Attributes ===
    private final String studentID;
    private final String department;
    private String password;
    private final List<QuizAttempt> quizAttempts;
    private final Map<String, AtomicInteger> weakTopics; // Thread-safe topic tracking
    private final Map<String, Long> correctAnswersByCategory;
    private final Map<String, Long> totalAnswersByCategory;

    // === Constructor ===
    public Student(String userID, String name, String email, String password, 
                  String studentID, String department) {
        super(userID, name, email, password);
        this.studentID = Objects.requireNonNull(studentID, "Student ID cannot be null");
        this.department = Objects.requireNonNull(department, "Department cannot be null");
        this.password = validatePassword(password);
        this.quizAttempts = new CopyOnWriteArrayList<>(); // Thread-safe
        this.weakTopics = new ConcurrentHashMap<>();
        this.correctAnswersByCategory = new ConcurrentHashMap<>();
        this.totalAnswersByCategory = new ConcurrentHashMap<>();
    }

    // === Core Methods ===

    /**
     * Records a quiz attempt and updates performance metrics
     * @param quiz The quiz being attempted
     * @param answeredQuestions List of answered questions
     * @return The created QuizAttempt
     */
    public QuizAttempt takeQuiz(Quiz quiz, List<Question> answeredQuestions) {
        Objects.requireNonNull(quiz, "Quiz cannot be null");
        Objects.requireNonNull(answeredQuestions, "Answered questions cannot be null");
        
        QuizAttempt attempt = new QuizAttempt(this, quiz);
        quizAttempts.add(attempt);
        updatePerformanceMetrics(attempt);
        return attempt;
    }

    /**
     * Adds a completed quiz attempt
     * @param attempt The QuizAttempt to add
     */
    public void addQuizAttempt(QuizAttempt attempt) {
        Objects.requireNonNull(attempt, "Quiz attempt cannot be null");
        if (!attempt.getStudent().equals(this)) {
            throw new IllegalArgumentException("Attempt does not belong to this student");
        }
        
        quizAttempts.add(attempt);
        updatePerformanceMetrics(attempt);
    }

    /**
     * Displays the student's progress report
     */
    public void viewProgress() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("  PROGRESS REPORT: " + getName());
        System.out.println("=".repeat(50));

        if (quizAttempts.isEmpty()) {
            System.out.println("No quiz attempts yet!");
            return;
        }

        printSummaryStatistics();
        printRecentAttempts();
        printCategoryPerformance();
        
        exportProgressReport();
    }

    // === Performance Tracking ===

   private void updatePerformanceMetrics(QuizAttempt attempt) {
    attempt.getIncorrectQuestions().forEach(q -> 
        weakTopics.computeIfAbsent(q.getCategory(), k -> new AtomicInteger()).incrementAndGet());

    attempt.getQuiz().getQuestions().forEach(q -> {
        String category = q.getCategory();
        totalAnswersByCategory.merge(category, 1L, Long::sum);
        if (attempt.getAnswers().getOrDefault(q, "").equals(q.getCorrectAnswer())) {
            correctAnswersByCategory.merge(category, 1L, Long::sum);
        }
    });
}


    // === Reporting ===

    private void printSummaryStatistics() {
        double avgScore = quizAttempts.stream()
            .mapToInt(QuizAttempt::getRawScore)
            .average()
            .orElse(0.0);
        
        System.out.printf("Total Attempts: %d\n", quizAttempts.size());
        System.out.printf("Average Score: %.1f\n", avgScore);
        System.out.println();
    }

    private void printRecentAttempts() {
        System.out.println("Recent Attempts:");
        System.out.printf("%-20s %-15s %s\n", "Quiz", "Score", "Date");
        System.out.println("-".repeat(50));

        quizAttempts.stream()
            .skip(Math.max(0, quizAttempts.size() - RECENT_ATTEMPTS_TO_SHOW))
            .forEach(attempt -> System.out.printf("%-20s %-15d %s\n",
                attempt.getQuiz().getTitle(),
                attempt.getRawScore(),
                LocalDateTime.now()));
    }

    private void printCategoryPerformance() {
        System.out.println("\nPerformance by Category:");
        totalAnswersByCategory.forEach((category, total) -> {
            long correct = correctAnswersByCategory.getOrDefault(category, 0L);
            double percentage = total > 0 ? (double) correct / total * 100 : 0;
            System.out.printf("- %s: %.1f%% (%d/%d)\n", 
                category, percentage, correct, total);
        });
    }

    private void exportProgressReport() {
        String filename = String.format(PROGRESS_REPORT_FILENAME, studentID);
        try (PrintWriter writer = new PrintWriter(filename)) {
            writer.println("=".repeat(50));
            writer.println("  PROGRESS REPORT: " + getName());
            writer.println("=".repeat(50));
            writer.println();
            
            writer.println("Summary Statistics:");
            writer.printf("Total Attempts: %d\n", quizAttempts.size());
            writer.printf("Average Score: %.1f\n", quizAttempts.stream()
                .mapToInt(QuizAttempt::getRawScore)
                .average()
                .orElse(0.0));
            writer.println();
            
            writer.println("Weak Topics:");
            weakTopics.entrySet().stream()
             .sorted((e1, e2) -> Integer.compare(e2.getValue().get(), e1.getValue().get()))
             .forEach(e -> writer.printf("- %s: %d errors\n", e.getKey(), e.getValue().get()));
            
            writer.println("\nReport generated on: " + LocalDateTime.now());
            System.out.println("\n✅ Progress report exported to: " + filename);
        } catch (FileNotFoundException e) {
            System.out.println("❌ Error exporting report: " + e.getMessage());
        }
    }

    // === Helper Methods ===
    private String validatePassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
        if (password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }
        return password;
    }

    // === Getters ===
    @Override
    public void displayDashboard() {
        viewProgress();
    }

    public String getStudentID() {
        return studentID;
    }

    public String getDepartment() {
        return department;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String newPassword) {
        this.password = validatePassword(newPassword);
    }

    public List<QuizAttempt> getQuizAttempts() {
        return Collections.unmodifiableList(quizAttempts);
    }

    public Map<String, Integer> getWeakTopics() {
    return weakTopics.entrySet().stream()
        .collect(Collectors.toUnmodifiableMap(
            Map.Entry::getKey,
            entry -> entry.getValue().get()  // Convert AtomicInteger to Integer
        ));
}

    @Override
    public String toString() {
        return String.format("Student[ID=%s, Name=%s, Department=%s]", 
            studentID, getName(), department);
    }
}