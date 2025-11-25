package vn.sun.membermanagementsystem.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.sun.membermanagementsystem.dto.request.UserCreateDTO;
import vn.sun.membermanagementsystem.dto.request.UserUpdateDTO;
import vn.sun.membermanagementsystem.dto.response.UserSummaryDTO;
import vn.sun.membermanagementsystem.enums.UserRole;
import vn.sun.membermanagementsystem.enums.UserStatus;

import java.util.List;

public interface UserService {

    UserSummaryDTO createUser(UserCreateDTO userCreateDTO);
    UserSummaryDTO updateUser(UserUpdateDTO userUpdateDTO);
    boolean deleteUser(Long userId);
    UserSummaryDTO getUserById(Long userId);
    List<UserSummaryDTO> getAllUsers();
    List<UserSummaryDTO> getUsersByStatus(UserStatus status);
    List<UserSummaryDTO> getUsersByRole(UserRole role);
    UserSummaryDTO getUserByEmail(String email);
    
    Page<UserSummaryDTO> getAllUsersWithPagination(Pageable pageable);
    Page<UserSummaryDTO> searchUsers(String keyword, UserStatus status, UserRole role, Pageable pageable);
    Page<UserSummaryDTO> searchUsersWithTeam(String keyword, UserStatus status, UserRole role, Long teamId, Pageable pageable);
}
