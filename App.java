import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.security.*;

public class App {
    private static final QuestionBank questionBank = new QuestionBank();
    private static final Map<String, Student> students = new ConcurrentHashMap<>();
    private static final Map<String, Admin> admins = new ConcurrentHashMap<>();
    private static final Map<String, Quiz> quizzes = new ConcurrentHashMap<>();
    private static final Leaderboard leaderboard = new Leaderboard(new Leaderboard.TimeWeightedStrategy());
    private static final Scanner scanner = new Scanner(System.in);
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    public static void main(String[] args) {
        initializeData();
        while (true) {
            displayMainMenu();
            
            int choice = getIntInput();
            scanner.nextLine(); // Consume newline
            
            switch (choice) {
                case 1 -> studentLogin();
                case 2 -> adminLogin();
                case 3 -> exitSystem();
                default -> System.out.println("⚠️ Invalid option! Please try again.");
            }
        }
    }

    private static void displayMainMenu() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("  UNIVERSITY TECHNICAL INTERVIEW PREPARATION SYSTEM");
        System.out.println("=".repeat(50));
        System.out.println("1. Student Login");
        System.out.println("2. Admin Login");
        System.out.println("3. Exit");
        System.out.print("Select option: ");
    }

    private static void exitSystem() {
        System.out.println("\nExiting system. Goodbye!");
        scanner.close();
        System.exit(0);
    }

    private static int getIntInput() {
        while (true) {
            try {
                return scanner.nextInt();
            } catch (InputMismatchException e) {
                System.out.print("⚠️ Invalid input. Please enter a number: ");
                scanner.nextLine();
            }
        }
    }

    private static void initializeData() {
        initializeStudents();
        initializeAdmins();
        addTechnicalQuestions();
        createTechnicalQuizzes();
    }

    private static void initializeStudents() {
        // BCS Students
        String[] bcsStudents = {"Alishba", "Urooj", "Minal", "Ali", "Ahmad"};
        for (int i = 0; i < bcsStudents.length; i++) {
            String id = "BCS" + (100 + i);
            students.put(id, new Student(
                "U" + (100 + i), bcsStudents[i], 
                bcsStudents[i].toLowerCase() + "@bcs.edu", 
                hashPassword(bcsStudents[i].toLowerCase() + "123"), 
                id, "BCS"
            ));
        }
        
        // BSE Students
        String[] bseStudents = {"Minahil", "Ayesha", "Fatima", "Sayeda", "Mustafa"};
        for (int i = 0; i < bseStudents.length; i++) {
            String id = "BSE" + (200 + i);
            students.put(id, new Student(
                "U" + (200 + i), bseStudents[i],
                bseStudents[i].toLowerCase() + "@bse.edu",
                hashPassword(bseStudents[i].toLowerCase() + "123"),
                id, "BSE"
            ));
        }
    }

    private static void initializeAdmins() {
        admins.put("zoupash", new Admin(
            "ADM1", "Zoupash", "zoupash@uni.edu", 
            hashPassword("admin123"), "ZOUPASH", questionBank
        ));
        
        admins.put("zahid", new Admin(
            "ADM2", "Zahid Abbas", "zahid@uni.edu", 
            hashPassword("admin123"), "ZAHID", questionBank
        ));
        
        admins.put("irum", new Admin(
            "ADM3", "Irum", "irum@uni.edu", 
            hashPassword("admin123"), "IRUM", questionBank
        ));
    }

    private static void addTechnicalQuestions() {
        // Programming Basics (70% easy-medium)
        addQuestion("What is the time complexity of binary search?", 
                   Arrays.asList("O(n)", "O(log n)", "O(n^2)"), 
                   "O(log n)", Question.DifficultyLevel.EASY, "Algorithms",
                   "Binary search halves the search space each iteration");
        
        addQuestion("Which keyword makes a variable constant in Java?", 
                   Arrays.asList("final", "static", "const"), 
                   "final", Question.DifficultyLevel.EASY, "Programming",
                   "The 'final' keyword prevents value reassignment");
        
        addQuestion("What is the default value of a boolean in Java?", 
                   Arrays.asList("true", "false", "null", "0"), 
                   "false", Question.DifficultyLevel.EASY, "Programming",
                   "Primitive booleans default to false");
        
        addQuestion("What does JVM stand for?", 
                   Arrays.asList("Java Virtual Machine", "Java Visual Machine", "Java Verified Machine"), 
                   "Java Virtual Machine", Question.DifficultyLevel.EASY, "Programming",
                   "JVM executes Java bytecode");
        
        // Data Structures (70% easy-medium)
        addQuestion("Which data structure uses LIFO principle?", 
                   Arrays.asList("Queue", "Stack", "Tree"), 
                   "Stack", Question.DifficultyLevel.EASY, "DSA",
                   "Stacks use Last-In-First-Out access");
        
        addQuestion("Which collection maintains insertion order?", 
                   Arrays.asList("HashSet", "TreeSet", "LinkedHashSet", "HashMap"), 
                   "LinkedHashSet", Question.DifficultyLevel.MEDIUM, "DSA",
                   "LinkedHashSet maintains insertion order with linked list");
        
        addQuestion("What is the complexity of HashMap lookup?", 
                   Arrays.asList("O(1)", "O(n)", "O(log n)", "O(n log n)"), 
                   "O(1)", Question.DifficultyLevel.MEDIUM, "DSA",
                   "HashMap provides constant time for get/put operations");
        
        // Algorithms (30% hard)
        addQuestion("What is the worst-case time complexity of quicksort?", 
                   Arrays.asList("O(n log n)", "O(n^2)", "O(log n)"), 
                   "O(n^2)", Question.DifficultyLevel.HARD, "Algorithms",
                   "Occurs when pivot is smallest/largest element");
        
        // Databases
        addQuestion("What does SQL stand for?", 
                   Arrays.asList("Structured Query Language", "Simple Question Language", "System Query Logic"), 
                   "Structured Query Language", Question.DifficultyLevel.EASY, "Databases",
                   "Standard language for relational databases");
        
        addQuestion("Which is NOT a valid JDBC driver type?", 
                   Arrays.asList("Type 1", "Type 2", "Type 3", "Type 5"), 
                   "Type 5", Question.DifficultyLevel.MEDIUM, "Databases",
                   "JDBC only defines types 1-4");
        
        // Operating Systems (30% hard)
        addQuestion("What is thrashing in OS?", 
                   Arrays.asList("Excessive disk I/O", "CPU overheating", "Memory leak"), 
                   "Excessive disk I/O", Question.DifficultyLevel.HARD, "OS",
                   "Occurs when pages are swapped in/out too frequently");
        
        // Add more questions to reach 20+
        addQuestion("What is the size of int in Java?", 
                   Arrays.asList("16-bit", "32-bit", "64-bit", "Depends on platform"), 
                   "32-bit", Question.DifficultyLevel.EASY, "Programming",
                   "Java int is always 32-bit regardless of platform");
        
        addQuestion("What is the superclass of all exceptions?", 
                   Arrays.asList("Throwable", "Error", "RuntimeException", "Exception"), 
                   "Throwable", Question.DifficultyLevel.MEDIUM, "Programming",
                   "Throwable is at the root of exception hierarchy");
        
        addQuestion("Which design pattern ensures only one instance of a class?", 
                   Arrays.asList("Factory", "Singleton", "Observer", "Decorator"), 
                   "Singleton", Question.DifficultyLevel.MEDIUM, "Design Patterns",
                   "Singleton restricts instantiation to one object");
        
        addQuestion("Which is NOT a valid HTTP method?", 
                   Arrays.asList("GET", "POST", "FETCH", "PUT"), 
                   "FETCH", Question.DifficultyLevel.EASY, "Web",
                   "Standard methods are GET, POST, PUT, DELETE, etc.");
        
        addQuestion("What is the default port for HTTPS?", 
                   Arrays.asList("80", "8080", "443", "8443"), 
                   "443", Question.DifficultyLevel.EASY, "Web",
                   "HTTPS uses port 443 by default");
        
        addQuestion("Which is NOT a Spring stereotype annotation?", 
                   Arrays.asList("@Component", "@Service", "@Repository", "@Dao"), 
                   "@Dao", Question.DifficultyLevel.MEDIUM, "Frameworks",
                   "Spring uses @Repository for DAO classes");
        
        addQuestion("Which is NOT a valid Java 8 feature?", 
                   Arrays.asList("Streams", "Lambdas", "Modules", "Optionals"), 
                   "Modules", Question.DifficultyLevel.MEDIUM, "Programming",
                   "Modules were introduced in Java 9");
        
        addQuestion("Which is used for dependency injection in Spring?", 
                   Arrays.asList("@Autowired", "@Inject", "@Resource", "All of these"), 
                   "All of these", Question.DifficultyLevel.MEDIUM, "Frameworks",
                   "Spring supports multiple dependency injection annotations");
        
        addQuestion("What is the maximum length of a String in Java?", 
                   Arrays.asList("2^31-1", "2^32-1", "2^16-1", "No fixed limit"), 
                   "2^31-1", Question.DifficultyLevel.HARD, "Programming",
                   "String uses char[] with max array size");
        
        addQuestion("Which is NOT a valid OOP concept?", 
                   Arrays.asList("Inheritance", "Polymorphism", "Instantiation", "Abstraction"), 
                   "Instantiation", Question.DifficultyLevel.EASY, "OOP",
                   "Instantiation is the process, not a core concept");
    }

    private static void addQuestion(String text, List<String> options, 
                                  String correctAnswer, 
                                  Question.DifficultyLevel difficulty,
                                  String category, String explanation) {
        Question q = new Question.Builder(UUID.randomUUID().toString())
            .withText(text)
            .withOptions(options)
            .withCorrectAnswer(correctAnswer)
            .withDifficulty(difficulty)
            .withCategory(category)
            .withExplanation(explanation)
            .build();
        questionBank.addQuestion(q);
    }

    private static void createTechnicalQuizzes() {
        Admin admin = admins.get("zoupash");
        
        // BCS Quiz - Focus on Programming and Algorithms
        Quiz bcsQuiz = admin.createQuiz("QZ1", "BCS Core Concepts", "BCS");
        questionBank.getQuestionsByCategory("Programming").stream()
            .limit(12)
            .forEach(q -> bcsQuiz.addQuestion(q, q.getDifficulty().getWeight()));
        
        questionBank.getQuestionsByCategory("Algorithms").stream()
            .limit(8)
            .forEach(q -> bcsQuiz.addQuestion(q, q.getDifficulty().getWeight()));
        
        // BSE Quiz - Focus on Systems and Databases
        Quiz bseQuiz = admin.createQuiz("QZ2", "BSE Systems Fundamentals", "BSE");
        questionBank.getQuestionsByCategory("OS").stream()
            .limit(7)
            .forEach(q -> bseQuiz.addQuestion(q, q.getDifficulty().getWeight()));
        
        questionBank.getQuestionsByCategory("Databases").stream()
            .limit(7)
            .forEach(q -> bseQuiz.addQuestion(q, q.getDifficulty().getWeight()));
        
        questionBank.getQuestionsByCategory("Web").stream()
            .limit(6)
            .forEach(q -> bseQuiz.addQuestion(q, q.getDifficulty().getWeight()));
        
        quizzes.put(bcsQuiz.getQuizId(), bcsQuiz);
        quizzes.put(bseQuiz.getQuizId(), bseQuiz);
    }

    private static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashedBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Password hashing failed", e);
        }
    }

    private static void studentLogin() {
        System.out.println("\n" + "-".repeat(50));
        System.out.println("STUDENT LOGIN");
        System.out.println("-".repeat(50));
        
        System.out.print("Enter your student ID: ");
        String id = scanner.nextLine().trim().toUpperCase();
        
        if (!students.containsKey(id)) {
            System.out.println("\n❌ Invalid student ID!");
            return;
        }
        
        Student student = students.get(id);
        System.out.print("Enter password for " + student.getName() + ": ");
        String password = scanner.nextLine().trim();
        
        if (!hashPassword(password).equals(student.getPassword())) {
            System.out.println("\n❌ Invalid credentials!");
            return;
        }
        
        studentMenu(student);
    }

    private static void studentMenu(Student student) {
        while (true) {
            System.out.println("\n" + "=".repeat(50));
            System.out.printf("  %s STUDENT DASHBOARD: %s\n", student.getDepartment(), student.getName());
            System.out.println("=".repeat(50));
            System.out.println("1. Take Technical Quiz");
            System.out.println("2. View My Progress Report");
            System.out.println("3. View Department Leaderboard");
            System.out.println("4. View Study Resources");
            System.out.println("5. Change Password");
            System.out.println("6. Logout");
            System.out.print("Select option: ");
            
            int choice = getIntInput();
            scanner.nextLine();
            
            switch (choice) {
                case 1 -> takeTechnicalQuiz(student);
                case 2 -> student.viewProgress();
                case 3 -> displayDepartmentLeaderboard(student.getDepartment());
                case 4 -> showStudyResources();
                case 5 -> changeStudentPassword(student);
                case 6 -> { return; }
                default -> System.out.println("⚠️ Invalid option! Please try again.");
            }
        }
    }

    private static void takeTechnicalQuiz(Student student) {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("AVAILABLE TECHNICAL QUIZZES");
        System.out.println("-".repeat(50));

        List<Quiz> availableQuizzes = quizzes.values().stream()
            .filter(q -> q.getCategory().equals(student.getDepartment()))
            .collect(Collectors.toList());

        if (availableQuizzes.isEmpty()) {
            System.out.println("No quizzes available for your department!");
            return;
        }

        availableQuizzes.forEach(q -> System.out.printf("%s: %s (%d questions)\n",
            q.getQuizId(), q.getTitle(), q.getQuestions().size()));

        System.out.print("\nEnter quiz ID: ");
        String quizId = scanner.nextLine().trim();
        Quiz quiz = quizzes.get(quizId);

        if (quiz == null || !quiz.getCategory().equals(student.getDepartment())) {
            System.out.println("\n❌ Invalid quiz for your department!");
            return;
        }

        QuizSession session = new QuizSession(quiz, student);
        session.startSession();
        
        QuizAttempt attempt = new QuizAttempt(student, quiz);
        Map<String, Integer> categoryStats = new HashMap<>();
        List<Question> answeredQuestions = new ArrayList<>();

        while (session.getStatus() == QuizSession.QuizStatus.IN_PROGRESS) {
            Question q = session.getCurrentQuestion();
            System.out.println("\n" + "=".repeat(50));
            System.out.printf("QUESTION %d/%d\n", 
                quiz.getQuestions().size() - session.getQuestionsRemaining() + 1,
                quiz.getQuestions().size());
            System.out.println("=".repeat(50));
            System.out.println(q.toFormattedString());

            System.out.print("\nYour answer (enter letter): ");
            long startTime = System.currentTimeMillis();
            String letter = scanner.nextLine().trim().toUpperCase();

            while (letter.isEmpty() || letter.charAt(0) < 'A' || 
                   letter.charAt(0) >= 'A' + q.getOptions().size()) {
                System.out.print("Invalid input. Enter letter between A-" + 
                    (char) ('A' + q.getOptions().size() - 1) + ": ");
                letter = scanner.nextLine().trim().toUpperCase();
            }

            long endTime = System.currentTimeMillis();
            int selectedIndex = letter.charAt(0) - 'A';
            String selectedAnswer = q.getOptions().get(selectedIndex);
            Duration timeTaken = Duration.ofMillis(endTime - startTime);
            
            boolean isCorrect = q.isCorrect(selectedAnswer);
            if (isCorrect) {
                System.out.println("\n✅ Correct!");
            } else {
                System.out.printf("\n❌ Incorrect! The correct answer is %s\n", q.getCorrectAnswer());
                System.out.println("Explanation: " + q.getExplanation());
            }

            session.submitAnswer(selectedAnswer);
            attempt.recordAnswer(q, selectedAnswer, timeTaken);
            categoryStats.merge(q.getCategory(), isCorrect ? 1 : 0, Integer::sum);
            answeredQuestions.add(q);
        }

        attempt.finalizeAttempt();
        student.addQuizAttempt(attempt);
        leaderboard.addAttempt(attempt);

        System.out.println("\n" + "=".repeat(50));
        System.out.println("QUIZ COMPLETED!");
        System.out.println("=".repeat(50));
        System.out.println("🎯 Raw Score: " + attempt.getRawScore() + "/" + quiz.getTotalScore());
        System.out.println("🎯 Weighted Score: " + String.format("%.1f", attempt.getWeightedScore()));
        System.out.println("⏱️ Total Time: " + formatDuration(attempt.getTotalDuration()));
        System.out.println("✅ Attempt recorded for " + student.getName());

        displayQuizResults(student, quiz, attempt, categoryStats, answeredQuestions);
    }

    private static String formatDuration(Duration duration) {
        long minutes = duration.toMinutes();
        long seconds = duration.minusMinutes(minutes).getSeconds();
        return String.format("%02d:%02d", minutes, seconds);
    }

    private static void displayQuizResults(Student student, Quiz quiz, QuizAttempt attempt,
                                          Map<String, Integer> categoryStats, 
                                          List<Question> answeredQuestions) {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("DETAILED RESULTS ANALYSIS");
        System.out.println("=".repeat(50));
        
        System.out.println("\nPerformance by Category:");
        System.out.println("-".repeat(50));
        
        categoryStats.forEach((category, correctCount) -> {
            long totalInCategory = answeredQuestions.stream()
                .filter(q -> q.getCategory().equals(category))
                .count();
                
            double percentage = (double) correctCount / totalInCategory * 100;
            System.out.printf("%-15s: %5.1f%% (%d/%d)\n", 
                category, percentage, correctCount, totalInCategory);
        });
        
        System.out.println("\nWeak Areas (Below 70%):");
        System.out.println("-".repeat(50));
        boolean hasWeakAreas = false;
        for (Map.Entry<String, Integer> entry : categoryStats.entrySet()) {
            String category = entry.getKey();
            int correctCount = entry.getValue();
            long totalInCategory = answeredQuestions.stream()
                .filter(q -> q.getCategory().equals(category))
                .count();
                
            double percentage = (double) correctCount / totalInCategory * 100;
            if (percentage < 70) {
                hasWeakAreas = true;
                System.out.printf("%-15s: %5.1f%% - Suggested Resources:\n", category, percentage);
                suggestResources(category);
            }
        }
        
        if (!hasWeakAreas) {
            System.out.println("Great job! No significant weak areas detected");
        }
        
        System.out.println("\nIncorrect Questions Review:");
        System.out.println("-".repeat(50));
        for (Question q : attempt.getIncorrectQuestions()) {
            System.out.println("Q: " + q.getText());
            System.out.println("A: " + q.getCorrectAnswer());
            System.out.println("Explanation: " + q.getExplanation());
            System.out.println("-".repeat(50));
        }
    }

    private static void suggestResources(String category) {
        switch (category) {
            case "Programming" -> 
                System.out.println("  - Java Tutorials: https://docs.oracle.com/javase/tutorial/\n" +
                                  "  - Practice coding: https://leetcode.com/problemset/all/");
            case "DSA" -> 
                System.out.println("  - Visualizations: https://visualgo.net/en\n" +
                                  "  - Practice problems: https://www.hackerrank.com/domains/data-structures");
            case "Algorithms" -> 
                System.out.println("  - Algorithm Design Manual: https://www.algorist.com/\n" +
                                  "  - Big-O Cheatsheet: https://www.bigocheatsheet.com/");
            case "Databases" -> 
                System.out.println("  - SQL Practice: https://sqlzoo.net/\n" +
                                  "  - Database Design: https://www.db-book.com/");
            case "OS" -> 
                System.out.println("  - Operating System Concepts: https://www.os-book.com/\n" +
                                  "  - Virtual memory tutorial: https://www.geeksforgeeks.org/virtual-memory/");
            default -> 
                System.out.println("  - General CS resources: https://teachyourselfcs.com/");
        }
    }

    private static void displayDepartmentLeaderboard(String department) {
        System.out.println("\n" + "=".repeat(50));
        System.out.printf("  %s DEPARTMENT LEADERBOARD\n", department);
        System.out.println("=".repeat(50));
        
        List<QuizAttempt> topAttempts = leaderboard.getTopN(10).stream()
            .filter(a -> a.getStudent().getDepartment().equals(department))
            .sorted((a, b) -> Double.compare(b.getWeightedScore(), a.getWeightedScore()))
            .collect(Collectors.toList());
        
        if (topAttempts.isEmpty()) {
            System.out.println("No attempts recorded yet!");
            return;
        }
        
        System.out.printf("%-5s %-20s %-30s %-10s %s\n", 
            "Rank", "Student", "Quiz", "Score", "Time");
        System.out.println("-".repeat(70));
        
        for (int i = 0; i < topAttempts.size(); i++) {
            QuizAttempt attempt = topAttempts.get(i);
            System.out.printf("%-5d %-20s %-30s %-10.1f %s\n", 
                i + 1,
                attempt.getStudent().getName(),
                attempt.getQuiz().getTitle(),
                attempt.getWeightedScore(),
                formatDuration(attempt.getTotalDuration()));
        }
    }

    private static void showStudyResources() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("  RECOMMENDED STUDY RESOURCES");
        System.out.println("=".repeat(50));
        System.out.println("1. Programming Basics: https://learnprogramming.online/");
        System.out.println("2. Data Structures: https://visualgo.net/en");
        System.out.println("3. Algorithms: https://www.coursera.org/specializations/algorithms");
        System.out.println("4. Databases: https://sqlbolt.com/");
        System.out.println("5. OS Concepts: https://pages.cs.wisc.edu/~remzi/OSTEP/");
        System.out.println("6. System Design: https://github.com/donnemartin/system-design-primer");
        System.out.println("7. Technical Interview Prep: https://leetcode.com/");
    }

    private static void changeStudentPassword(Student student) {
        System.out.print("\nEnter current password: ");
        String current = scanner.nextLine();
        
        if (!hashPassword(current).equals(student.getPassword())) {
            System.out.println("❌ Incorrect current password!");
            return;
        }
        
        System.out.print("Enter new password: ");
        String newPass = scanner.nextLine();
        
        if (newPass.length() < 8) {
            System.out.println("❌ Password must be at least 8 characters");
            return;
        }
        
        System.out.print("Confirm new password: ");
        String confirm = scanner.nextLine();
        
        if (!newPass.equals(confirm)) {
            System.out.println("❌ Passwords don't match!");
            return;
        }
        
        student.setPassword(hashPassword(newPass));
        System.out.println("✅ Password changed successfully!");
    }

    private static void adminLogin() {
        System.out.println("\n" + "-".repeat(50));
        System.out.println("ADMIN LOGIN");
        System.out.println("-".repeat(50));
        
        System.out.print("Enter username: ");
        String username = scanner.nextLine().trim().toLowerCase();
        
        if (!admins.containsKey(username)) {
            System.out.println("\n❌ Admin not found!");
            return;
        }
        
        Admin admin = admins.get(username);
        System.out.print("Enter password: ");
        String password = scanner.nextLine();
        
        if (!hashPassword(password).equals(admin.getpassword())) {
            System.out.println("\n❌ Invalid credentials!");
            return;
        }
        
        adminMenu(admin);
    }

    private static void adminMenu(Admin admin) {
        while (true) {
            System.out.println("\n" + "=".repeat(50));
            System.out.println("  ADMIN DASHBOARD: " + admin.getName());
            System.out.println("=".repeat(50));
            System.out.println("1. Add New Question");
            System.out.println("2. Create New Quiz");
            System.out.println("3. View Question Bank");
            System.out.println("4. View All Quizzes");
            System.out.println("5. View All Students");
            System.out.println("6. Change Password");
            System.out.println("7. Logout");
            System.out.print("Select option: ");
            
            int choice = getIntInput();
            scanner.nextLine();
            
            switch (choice) {
                case 1 -> addTechnicalQuestion(admin);
                case 2 -> createNewQuiz(admin);
                case 3 -> displayQuestionBank();
                case 4 -> displayAllQuizzes();
                case 5 -> displayAllStudents();
                case 6 -> changeAdminPassword(admin);
                case 7 -> { return; }
                default -> System.out.println("⚠️ Invalid option! Please try again.");
            }
        }
    }

    private static void displayAllStudents() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("  ALL STUDENTS");
        System.out.println("=".repeat(50));
        
        System.out.printf("%-10s %-20s %-10s %-30s\n", "ID", "Name", "Dept", "Email");
        System.out.println("-".repeat(70));
        
       students.values().stream()
    .sorted(Comparator.comparing(s -> s.getStudentID()))
    .forEach(student -> System.out.printf(
        "%s: %s (%s)\n", 
        student.getStudentID(), 
        student.getName(), 
        student.getDepartment()
    ));
    }

    private static void changeAdminPassword(Admin admin) {
        System.out.print("\nEnter current password: ");
        String current = scanner.nextLine();
        
        if (!hashPassword(current).equals(admin.getpassword())) {
            System.out.println("❌ Incorrect current password!");
            return;
        }
        
        System.out.print("Enter new password: ");
        String newPass = scanner.nextLine();
        
        if (newPass.length() < 8) {
            System.out.println("❌ Password must be at least 8 characters");
            return;
        }
        
        System.out.print("Confirm new password: ");
        String confirm = scanner.nextLine();
        
        if (!newPass.equals(confirm)) {
            System.out.println("❌ Passwords don't match!");
            return;
        }
        
        admin.setPassword(hashPassword(newPass));
        System.out.println("✅ Password changed successfully!");
    }

    private static void addTechnicalQuestion(Admin admin) {
        System.out.println("\n" + "-".repeat(50));
        System.out.println("ADD NEW QUESTION");
        System.out.println("-".repeat(50));
        
        System.out.print("Enter question text: ");
        String text = scanner.nextLine();
        
        if (text.trim().isEmpty()) {
            System.out.println("❌ Question text cannot be empty!");
            return;
        }
        
        System.out.print("Enter options (comma separated): ");
        List<String> options = Arrays.stream(scanner.nextLine().split("\\s*,\\s*"))
            .filter(opt -> !opt.trim().isEmpty())
            .collect(Collectors.toList());
            
        if (options.size() < 2) {
            System.out.println("❌ At least 2 options required!");
            return;
        }
        
        System.out.print("Correct answer (enter letter A-" + (char)('A' + options.size() - 1) + "): ");
        String correctOption = scanner.nextLine().toUpperCase();
        
        while (correctOption.isEmpty() || correctOption.charAt(0) < 'A' || 
               correctOption.charAt(0) >= 'A' + options.size()) {
            System.out.print("Invalid input. Enter letter between A-" + (char)('A' + options.size() - 1) + ": ");
            correctOption = scanner.nextLine().toUpperCase();
        }
        
        String correctAnswer = options.get(correctOption.charAt(0) - 'A');
        
        System.out.print("Explanation: ");
        String explanation = scanner.nextLine();
        
        System.out.print("Difficulty (1-Easy, 2-Medium, 3-Hard): ");
        int diffChoice = getIntInput();
        while (diffChoice < 1 || diffChoice > 3) {
            System.out.print("Invalid choice. Enter 1-3: ");
            diffChoice = getIntInput();
        }
        Question.DifficultyLevel difficulty = Question.DifficultyLevel.values()[diffChoice - 1];
        scanner.nextLine();
        
        System.out.print("Category: ");
        String category = scanner.nextLine();
        
        Question question = new Question.Builder(UUID.randomUUID().toString())
            .withText(text)
            .withOptions(options)
            .withCorrectAnswer(correctAnswer)
            .withDifficulty(difficulty)
            .withCategory(category)
            .withExplanation(explanation)
            .build();
        
        admin.addQuestion(question);
        System.out.println("\n✅ Question added successfully!");
    }

    private static void createNewQuiz(Admin admin) {
        System.out.println("\n" + "-".repeat(50));
        System.out.println("CREATE NEW QUIZ");
        System.out.println("-".repeat(50));
        
        System.out.print("Enter quiz title: ");
        String title = scanner.nextLine();
        
        if (title.trim().isEmpty()) {
            System.out.println("❌ Quiz title cannot be empty!");
            return;
        }
        
        System.out.print("Enter target department (BCS/BSE): ");
        String department = scanner.nextLine().toUpperCase();
        
        while (!department.equals("BCS") && !department.equals("BSE")) {
            System.out.print("Invalid department. Enter BCS or BSE: ");
            department = scanner.nextLine().toUpperCase();
        }
        
        Quiz quiz = admin.createQuiz(UUID.randomUUID().toString(), title, department);
        
        System.out.println("\nAvailable Questions:");
        List<Question> allQuestions = questionBank.getAllQuestions();
        if (allQuestions.isEmpty()) {
            System.out.println("No questions available in the question bank!");
            return;
        }
        
        allQuestions.forEach(q -> 
            System.out.printf("[%s] %-8s %-10s %s\n", 
                q.getQuestionId().substring(0, 8),
                q.getDifficulty(),
                q.getCategory(),
                q.getText().substring(0, Math.min(40, q.getText().length())) + "..."));
            
        System.out.print("\nEnter question IDs to include (comma separated): ");
        String[] questionIds = scanner.nextLine().split("\\s*,\\s*");
        
        int addedCount = 0;
        for (String id : questionIds) {
            Question q = questionBank.getQuestion(id.trim());
            if (q != null) {
                quiz.addQuestion(q, q.getDifficulty().getWeight());
                addedCount++;
                System.out.println("Added: " + q.getText().substring(0, Math.min(50, q.getText().length())) + "...");
            } else {
                System.out.println("⚠️ Question not found: " + id);
            }
        }
        
        if (addedCount == 0) {
            System.out.println("❌ No questions were added to the quiz!");
            return;
        }
        
        quizzes.put(quiz.getQuizId(), quiz);
        System.out.printf("\n✅ Quiz created successfully! ID: %s\n", quiz.getQuizId());
    }

    private static void displayQuestionBank() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("  QUESTION BANK");
        System.out.println("=".repeat(50));
        
        List<Question> questions = questionBank.getAllQuestions();
        if (questions.isEmpty()) {
            System.out.println("No questions available!");
            return;
        }
        
        System.out.printf("%-8s %-10s %-15s %s\n", "ID", "Difficulty", "Category", "Question");
        System.out.println("-".repeat(80));
        
        questions.forEach(q -> 
            System.out.printf("%-8s %-10s %-15s %s\n", 
                q.getQuestionId().substring(0, 8),
                q.getDifficulty(),
                q.getCategory(),
                q.getText().substring(0, Math.min(50, q.getText().length())) + "..."));
    }

    private static void displayAllQuizzes() {
    System.out.println("\n" + "=".repeat(50));
    System.out.println("  ALL QUIZZES");
    System.out.println("=".repeat(50));

    if (quizzes.isEmpty()) {
        System.out.println("No quizzes available!");
        return;
    }

    System.out.printf("%-10s %-25s %-10s %s\n", "ID", "Title", "Dept", "Questions");
    System.out.println("-".repeat(60));

    quizzes.values().forEach(q -> 
        System.out.printf("%-10s %-25s %-10s %d\n", 
            q.getQuizId().length() > 8 ? q.getQuizId().substring(0, 8) : q.getQuizId(),
            q.getTitle(),
            q.getCategory(),
            q.getQuestions().size()));
}

}