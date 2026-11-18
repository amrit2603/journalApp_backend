package net.engineeringdigest.journalApp.config;

import net.engineeringdigest.journalApp.entity.User;
import net.engineeringdigest.journalApp.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** Opt-in local demo setup. Existing accounts are never overwritten. */
@Configuration
@Profile("demo")
public class DemoUserConfig {
    @Bean
    CommandLineRunner demoUser(UserService users,
            @Value("${JOURNAL_DEMO_USERNAME:}") String username,
            @Value("${JOURNAL_DEMO_PASSWORD:}") String password) {
        return args -> {
            if (username.isBlank() || username.length() > 100 || password.length() < 12 || password.length() > 72) {
                throw new IllegalArgumentException("Demo profile needs a username (1-100 chars) and password (12-72 chars)");
            }
            if (users.findByUserName(username) != null) return;
            var user = User.builder().userName(username).password(password).build();
            if (!users.saveNewUser(user)) throw new IllegalStateException("Could not create demo account");
        };
    }
}
