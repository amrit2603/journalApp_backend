package net.engineeringdigest.journalApp;

import net.engineeringdigest.journalApp.repository.JournalEntryRepository;
import net.engineeringdigest.journalApp.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = "spring.data.mongodb.auto-index-creation=false")
class JournalAppApplicationTests {
    @MockitoBean UserRepository users;
    @MockitoBean JournalEntryRepository entries;
    @Test void contextLoads() {}
}
