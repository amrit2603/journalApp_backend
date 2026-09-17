package et.engineeringdigest.journalApp.entity;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.PersistentPropertyTranslator;

import java.util.List;

@Document(collection = "users")
public class User {

    @Id
    private ObjectId id;

    private String userName;
    private String email;
    private String password;

    private boolean sentimentAnalysis;

    private List<String> roles;

    @DBRef
    private List<JournalEntry> journalEntries;
}
