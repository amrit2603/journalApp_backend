package net.engineeringdigest.journalApp;

import java.util.ArrayList;
import java.util.Optional;
import net.engineeringdigest.journalApp.config.SecurityConfig;
import net.engineeringdigest.journalApp.controller.JournalEntryController;
import net.engineeringdigest.journalApp.entity.JournalEntry;
import net.engineeringdigest.journalApp.entity.User;
import net.engineeringdigest.journalApp.repository.*;
import net.engineeringdigest.journalApp.service.*;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitConfig(JournalSecurityTests.Config.class)
@WebAppConfiguration
class JournalSecurityTests {
    @Configuration
    @EnableWebMvc
    @Import({SecurityConfig.class, UserService.class, JournalEntryService.class, JournalEntryController.class})
    static class Config {
        @Bean UserRepository users() { return mock(UserRepository.class); }
        @Bean JournalEntryRepository entries() { return mock(JournalEntryRepository.class); }
    }

    @Autowired WebApplicationContext context;
    @Autowired UserRepository users;
    @Autowired JournalEntryRepository entries;
    @Autowired PasswordEncoder encoder;
    MockMvc mvc;
    final ObjectId id = new ObjectId("507f1f77bcf86cd799439011");
    final String password = "synthetic-test-password";

    @BeforeEach
    void setup() {
        reset(users, entries);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        var entry = new JournalEntry();
        entry.setId(id); entry.setTitle("Alice note"); entry.setContent("Private");
        var alice = User.builder().userName("alice").password(encoder.encode(password)).build();
        alice.getJournalEntries().add(entry);
        var bob = User.builder().userName("bob").password(encoder.encode(password)).build();
        when(users.findByUserName("alice")).thenReturn(alice);
        when(users.findByUserName("bob")).thenReturn(bob);
        when(entries.findById(id)).thenReturn(Optional.of(entry));
        when(entries.save(any())).thenAnswer(call -> {
            JournalEntry saved = call.getArgument(0);
            if (saved.getId() == null) saved.setId(new ObjectId());
            return saved;
        });
    }

    @Test void anonymousAndWrongPasswordAreRejected() throws Exception {
        mvc.perform(get("/journal")).andExpect(status().isUnauthorized());
        mvc.perform(get("/journal").with(httpBasic("alice", "wrong"))).andExpect(status().isUnauthorized());
        mvc.perform(get("/journal").with(httpBasic("missing", password))).andExpect(status().isUnauthorized());
        verifyNoInteractions(entries);
    }

    @Test void databasePasswordAuthenticatesAndOwnerCanRead() throws Exception {
        mvc.perform(get("/journal/id/"+id).with(httpBasic("alice", password)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id.toHexString()))
                .andExpect(jsonPath("$.title").value("Alice note"));
    }

    @Test void anotherUserCannotReadChangeOrDeleteEntry() throws Exception {
        mvc.perform(get("/journal/id/"+id).with(httpBasic("bob", password))).andExpect(status().isNotFound());
        mvc.perform(put("/journal/id/"+id).with(httpBasic("bob", password)).contentType("application/json")
                .content("{\"title\":\"stolen\"}")).andExpect(status().isNotFound());
        mvc.perform(delete("/journal/id/"+id).with(httpBasic("bob", password))).andExpect(status().isNotFound());
        verifyNoInteractions(entries);
    }

    @Test void invalidIdAndInvalidBodiesReturnBadRequest() throws Exception {
        mvc.perform(get("/journal/id/not-an-id").with(httpBasic("alice", password))).andExpect(status().isBadRequest());
        for (String body : new String[]{"{}", "{\"title\":\"   \"}",
                "{\"title\":\"ok\",\"content\":\""+"x".repeat(10001)+"\"}"}) {
            mvc.perform(post("/journal").with(httpBasic("alice", password)).contentType("application/json")
                    .content(body)).andExpect(status().isBadRequest());
        }
        verify(entries, never()).save(any());
    }

    @Test void createLinksEntryToAuthenticatedUser() throws Exception {
        mvc.perform(post("/journal").with(httpBasic("bob", password)).contentType("application/json")
                .content("{\"title\":\"New note\",\"content\":\"Hello\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.title").value("New note"))
                .andExpect(jsonPath("$.id").isString());
        var bob = users.findByUserName("bob");
        org.junit.jupiter.api.Assertions.assertEquals(1,bob.getJournalEntries().size());
        verify(users).save(bob);
    }

    @Test void emptyListIsAnEmptyArray() throws Exception {
        mvc.perform(get("/journal").with(httpBasic("bob", password)))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test void ownerCanUpdateAndDelete() throws Exception {
        mvc.perform(put("/journal/id/"+id).with(httpBasic("alice", password)).contentType("application/json")
                .content("{\"title\":\"Updated\",\"content\":\"Changed\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Updated"));
        mvc.perform(delete("/journal/id/"+id).with(httpBasic("alice", password))).andExpect(status().isNoContent());
        verify(entries).deleteById(id);
    }
}
