package et.engineeringdigest.journalApp.service;

import et.engineeringdigest.journalApp.entity.User;
import et.engineeringdigest.journalApp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public User findByUserName(String userName){
        return userRepository.findByUserName(userName).orElse(null);
    }
    public void saveUser(User user){
        userRepository.save(user);
    }
}
