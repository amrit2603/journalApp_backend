package net.engineeringdigest.journalApp.config;

import net.engineeringdigest.journalApp.entity.User;
import net.engineeringdigest.journalApp.service.UserService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DemoUserConfigTests {
    @Test void existingAccountIsNeverOverwritten() throws Exception {
        var users = mock(UserService.class);
        when(users.findByUserName("demo")).thenReturn(new User());
        new DemoUserConfig().demoUser(users,"demo","synthetic-test-password").run();
        verify(users,never()).saveNewUser(any());
    }

    @Test void invalidDemoConfigurationDoesNotTouchDatabase() {
        var users = mock(UserService.class);
        assertThrows(IllegalArgumentException.class,
                () -> new DemoUserConfig().demoUser(users,"demo","short").run());
        verifyNoInteractions(users);
    }

    @Test void newDemoAccountHasAnInitializedEntryList() throws Exception {
        var users = mock(UserService.class);
        when(users.saveNewUser(any())).thenReturn(true);
        new DemoUserConfig().demoUser(users,"demo","synthetic-test-password").run();
        verify(users).saveNewUser(argThat(user -> user.getUserName().equals("demo") && user.getJournalEntries().isEmpty()));
    }
}
