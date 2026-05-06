package com.bookhub.repository;

import com.bookhub.domain.Book;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookRepository extends JpaRepository<Book, Long> {
    @EntityGraph(attributePaths = "bookRatings")
    @Query("""
            select b from Book b
            where :term is null
               or lower(b.title) like lower(concat('%', :term, '%'))
               or lower(b.author) like lower(concat('%', :term, '%'))
               or lower(coalesce(b.genre, '')) like lower(concat('%', :term, '%'))
               or lower(b.description) like lower(concat('%', :term, '%'))
            order by b.title asc
            """)
    List<Book> search(@Param("term") String term);
}
