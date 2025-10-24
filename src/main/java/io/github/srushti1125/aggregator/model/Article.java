package io.github.srushti1125.aggregator.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate; // Import LocalDate

@Entity
@Getter
@Setter
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 1000) // Allow longer titles
    private String title;

    @Column(unique = true, nullable = false, length = 1000)
    private String url;

    @Column(length = 1000) // Allow long image URLs
    private String imageUrl; // Added imageUrl

    private LocalDate publishedDate; // Added publishedDate
    private String source;

    private boolean sentInDigest = false;
}