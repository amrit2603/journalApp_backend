package et.engineeringdigest.journalApp.service;

import et.engineeringdigest.journalApp.entity.JournalEntry;
import et.engineeringdigest.journalApp.repository.JournalEntryRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class JournalEntryService {

    private final JournalEntryRepository journalEntryRepository;

    public JournalEntryService(JournalEntryRepository journalEntryRepository) {
        this.journalEntryRepository = journalEntryRepository;
    }

    public JournalEntry saveEntry(JournalEntry entry, String userName) {
        entry.setUserName(userName);
        entry.setDate(LocalDateTime.now());
        return journalEntryRepository.save(entry);
    }
}
