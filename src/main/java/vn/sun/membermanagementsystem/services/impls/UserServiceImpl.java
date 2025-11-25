package vn.sun.membermanagementsystem.services.impls;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.sun.membermanagementsystem.dto.request.UserCreateDTO;
import vn.sun.membermanagementsystem.dto.request.UserUpdateDTO;
import vn.sun.membermanagementsystem.dto.response.UserSummaryDTO;
import vn.sun.membermanagementsystem.entities.User;
import vn.sun.membermanagementsystem.enums.UserRole;
import vn.sun.membermanagementsystem.enums.UserStatus;
import vn.sun.membermanagementsystem.exception.DuplicateResourceException;
import vn.sun.membermanagementsystem.exception.ResourceNotFoundException;
import vn.sun.membermanagementsystem.mapper.UserMapper;
import vn.sun.membermanagementsystem.repositories.UserRepository;
import vn.sun.membermanagementsystem.services.UserService;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public UserSummaryDTO createUser(UserCreateDTO userCreateDTO) {
        log.info("Creating user with email: {}", userCreateDTO.getEmail());
        
        if (userRepository.existsByEmailAndNotDeleted(userCreateDTO.getEmail())) {
            log.error("Email already exists: {}", userCreateDTO.getEmail());
            throw new DuplicateResourceException("Email already exists: " + userCreateDTO.getEmail());
        }
        
        User user = new User();
        user.setName(userCreateDTO.getName());
        user.setEmail(userCreateDTO.getEmail());
        user.setPasswordHash(passwordEncoder.encode(userCreateDTO.getPassword()));
        user.setBirthday(userCreateDTO.getBirthday());
        user.setRole(userCreateDTO.getRole());
        user.setStatus(userCreateDTO.getStatus() != null ? userCreateDTO.getStatus() : UserStatus.ACTIVE);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        
        User savedUser = userRepository.save(user);
        log.info("User created successfully with ID: {}", savedUser.getId());

        // Implement logic to create associations with projects, team, skills, position
        
        return userMapper.toSummaryDTO(savedUser);
    }

    @Override
    @Transactional
    public UserSummaryDTO updateUser(UserUpdateDTO userUpdateDTO) {
        log.info("Updating user with ID: {}", userUpdateDTO.getId());
        // update sau
        return null;
    }

    @Override
    @Transactional
    public boolean deleteUser(Long userId) {
        log.info("Deleting user with ID: {}", userId);
        
        User user = userRepository.findByIdAndNotDeleted(userId)
                .orElseThrow(() -> {
                    log.error("User not found with ID: {}", userId);
                    return new ResourceNotFoundException("User not found with ID: " + userId);
                });
        
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);
        
        log.info("User deleted successfully with ID: {}", userId);
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public UserSummaryDTO getUserById(Long userId) {
        log.info("Getting user with ID: {}", userId);
        
        User user = userRepository.findByIdAndNotDeleted(userId)
                .orElseThrow(() -> {
                    log.error("User not found with ID: {}", userId);
                    return new ResourceNotFoundException("User not found with ID: " + userId);
                });
        
        return userMapper.toSummaryDTO(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSummaryDTO> getAllUsers() {
        log.info("Getting all users");
        
        List<User> users = userRepository.findAllNotDeleted();
        return userMapper.toSummaryDTOList(users);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSummaryDTO> getUsersByStatus(UserStatus status) {
        log.info("Getting users by status: {}", status);
        
        List<User> users = userRepository.findByStatusAndNotDeleted(status);
        return userMapper.toSummaryDTOList(users);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSummaryDTO> getUsersByRole(UserRole role) {
        log.info("Getting users by role: {}", role);
        
        List<User> users = userRepository.findByRoleAndNotDeleted(role);
        return userMapper.toSummaryDTOList(users);
    }

    @Override
    @Transactional(readOnly = true)
    public UserSummaryDTO getUserByEmail(String email) {
        log.info("Getting user by email: {}", email);
        
        User user = userRepository.findByEmailAndNotDeleted(email)
                .orElseThrow(() -> {
                    log.error("User not found with email: {}", email);
                    return new ResourceNotFoundException("User not found with email: " + email);
                });
        
        return userMapper.toSummaryDTO(user);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<UserSummaryDTO> getAllUsersWithPagination(Pageable pageable) {
        log.info("Getting all users with pagination: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        
        Page<User> users = userRepository.findAllNotDeleted(pageable);
        return users.map(userMapper::toSummaryDTO);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<UserSummaryDTO> searchUsers(String keyword, UserStatus status, UserRole role, Pageable pageable) {
        log.info("Searching users with keyword={}, status={}, role={}, page={}, size={}", 
                keyword, status, role, pageable.getPageNumber(), pageable.getPageSize());
        
        Page<User> users = userRepository.searchUsers(keyword, status, role, pageable);
        return users.map(userMapper::toSummaryDTO);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<UserSummaryDTO> searchUsersWithTeam(String keyword, UserStatus status, UserRole role, Long teamId, Pageable pageable) {
        log.info("Searching users with keyword={}, status={}, role={}, teamId={}, page={}, size={}", 
                keyword, status, role, teamId, pageable.getPageNumber(), pageable.getPageSize());
        
        Page<User> users = userRepository.searchUsersWithTeam(keyword, status, role, teamId, pageable);
        return users.map(userMapper::toSummaryDTO);
    }
}
