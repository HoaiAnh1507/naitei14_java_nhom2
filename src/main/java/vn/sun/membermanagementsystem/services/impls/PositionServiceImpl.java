package vn.sun.membermanagementsystem.services.impls;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.sun.membermanagementsystem.dto.request.CreatePositionRequest;
import vn.sun.membermanagementsystem.dto.request.UpdatePositionRequest;
import vn.sun.membermanagementsystem.dto.response.PositionDTO;
import vn.sun.membermanagementsystem.entities.Position;
import vn.sun.membermanagementsystem.exception.DuplicateResourceException;
import vn.sun.membermanagementsystem.exception.ResourceNotFoundException;
import vn.sun.membermanagementsystem.mapper.PositionMapper;
import vn.sun.membermanagementsystem.repositories.PositionRepository;
import vn.sun.membermanagementsystem.services.PositionService;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PositionServiceImpl implements PositionService {
    
    private final PositionRepository positionRepository;
    private final PositionMapper positionMapper;
    
    @Override
    public Page<PositionDTO> getAllPositions(Pageable pageable) {
        log.info("Getting all positions with pagination: {}", pageable);
        Page<Position> positions = positionRepository.findAllActive(pageable);
        return positions.map(positionMapper::toDTO);
    }
    
    @Override
    public PositionDTO getPositionById(Long id) {
        log.info("Getting position by id: {}", id);
        Position position = positionRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> new ResourceNotFoundException("Position not found with id: " + id));
        return positionMapper.toDTO(position);
    }
    
    @Override
    @Transactional
    public PositionDTO createPosition(CreatePositionRequest request) {
        log.info("Creating position with name: {}", request.getName());
        
        validatePositionUniqueness(request.getName(), request.getAbbreviation(), null);
        
        Position position = positionMapper.toEntity(request);
        Position savedPosition = positionRepository.save(position);
        
        log.info("Position created successfully with id: {}", savedPosition.getId());
        return positionMapper.toDTO(savedPosition);
    }
    
    @Override
    @Transactional
    public PositionDTO updatePosition(Long id, UpdatePositionRequest request) {
        log.info("Updating position with id: {}", id);
        
        Position position = positionRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> new ResourceNotFoundException("Position not found with id: " + id));
        
        validatePositionUniqueness(request.getName(), request.getAbbreviation(), id);
        
        positionMapper.updateEntity(request, position);
        Position updatedPosition = positionRepository.save(position);
        
        log.info("Position updated successfully with id: {}", id);
        return positionMapper.toDTO(updatedPosition);
    }
    
    @Override
    @Transactional
    public void deletePosition(Long id) {
        log.info("Deleting position with id: {}", id);
        
        Position position = positionRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> new ResourceNotFoundException("Position not found with id: " + id));
        
        position.setDeletedAt(LocalDateTime.now());
        positionRepository.save(position);
        
        log.info("Position deleted successfully with id: {}", id);
    }
    
    private void validatePositionUniqueness(String name, String abbreviation, Long excludeId) {
        if (positionRepository.existsByNameIgnoreCaseAndNotDeleted(name, excludeId)) {
            throw new DuplicateResourceException("Position with name '" + name + "' already exists");
        }
        
        if (positionRepository.existsByAbbreviationIgnoreCaseAndNotDeleted(abbreviation, excludeId)) {
            throw new DuplicateResourceException("Position with abbreviation '" + abbreviation + "' already exists");
        }
    }
}
