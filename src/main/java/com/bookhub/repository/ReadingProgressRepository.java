package com.bookhub.repository;

import com.bookhub.domain.ReadingProgress;
import com.bookhub.domain.enums.ReadingStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ReadingProgressRepository extends JpaRepository<ReadingProgress, Long> {
    long countByStatus(ReadingStatus status);

    boolean existsByUserIdAndBookId(Long userId, Long bookId);

    Optional<ReadingProgress> findByUserIdAndBookId(Long userId, Long bookId);

    @EntityGraph(attributePaths = {"user", "book"})
    List<ReadingProgress> findAllByOrderByUpdatedAtDesc();

    @EntityGraph(attributePaths = {"user", "book"})
    List<ReadingProgress> findByUserIdOrderByUpdatedAtDesc(Long userId);

    @EntityGraph(attributePaths = {"user", "book"})
    List<ReadingProgress> findByStatusOrderByUpdatedAtDesc(ReadingStatus status);

    @Query(value = """
            select b.id, b.title, count(rp.id), avg(rp.progress_percent)
            from reading_progresses rp
            join books b on b.id = rp.book_id
            group by b.id, b.title
            order by count(rp.id) desc
            limit 5
            """, nativeQuery = true)
    List<Object[]> mostReadBooks();
}
