package vn.sun.membermanagementsystem.services.impls;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import vn.sun.membermanagementsystem.dto.request.CreatePositionRequest;
import vn.sun.membermanagementsystem.dto.request.UpdatePositionRequest;
import vn.sun.membermanagementsystem.dto.response.PositionDTO;
import vn.sun.membermanagementsystem.entities.Position;
import vn.sun.membermanagementsystem.exception.DuplicateResourceException;
import vn.sun.membermanagementsystem.exception.ResourceNotFoundException;
import vn.sun.membermanagementsystem.mapper.PositionMapper;
import vn.sun.membermanagementsystem.repositories.PositionRepository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PositionServiceImpl Unit Tests")
class PositionServiceImplTest {

    @Mock
    private PositionRepository positionRepository;

    @Mock
    private PositionMapper positionMapper;

    @InjectMocks
    private PositionServiceImpl positionService;

    private Position testPosition;
    private CreatePositionRequest createPositionRequest;
    private UpdatePositionRequest updatePositionRequest;
    private PositionDTO positionDTO;

    @BeforeEach
    void setUp() {
        testPosition = new Position();
        testPosition.setId(1L);
        testPosition.setName("Software Developer");
        testPosition.setAbbreviation("DEV");
        testPosition.setCreatedAt(LocalDateTime.now());
        testPosition.setUpdatedAt(LocalDateTime.now());
        testPosition.setDeletedAt(null);

        createPositionRequest = CreatePositionRequest.builder()
                .name("Software Developer")
                .abbreviation("DEV")
                .build();

        updatePositionRequest = UpdatePositionRequest.builder()
                .name("Senior Software Developer")
                .abbreviation("SR-DEV")
                .build();

        positionDTO = PositionDTO.builder()
                .id(1L)
                .name("Software Developer")
                .abbreviation("DEV")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Get all positions with pagination successfully")
    void testGetAllPositions_Success() {
        // Arrange
        Position position2 = new Position();
        position2.setId(2L);
        position2.setName("Project Manager");
        position2.setAbbreviation("PM");

        List<Position> positions = Arrays.asList(testPosition, position2);
        Page<Position> positionPage = new PageImpl<>(positions);
        Pageable pageable = PageRequest.of(0, 10, Sort.by("name").ascending());

        PositionDTO positionDTO2 = PositionDTO.builder()
                .id(2L)
                .name("Project Manager")
                .abbreviation("PM")
                .build();

        when(positionRepository.findAllActive(pageable)).thenReturn(positionPage);
        when(positionMapper.toDTO(testPosition)).thenReturn(positionDTO);
        when(positionMapper.toDTO(position2)).thenReturn(positionDTO2);

        // Act
        Page<PositionDTO> result = positionService.getAllPositions(pageable);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        verify(positionRepository, times(1)).findAllActive(pageable);
        verify(positionMapper, times(2)).toDTO(any(Position.class));
    }

    @Test
    @DisplayName("Get all positions returns empty page when no positions exist")
    void testGetAllPositions_EmptyPage() {
        // Arrange
        Page<Position> emptyPage = new PageImpl<>(List.of());
        Pageable pageable = PageRequest.of(0, 10);

        when(positionRepository.findAllActive(pageable)).thenReturn(emptyPage);

        // Act
        Page<PositionDTO> result = positionService.getAllPositions(pageable);

        // Assert
        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        assertEquals(0, result.getTotalElements());
        verify(positionRepository, times(1)).findAllActive(pageable);
    }

    @Test
    @DisplayName("Get position by ID successfully")
    void testGetPositionById_Success() {
        // Arrange
        when(positionRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(testPosition));
        when(positionMapper.toDTO(testPosition)).thenReturn(positionDTO);

        // Act
        PositionDTO result = positionService.getPositionById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Software Developer", result.getName());
        assertEquals("DEV", result.getAbbreviation());
        verify(positionRepository, times(1)).findByIdAndNotDeleted(1L);
        verify(positionMapper, times(1)).toDTO(testPosition);
    }

    @Test
    @DisplayName("Get position by ID not found should throw ResourceNotFoundException")
    void testGetPositionById_NotFound_ThrowsException() {
        // Arrange
        when(positionRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> positionService.getPositionById(1L)
        );

        assertTrue(exception.getMessage().contains("Position not found with id"));
        verify(positionRepository, times(1)).findByIdAndNotDeleted(1L);
        verify(positionMapper, never()).toDTO(any(Position.class));
    }

    @Test
    @DisplayName("Create position successfully")
    void testCreatePosition_Success() {
        // Arrange
        when(positionRepository.existsByNameIgnoreCaseAndNotDeleted("Software Developer", null))
                .thenReturn(false);
        when(positionRepository.existsByAbbreviationIgnoreCaseAndNotDeleted("DEV", null))
                .thenReturn(false);
        when(positionMapper.toEntity(createPositionRequest)).thenReturn(testPosition);
        when(positionRepository.save(testPosition)).thenReturn(testPosition);
        when(positionMapper.toDTO(testPosition)).thenReturn(positionDTO);

        // Act
        PositionDTO result = positionService.createPosition(createPositionRequest);

        // Assert
        assertNotNull(result);
        assertEquals("Software Developer", result.getName());
        assertEquals("DEV", result.getAbbreviation());
        verify(positionRepository, times(1)).existsByNameIgnoreCaseAndNotDeleted("Software Developer", null);
        verify(positionRepository, times(1)).existsByAbbreviationIgnoreCaseAndNotDeleted("DEV", null);
        verify(positionRepository, times(1)).save(testPosition);
        verify(positionMapper, times(1)).toDTO(testPosition);
    }

    @Test
    @DisplayName("Create position with duplicate name should throw DuplicateResourceException")
    void testCreatePosition_DuplicateName_ThrowsException() {
        // Arrange
        when(positionRepository.existsByNameIgnoreCaseAndNotDeleted("Software Developer", null))
                .thenReturn(true);

        // Act & Assert
        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> positionService.createPosition(createPositionRequest)
        );

        assertTrue(exception.getMessage().contains("Position with name"));
        assertTrue(exception.getMessage().contains("already exists"));
        verify(positionRepository, times(1)).existsByNameIgnoreCaseAndNotDeleted("Software Developer", null);
        verify(positionRepository, never()).save(any(Position.class));
    }

    @Test
    @DisplayName("Create position with duplicate abbreviation should throw DuplicateResourceException")
    void testCreatePosition_DuplicateAbbreviation_ThrowsException() {
        // Arrange
        when(positionRepository.existsByNameIgnoreCaseAndNotDeleted("Software Developer", null))
                .thenReturn(false);
        when(positionRepository.existsByAbbreviationIgnoreCaseAndNotDeleted("DEV", null))
                .thenReturn(true);

        // Act & Assert
        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> positionService.createPosition(createPositionRequest)
        );

        assertTrue(exception.getMessage().contains("Position with abbreviation"));
        assertTrue(exception.getMessage().contains("already exists"));
        verify(positionRepository, times(1)).existsByAbbreviationIgnoreCaseAndNotDeleted("DEV", null);
        verify(positionRepository, never()).save(any(Position.class));
    }

    @Test
    @DisplayName("Update position successfully")
    void testUpdatePosition_Success() {
        // Arrange
        when(positionRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(testPosition));
        when(positionRepository.existsByNameIgnoreCaseAndNotDeleted("Senior Software Developer", 1L))
                .thenReturn(false);
        when(positionRepository.existsByAbbreviationIgnoreCaseAndNotDeleted("SR-DEV", 1L))
                .thenReturn(false);
        doNothing().when(positionMapper).updateEntity(updatePositionRequest, testPosition);
        when(positionRepository.save(testPosition)).thenReturn(testPosition);
        
        PositionDTO updatedDTO = PositionDTO.builder()
                .id(1L)
                .name("Senior Software Developer")
                .abbreviation("SR-DEV")
                .build();
        when(positionMapper.toDTO(testPosition)).thenReturn(updatedDTO);

        // Act
        PositionDTO result = positionService.updatePosition(1L, updatePositionRequest);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(positionRepository, times(1)).findByIdAndNotDeleted(1L);
        verify(positionMapper, times(1)).updateEntity(updatePositionRequest, testPosition);
        verify(positionRepository, times(1)).save(testPosition);
    }

    @Test
    @DisplayName("Update position not found should throw ResourceNotFoundException")
    void testUpdatePosition_NotFound_ThrowsException() {
        // Arrange
        when(positionRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> positionService.updatePosition(1L, updatePositionRequest)
        );

        assertTrue(exception.getMessage().contains("Position not found with id"));
        verify(positionRepository, times(1)).findByIdAndNotDeleted(1L);
        verify(positionRepository, never()).save(any(Position.class));
    }

    @Test
    @DisplayName("Update position with duplicate name should throw DuplicateResourceException")
    void testUpdatePosition_DuplicateName_ThrowsException() {
        // Arrange
        when(positionRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(testPosition));
        when(positionRepository.existsByNameIgnoreCaseAndNotDeleted("Senior Software Developer", 1L))
                .thenReturn(true);

        // Act & Assert
        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> positionService.updatePosition(1L, updatePositionRequest)
        );

        assertTrue(exception.getMessage().contains("Position with name"));
        verify(positionRepository, never()).save(any(Position.class));
    }

    @Test
    @DisplayName("Update position with duplicate abbreviation should throw DuplicateResourceException")
    void testUpdatePosition_DuplicateAbbreviation_ThrowsException() {
        // Arrange
        when(positionRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(testPosition));
        when(positionRepository.existsByNameIgnoreCaseAndNotDeleted("Senior Software Developer", 1L))
                .thenReturn(false);
        when(positionRepository.existsByAbbreviationIgnoreCaseAndNotDeleted("SR-DEV", 1L))
                .thenReturn(true);

        // Act & Assert
        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> positionService.updatePosition(1L, updatePositionRequest)
        );

        assertTrue(exception.getMessage().contains("Position with abbreviation"));
        verify(positionRepository, never()).save(any(Position.class));
    }

    @Test
    @DisplayName("Delete position successfully")
    void testDeletePosition_Success() {
        // Arrange
        when(positionRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(testPosition));
        when(positionRepository.save(any(Position.class))).thenReturn(testPosition);

        // Act
        positionService.deletePosition(1L);

        // Assert
        verify(positionRepository, times(1)).findByIdAndNotDeleted(1L);
        verify(positionRepository, times(1)).save(argThat(position -> 
            position.getDeletedAt() != null
        ));
    }

    @Test
    @DisplayName("Delete position sets deletedAt timestamp")
    void testDeletePosition_SetsDeletedAt() {
        // Arrange
        when(positionRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(testPosition));
        when(positionRepository.save(any(Position.class))).thenAnswer(invocation -> {
            Position position = invocation.getArgument(0);
            assertNotNull(position.getDeletedAt());
            return position;
        });

        // Act
        positionService.deletePosition(1L);

        // Assert
        assertNotNull(testPosition.getDeletedAt());
        verify(positionRepository, times(1)).save(testPosition);
    }

    @Test
    @DisplayName("Delete non-existing position should throw ResourceNotFoundException")
    void testDeletePosition_NotFound_ThrowsException() {
        // Arrange
        when(positionRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> positionService.deletePosition(1L)
        );

        assertTrue(exception.getMessage().contains("Position not found with id"));
        verify(positionRepository, times(1)).findByIdAndNotDeleted(1L);
        verify(positionRepository, never()).save(any(Position.class));
    }

    @Test
    @DisplayName("Validation checks name case-insensitively")
    void testValidation_CaseInsensitiveName() {
        // Arrange
        CreatePositionRequest request = CreatePositionRequest.builder()
                .name("SOFTWARE DEVELOPER")
                .abbreviation("DEV2")
                .build();

        when(positionRepository.existsByNameIgnoreCaseAndNotDeleted("SOFTWARE DEVELOPER", null))
                .thenReturn(true);

        // Act & Assert
        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> positionService.createPosition(request)
        );

        assertTrue(exception.getMessage().contains("already exists"));
        verify(positionRepository, times(1))
                .existsByNameIgnoreCaseAndNotDeleted("SOFTWARE DEVELOPER", null);
    }

    @Test
    @DisplayName("Validation checks abbreviation case-insensitively")
    void testValidation_CaseInsensitiveAbbreviation() {
        // Arrange
        CreatePositionRequest request = CreatePositionRequest.builder()
                .name("Test Position")
                .abbreviation("dev")
                .build();

        when(positionRepository.existsByNameIgnoreCaseAndNotDeleted("Test Position", null))
                .thenReturn(false);
        when(positionRepository.existsByAbbreviationIgnoreCaseAndNotDeleted("dev", null))
                .thenReturn(true);

        // Act & Assert
        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> positionService.createPosition(request)
        );

        assertTrue(exception.getMessage().contains("already exists"));
        verify(positionRepository, times(1))
                .existsByAbbreviationIgnoreCaseAndNotDeleted("dev", null);
    }

    @Test
    @DisplayName("Update allows same position to keep its own name")
    void testUpdate_AllowsSamePositionName() {
        // Arrange
        UpdatePositionRequest request = UpdatePositionRequest.builder()
                .name("Software Developer")
                .abbreviation("DEV")
                .build();

        when(positionRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(testPosition));
        when(positionRepository.existsByNameIgnoreCaseAndNotDeleted("Software Developer", 1L))
                .thenReturn(false); // Same position, so not duplicate
        when(positionRepository.existsByAbbreviationIgnoreCaseAndNotDeleted("DEV", 1L))
                .thenReturn(false);
        doNothing().when(positionMapper).updateEntity(request, testPosition);
        when(positionRepository.save(testPosition)).thenReturn(testPosition);
        when(positionMapper.toDTO(testPosition)).thenReturn(positionDTO);

        // Act
        PositionDTO result = positionService.updatePosition(1L, request);

        // Assert
        assertNotNull(result);
        verify(positionRepository, times(1)).save(testPosition);
    }

    @Test
    @DisplayName("Pagination works correctly with different page sizes")
    void testGetAllPositions_DifferentPageSizes() {
        // Arrange
        List<Position> positions = Arrays.asList(testPosition);
        Page<Position> page = new PageImpl<>(positions, PageRequest.of(0, 5), 1);
        
        when(positionRepository.findAllActive(any(Pageable.class))).thenReturn(page);
        when(positionMapper.toDTO(any(Position.class))).thenReturn(positionDTO);

        // Act
        Page<PositionDTO> result = positionService.getAllPositions(PageRequest.of(0, 5));

        // Assert
        assertNotNull(result);
        assertEquals(5, result.getSize());
        assertEquals(1, result.getTotalElements());
        verify(positionRepository, times(1)).findAllActive(any(Pageable.class));
    }

    @Test
    @DisplayName("Sorting works correctly")
    void testGetAllPositions_WithSorting() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10, Sort.by("name").descending());
        List<Position> positions = Arrays.asList(testPosition);
        Page<Position> page = new PageImpl<>(positions, pageable, 1);
        
        when(positionRepository.findAllActive(pageable)).thenReturn(page);
        when(positionMapper.toDTO(testPosition)).thenReturn(positionDTO);

        // Act
        Page<PositionDTO> result = positionService.getAllPositions(pageable);

        // Assert
        assertNotNull(result);
        assertEquals(Sort.Direction.DESC, result.getSort().getOrderFor("name").getDirection());
        verify(positionRepository, times(1)).findAllActive(pageable);
    }
}
