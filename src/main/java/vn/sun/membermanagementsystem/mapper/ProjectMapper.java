package vn.sun.membermanagementsystem.mapper;

import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import vn.sun.membermanagementsystem.dto.response.ProjectDTO;
import vn.sun.membermanagementsystem.dto.response.ProjectDetailDTO;
import vn.sun.membermanagementsystem.dto.response.ProjectLeadershipHistoryDTO;
import vn.sun.membermanagementsystem.dto.response.ProjectMemberDTO;
import vn.sun.membermanagementsystem.entities.Project;
import vn.sun.membermanagementsystem.entities.ProjectLeadershipHistory;
import vn.sun.membermanagementsystem.entities.ProjectMember;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProjectMapper {

    @Named("toBasicDTO")
    @Mapping(source = "team.id", target = "teamId")
    @Mapping(source = "team.name", target = "teamName")
    @Mapping(source = "status", target = "status", qualifiedByName = "statusToString")
    ProjectDTO toDTO(Project project);

    @IterableMapping(qualifiedByName = "toBasicDTO")
    List<ProjectDTO> toDTOList(List<Project> projects);


    @Mapping(source = "team.id", target = "teamId")
    @Mapping(source = "team.name", target = "teamName")
    @Mapping(source = "status", target = "status", qualifiedByName = "statusToString")
    @Mapping(source = "projectMembers", target = "members")
    @Mapping(source = "leadershipHistory", target = "leadershipHistory")
    ProjectDetailDTO toDetailDTO(Project project);


    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.name", target = "userName")
    @Mapping(source = "user.email", target = "userEmail")
    @Mapping(source = "status", target = "status", qualifiedByName = "memberStatusToString")
    ProjectMemberDTO toMemberDTO(ProjectMember projectMember);

    @Mapping(source = "leader.name", target = "leaderName")
    @Mapping(source = "leader.email", target = "leaderEmail")
    @Mapping(target = "isCurrent", expression = "java(history.getEndedAt() == null)")
    ProjectLeadershipHistoryDTO toLeadershipHistoryDTO(ProjectLeadershipHistory history);


    @Named("statusToString")
    default String statusToString(Project.ProjectStatus status) {
        return status != null ? status.name() : null;
    }

    @Named("memberStatusToString")
    default String memberStatusToString(ProjectMember.MemberStatus status) {
        return status != null ? status.name() : null;
    }
}