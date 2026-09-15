package et.engineeringdigest.journalApp.controller;

import et.engineeringdigest.journalApp.service.JournalEntryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/journal")

public class JournalEntryController {

    @Autowired
    private JournalEntryService journalEntryService;


}
