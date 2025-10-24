package io.github.srushti1125.aggregator.repository;

import io.github.srushti1125.aggregator.model.Article;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {

    // Find unsent articles published on or after a specific date
    // V V V CHANGE THIS LINE V V V
    List<Article> findBySentInDigestFalseAndPublishedDateGreaterThanEqual(LocalDate startDate);

    // Keep this for checking duplicates
    boolean existsByUrl(String url);

    // Optional: Delete the incorrect method definition if you added it
    // List<Article> findBySentInDigestFalseAndPublishedDate(LocalDate publishedDate);
}