package com.learnhub.repo;

import com.learnhub.domain.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LessonRepository extends JpaRepository<Lesson, Long> {
    Optional<Lesson> findBySlug(String slug);

    @Query("select l from Lesson l join fetch l.stage s order by s.orderNo, l.orderNo")
    List<Lesson> findAllOrdered();

    @Query("""
           select l from Lesson l join fetch l.stage s
           where lower(l.title) like lower(concat('%', :q, '%'))
              or lower(l.summary) like lower(concat('%', :q, '%'))
           order by s.orderNo, l.orderNo
           """)
    List<Lesson> search(@Param("q") String q);
}
