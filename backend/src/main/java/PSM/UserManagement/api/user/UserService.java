package PSM.UserManagement.api.user;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import PSM.UserManagement.User;

@Service
public class UserService {
    private final UserRepository repository;

    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    public List<User> findAll() {
        return repository.findAll();
    }

    public User findById(UUID id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
    }

    public User create(User entity) {
        return repository.save(entity);
    }

    public User update(UUID id, User entity) {
        findById(id);
        return repository.save(entity);
    }

    public void delete(UUID id) {
        repository.deleteById(id);
    }
}
