package vn.sun.membermanagementsystem.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.sun.membermanagementsystem.entities.Position;

import java.util.Optional;

@Repository
public interface PositionRepository extends JpaRepository<Position, Long> {
    
    @Query("SELECT p FROM Position p WHERE p.deletedAt IS NULL")
    Page<Position> findAllActive(Pageable pageable);
    
    @Query("SELECT p FROM Position p WHERE p.id = :id AND p.deletedAt IS NULL")
    Optional<Position> findByIdAndNotDeleted(@Param("id") Long id);
    
    @Query("SELECT COUNT(p) > 0 FROM Position p WHERE LOWER(p.name) = LOWER(:name) AND p.deletedAt IS NULL AND (:id IS NULL OR p.id != :id)")
    boolean existsByNameIgnoreCaseAndNotDeleted(@Param("name") String name, @Param("id") Long id);
    
    @Query("SELECT COUNT(p) > 0 FROM Position p WHERE LOWER(p.abbreviation) = LOWER(:abbreviation) AND p.deletedAt IS NULL AND (:id IS NULL OR p.id != :id)")
    boolean existsByAbbreviationIgnoreCaseAndNotDeleted(@Param("abbreviation") String abbreviation, @Param("id") Long id);
}
