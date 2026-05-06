package com.bookhub.repository;

import com.bookhub.domain.BookRating;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BookRatingRepository extends JpaRepository<BookRating, Long> {
    Optional<BookRating> findByBookIdAndUserId(Long bookId, Long userId);

    List<BookRating> findByBookId(Long bookId);

    @Query(value = """
            select b.id, b.title, count(br.id), avg(br.rating_value)
            from book_ratings br
            join books b on b.id = br.book_id
            group by b.id, b.title
            order by avg(br.rating_value) desc, count(br.id) desc
            limit 5
            """, nativeQuery = true)
    List<Object[]> highestRatedBooks();
}
