package vn.sun.membermanagementsystem.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import vn.sun.membermanagementsystem.dto.request.CreatePositionRequest;
import vn.sun.membermanagementsystem.dto.request.UpdatePositionRequest;
import vn.sun.membermanagementsystem.dto.response.PositionDTO;
import vn.sun.membermanagementsystem.entities.Position;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PositionMapper {
    
    PositionDTO toDTO(Position position);

    List<PositionDTO> toDTOList(List<Position> positions);

    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "userPositionHistories", ignore = true)
    Position toEntity(PositionDTO positionDTO);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "userPositionHistories", ignore = true)
    Position toEntity(CreatePositionRequest request);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "userPositionHistories", ignore = true)
    void updateEntity(UpdatePositionRequest request, @MappingTarget Position position);
}
