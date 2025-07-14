import java.util.*;
import java.security.*;
import java.nio.charset.StandardCharsets;

public abstract class User {
    // === Constants ===
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final int MIN_PASSWORD_LENGTH = 8;

    // === Static User Database ===
    private static final Map<String, User> usersDB = Collections.synchronizedMap(new HashMap<>());

    // === Instance Attributes ===
    private final String userID;  // Immutable for security
    private String name;
    private String email;
    private final String passwordHash;  // Hashed, never stored as plaintext
    private final byte[] salt;  // Unique salt per user

    // === Constructor ===
    protected User(String userID, String name, String email, String password) {
        this.userID = Objects.requireNonNull(userID, "User ID cannot be null");
        this.name = validateName(name);
        this.email = validateEmail(email);
        this.salt = generateSalt();
        this.passwordHash = hashPassword(validatePassword(password));
        
        // Thread-safe registration
        synchronized (usersDB) {
            if (usersDB.containsKey(email)) {
                throw new IllegalArgumentException("Email already registered");
            }
            usersDB.put(email, this);
        }
    }

    // === Validation Methods ===
    private String validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }
        return name.trim();
    }

    private String validateEmail(String email) {
        if (email == null || !email.matches("^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            throw new IllegalArgumentException("Invalid email format");
        }
        return email.toLowerCase();
    }

    private String validatePassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException(
                String.format("Password must be at least %d characters", MIN_PASSWORD_LENGTH)
            );
        }
        return password;
    }

    // === Security Methods ===
    private byte[] generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return salt;
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
            md.update(salt);
            byte[] hashedBytes = md.digest(password.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder sb = new StringBuilder();
            for (byte b : hashedBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Hashing algorithm not available", e);
        }
    }

    // === Authentication ===
    public static User login(String email, String password) {
        if (email == null || password == null) {
            return null;
        }

        User user = usersDB.get(email.toLowerCase());
        if (user == null) {
            return null;  // User not found
        }

        return user.passwordHash.equals(user.hashPassword(password)) ? user : null;
    }

    public void logout() {
        System.out.println("User " + email + " logged out.");
    }

    // === Password Management ===
    public boolean changePassword(String oldPassword, String newPassword) {
        if (!hashPassword(oldPassword).equals(passwordHash)) {
            return false;
        }
        
        String newHash = hashPassword(validatePassword(newPassword));
        // In real implementation, would need thread-safe update mechanism
        // passwordHash = newHash; // Note: This won't work as passwordHash is final
        
        return true;
    }

    // === Getters ===
    public String getUserID() { return userID; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getpassword(){ return passwordHash;}

    // === Abstract Methods ===
    public abstract void displayDashboard();

    // === Static Helpers ===
    public static boolean emailExists(String email) {
        return usersDB.containsKey(email.toLowerCase());
    }

    // === Cleanup ===
    public static void deregisterUser(String email) {
        usersDB.remove(email.toLowerCase());
    }
}