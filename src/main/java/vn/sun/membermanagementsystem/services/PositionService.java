package vn.sun.membermanagementsystem.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.sun.membermanagementsystem.dto.request.CreatePositionRequest;
import vn.sun.membermanagementsystem.dto.request.UpdatePositionRequest;
import vn.sun.membermanagementsystem.dto.response.PositionDTO;

public interface PositionService {
    
    Page<PositionDTO> getAllPositions(Pageable pageable);
    
    PositionDTO getPositionById(Long id);
    
    PositionDTO createPosition(CreatePositionRequest request);
    
    PositionDTO updatePosition(Long id, UpdatePositionRequest request);
    
    void deletePosition(Long id);
}
