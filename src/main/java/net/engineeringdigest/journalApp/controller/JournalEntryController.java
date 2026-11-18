package net.engineeringdigest.journalApp.controller;

import java.time.LocalDateTime;
import java.util.List;
import net.engineeringdigest.journalApp.entity.JournalEntry;
import net.engineeringdigest.journalApp.entity.User;
import net.engineeringdigest.journalApp.service.JournalEntryService;
import net.engineeringdigest.journalApp.service.UserService;
import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/journal")
public class JournalEntryController {
    private final JournalEntryService entries;
    private final UserService users;

    public JournalEntryController(JournalEntryService entries, UserService users) {
        this.entries = entries;
        this.users = users;
    }

    public record EntryInput(String title, String content) {}
    public record EntryView(String id, String title, String content, LocalDateTime date) {
        static EntryView of(JournalEntry entry) {
            return new EntryView(entry.getId().toHexString(), entry.getTitle(), entry.getContent(), entry.getDate());
        }
    }

    private User currentUser(Authentication auth) {
        User user = users.findByUserName(auth.getName());
        if (user == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return user;
    }

    private ObjectId parseId(String value) {
        if (!ObjectId.isValid(value)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid journal id");
        return new ObjectId(value);
    }

    private JournalEntry ownedEntry(String value, Authentication auth) {
        ObjectId id = parseId(value);
        User user = currentUser(auth);
        if (user.getJournalEntries().stream().noneMatch(entry -> id.equals(entry.getId()))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return entries.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private void validate(EntryInput input) {
        if (input.title() == null || input.title().isBlank() || input.title().length() > 200
                || (input.content() != null && input.content().length() > 10000)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title required (max 200); content max 10000");
        }
    }

    @GetMapping
    public List<EntryView> list(Authentication auth) {
        return currentUser(auth).getJournalEntries().stream().map(EntryView::of).toList();
    }

    @PostMapping
    public ResponseEntity<EntryView> create(@RequestBody EntryInput input, Authentication auth) {
        validate(input);
        currentUser(auth);
        var entry = new JournalEntry();
        entry.setTitle(input.title().trim());
        entry.setContent(input.content());
        entries.saveEntry(entry, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(EntryView.of(entry));
    }

    @GetMapping("/id/{id}")
    public EntryView get(@PathVariable String id, Authentication auth) {
        return EntryView.of(ownedEntry(id, auth));
    }

    @PutMapping("/id/{id}")
    public EntryView update(@PathVariable String id, @RequestBody EntryInput input, Authentication auth) {
        var entry = ownedEntry(id, auth);
        validate(input);
        entry.setTitle(input.title().trim());
        entry.setContent(input.content());
        entries.saveEntry(entry);
        return EntryView.of(entry);
    }

    @DeleteMapping("/id/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id, Authentication auth) {
        ownedEntry(id, auth);
        if (!entries.deleteById(parseId(id), auth.getName())) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return ResponseEntity.noContent().build();
    }
}
