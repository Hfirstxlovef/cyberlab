package org.cyberlab.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordHashGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);
        String password = "hongan123";
        String hash = encoder.encode(password);
        // Password hash generated (output removed as per debug code cleanup)
    }
}
