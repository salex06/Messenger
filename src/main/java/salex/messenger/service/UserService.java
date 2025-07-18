package salex.messenger.service;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import salex.messenger.dto.account.remove.RemoveUserRequest;
import salex.messenger.dto.account.update.about.UpdateAboutRequest;
import salex.messenger.dto.account.update.name.UpdateNameRequest;
import salex.messenger.dto.account.update.photo.UpdatePhotoRequest;
import salex.messenger.dto.account.update.surname.UpdateSurnameRequest;
import salex.messenger.dto.signup.SignUpRequest;
import salex.messenger.entity.User;
import salex.messenger.exception.UserNotFoundException;
import salex.messenger.exception.UsernameAlreadyExistsException;
import salex.messenger.repository.UserRepository;

@RequiredArgsConstructor
@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ImageStorageService imageStorageService;

    public Optional<User> findUser(Long userId) {
        return userRepository.findById(userId);
    }

    public Optional<User> findUser(String username) {
        return userRepository.findByUsername(username);
    }

    public User saveUser(SignUpRequest signUpRequest) {
        if (userRepository.existsByUsername(signUpRequest.username())) {
            throw new UsernameAlreadyExistsException(
                    "Пользователь с именем '" + signUpRequest.username() + "' уже существует");
        }

        User user = new User(
                null,
                signUpRequest.username(),
                passwordEncoder.encode(signUpRequest.password()),
                signUpRequest.name(),
                signUpRequest.surname(),
                signUpRequest.about(),
                signUpRequest.photo() == null
                        ? null
                        : imageStorageService.store(
                                signUpRequest.photo(),
                                ImageStorageService.generateFilename(signUpRequest.username(), signUpRequest.photo()),
                                imageStorageService.getUserPhotoDir()));
        return userRepository.save(user);
    }

    public void removeUser(RemoveUserRequest removeUserRequest) {
        User user = userRepository
                .findByUsername(removeUserRequest.username())
                .orElseThrow(() ->
                        new UserNotFoundException("Пользователь '" + removeUserRequest.username() + "' не найден"));

        userRepository.deleteById(user.getId());
    }

    public User updateName(String username, UpdateNameRequest request) {
        User user = userRepository
                .findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("Пользователь '" + username + "' не найден"));

        user.setName(request.newName());

        return userRepository.save(user);
    }

    public User updateSurname(String username, UpdateSurnameRequest request) {
        User user = userRepository
                .findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("Пользователь '" + username + "' не найден"));

        user.setSurname(request.newSurname());

        return userRepository.save(user);
    }

    public User updateAbout(String username, UpdateAboutRequest request) {
        User user = userRepository
                .findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("Пользователь '" + username + "' не найден"));

        user.setAbout(request.newAbout());

        return userRepository.save(user);
    }

    public User replacePhoto(String username, UpdatePhotoRequest request) {
        User user = userRepository
                .findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("Пользователь '" + username + "' не найден"));

        if (user.getPhotoPath() != null) {
            imageStorageService.remove(user.getPhotoPath(), imageStorageService.getUserPhotoDir());
            user.setPhotoPath(null);
        }

        String filepath = imageStorageService.store(
                request.newPhoto(),
                ImageStorageService.generateFilename(user.getUsername(), request.newPhoto()),
                imageStorageService.getUserPhotoDir());

        user.setPhotoPath(filepath);
        return userRepository.save(user);
    }

    public Page<User> getUsersByUsernamePattern(String query, Pageable pageable, String principalUsername) {
        return userRepository.suggestUsernames(query, pageable, principalUsername);
    }
}
