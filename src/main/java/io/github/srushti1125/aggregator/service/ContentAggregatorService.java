package io.github.srushti1125.aggregator.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.github.srushti1125.aggregator.model.Article;
import io.github.srushti1125.aggregator.model.User;
import io.github.srushti1125.aggregator.repository.ArticleRepository;
import io.github.srushti1125.aggregator.repository.UserRepository;

// --- Date/Time Imports ---
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit; // Import ChronoUnit if needed elsewhere, not strictly needed for this logic
import java.util.Date; // For RSS Date conversion

// --- RSS Imports ---
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

// --- Spring Imports ---
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

// --- JSON Record Classes ---
@JsonIgnoreProperties(ignoreUnknown = true)
record HnHit(String title, String url, Long created_at_i) {}
@JsonIgnoreProperties(ignoreUnknown = true)
record HnResponse(List<HnHit> hits) {}

@JsonIgnoreProperties(ignoreUnknown = true)
record NewsApiArticle(String title, String url, String urlToImage, String publishedAt) {}
@JsonIgnoreProperties(ignoreUnknown = true)
record NewsApiResponse(List<NewsApiArticle> articles) {}

@JsonIgnoreProperties(ignoreUnknown = true)
record RedditPostData(String title, String url, Double created_utc) {}
@JsonIgnoreProperties(ignoreUnknown = true)
record RedditPost(RedditPostData data) {}
@JsonIgnoreProperties(ignoreUnknown = true)
record RedditResponseData(List<RedditPost> children) {}
@JsonIgnoreProperties(ignoreUnknown = true)
record RedditResponse(RedditResponseData data) {}


@Service
public class ContentAggregatorService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ArticleRepository articleRepository;
    @Value("${newsapi.key}")
    private String newsApiKey;
    private final RestTemplate restTemplate = new RestTemplate();

    // Runs 5 seconds after startup (for testing), then hourly
    @Scheduled(initialDelay = 5000, fixedRate = 3600000)
    public void fetchContent() {
        System.out.println("LOG: fetchContent() TASK STARTED (fetching last 7 days where possible).");

        Set<String> allKeywords = new HashSet<>();
        for (User user : userRepository.findAll()) {
            if (user.getKeywords() != null) {
                allKeywords.addAll(user.getKeywords());
            }
        }
        System.out.println("LOG: Found keywords: " + allKeywords);
        if (allKeywords.isEmpty()) {
            System.out.println("LOG: No keywords found. Skipping fetch.");
            return;
        }

        // --- Calculate start date for filtering ---
        LocalDate sevenDaysAgoDate = LocalDate.now().minusDays(7);
        // Hacker News uses Unix timestamps (seconds since epoch)
        long sevenDaysAgoTimestamp = sevenDaysAgoDate.atStartOfDay(ZoneId.systemDefault()).toEpochSecond();

        for (String keyword : allKeywords) {
            if (keyword == null || keyword.trim().isEmpty()) continue;
            String trimmedKeyword = keyword.trim();
            // Pass the calculated date/timestamp to the relevant fetch methods
            fetchFromHackerNews(trimmedKeyword, sevenDaysAgoTimestamp);
            fetchFromNewsAPI(trimmedKeyword, sevenDaysAgoDate);
            fetchFromReddit(trimmedKeyword); // Reddit fetch remains unchanged (no reliable date filter)
            fetchFromToiRss(trimmedKeyword); // RSS fetch remains unchanged
            fetchFromMediumRss(trimmedKeyword); // RSS fetch remains unchanged
        }
        System.out.println("LOG: fetchContent() TASK FINISHED.");
    }

    // saveArticle method remains the same as the previous "complete code" version
    private void saveArticle(String title, String url, String imageUrl, LocalDate publishedDate, String source) {
        if (url != null && title != null && publishedDate != null && !articleRepository.existsByUrl(url)) {
            Article article = new Article();
            article.setTitle(title.length() > 999 ? title.substring(0, 999) : title);
            article.setUrl(url);
            article.setImageUrl(imageUrl);
            article.setPublishedDate(publishedDate);
            article.setSource(source); // <-- Save the source
            article.setSentInDigest(false);
            try {
                articleRepository.save(article);
                System.out.println("SUCCESS: Saved new ["+ source +"] article: " + article.getTitle());
            } catch (Exception e) {
                System.err.println("ERROR saving article '" + title + "': " + e.getMessage());
            }
        } else {
            System.out.println("LOG: Skipping duplicate, null data, or old article: " + (title != null ? title : "No Title"));
        }
    }

    /**
     * Source 1: Hacker News API - Modified for Date Range
     */
    // Added startTimestampSeconds parameter
    private void fetchFromHackerNews(String keyword, long startTimestampSeconds) {
        // Add numericFilters URL parameter to filter by creation timestamp
        String apiUrl = "http://hn.algolia.com/api/v1/search?query=" + URLEncoder.encode(keyword, StandardCharsets.UTF_8)
                + "&tags=story"
                + "&numericFilters=created_at_i>" + startTimestampSeconds; // Filter by timestamp

        System.out.println("LOG: Calling HackerNews API (last 7 days): " + apiUrl);
        try {
            HnResponse response = restTemplate.getForObject(apiUrl, HnResponse.class);
            if (response != null && response.hits() != null) {
                System.out.println("LOG: HackerNews success for '" + keyword + "'. Found " + response.hits().size() + " hits in last 7 days.");
                for (HnHit hit : response.hits()) {
                    LocalDate publishedDate = null;
                    if (hit.created_at_i() != null) {
                        // Convert Unix timestamp to LocalDate
                        publishedDate = Instant.ofEpochSecond(hit.created_at_i()).atZone(ZoneId.systemDefault()).toLocalDate();
                    }
                    saveArticle(hit.title(), hit.url(), null, publishedDate, "Hacker News"); // Add source // Pass date, no image
                }
            } else {
                System.out.println("LOG: HackerNews call for '" + keyword + "' returned null or no hits.");
            }
        } catch (Exception e) {
            System.err.println("CRITICAL ERROR fetching from HackerNews for '" + keyword + "': " + e.getMessage());
        }
    }

    /**
     * Source 2: NewsAPI.org - Modified for Date Range
     */
    // Added startDate parameter
    private void fetchFromNewsAPI(String keyword, LocalDate startDate) {
        // Format the start date for the API query
        String formattedStartDate = startDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
        // Add the 'from' URL parameter
        String apiUrl = "https://newsapi.org/v2/everything?q=" + URLEncoder.encode(keyword, StandardCharsets.UTF_8)
                + "&apiKey=" + newsApiKey
                + "&from=" + formattedStartDate; // Filter by start date

        System.out.println("LOG: Calling NewsAPI (last 7 days): " + apiUrl);
        try {
            NewsApiResponse response = restTemplate.getForObject(apiUrl, NewsApiResponse.class);
            if (response != null && response.articles() != null) {
                System.out.println("LOG: NewsAPI success for '" + keyword + "'. Found " + response.articles().size() + " hits in last 7 days.");
                for (NewsApiArticle article : response.articles()) {
                    LocalDate publishedDate = null;
                    if (article.publishedAt() != null) {
                        try {
                            // Parse the ISO date-time string
                            publishedDate = ZonedDateTime.parse(article.publishedAt(), DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDate();
                        } catch (DateTimeParseException ex1) {
                            try {
                                // Try parsing as Instant if the first format fails
                                publishedDate = ZonedDateTime.parse(article.publishedAt(), DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.systemDefault())).toLocalDate();
                            } catch(DateTimeParseException ex2) {
                                System.err.println("WARN: Could not parse NewsAPI date: " + article.publishedAt());
                            }
                        }
                    }
                    // Double-check date is within range before saving (API might include boundary slightly off)
                    if (publishedDate != null && !publishedDate.isBefore(startDate)) {
                        saveArticle(article.title(), article.url(), article.urlToImage(), publishedDate, "NewsAPI"); // Add source // Pass date and image
                    } else {
                        System.out.println("LOG: Skipping NewsAPI article - too old or no date: " + article.title());
                    }
                }
            } else {
                System.out.println("LOG: NewsAPI call for '" + keyword + "' returned null or no articles.");
            }
        } catch (Exception e) {
            System.err.println("CRITICAL ERROR fetching from NewsAPI for '" + keyword + "': " + e.getMessage());
        }
    }

    // --- fetchFromReddit, fetchFromToiRss, fetchFromMediumRss, fetchFromGoogleNewsRss ---
    // These methods remain the same as in the previous "complete code" version.
    // They don't have the added date/timestamp parameter in their signature
    // and don't modify their API URLs for date filtering.
    // They still parse the date if available and pass it to saveArticle.

    /**
     * Source 3: Reddit Search API - Unchanged (No reliable date filter)
     */
    private void fetchFromReddit(String keyword) {
        String apiUrl = "https://www.reddit.com/search.json?q=" + URLEncoder.encode(keyword, StandardCharsets.UTF_8);
        System.out.println("LOG: Calling Reddit API: " + apiUrl);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "java-aggregator:io.github.srushti1125:v1.0 (by /u/yourRedditUsername)"); // Be polite
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<RedditResponse> response = restTemplate.exchange(apiUrl, HttpMethod.GET, entity, RedditResponse.class);

            if (response.getBody() != null && response.getBody().data() != null && response.getBody().data().children() != null) {
                System.out.println("LOG: Reddit success for '" + keyword + "'. Found " + response.getBody().data().children().size() + " hits.");
                for (RedditPost post : response.getBody().data().children()) {
                    LocalDate publishedDate = null;
                    if (post.data() != null && post.data().created_utc() != null) {
                        publishedDate = Instant.ofEpochSecond(post.data().created_utc().longValue()).atZone(ZoneId.systemDefault()).toLocalDate();
                    }
                    saveArticle(post.data() != null ? post.data().title() : null,
                            post.data() != null ? post.data().url() : null,
                            null, publishedDate, "Reddit"); // Add source// Pass date, no image
                }
            } else {
                System.out.println("LOG: Reddit call for '" + keyword + "' returned null or no data/children.");
            }
        } catch (Exception e) {
            System.err.println("CRITICAL ERROR fetching from Reddit for '" + keyword + "': " + e.getMessage());
        }
    }

    /**
     * Source 4: The Times of India (via Google News RSS) - Unchanged
     */
    private void fetchFromToiRss(String keyword) {
        fetchFromGoogleNewsRss(keyword, "timesofindia.indiatimes.com");
    }

    /**
     * Source 5: Medium (via Google News RSS) - Unchanged
     */
    private void fetchFromMediumRss(String keyword) {
        fetchFromGoogleNewsRss(keyword, "medium.com");
    }

    /**
     * Helper method to fetch from Google News RSS - Unchanged (No reliable date filter)
     */
    private void fetchFromGoogleNewsRss(String keyword, String site) {
        XmlReader reader = null; // Declare reader outside try block
        try {
            String encodedQuery = URLEncoder.encode(keyword + " site:" + site, StandardCharsets.UTF_8);
            String rssUrl = "https://news.google.com/rss/search?q=" + encodedQuery + "&hl=en-IN&gl=IN&ceid=IN:en";
            System.out.println("LOG: Calling Google News RSS: " + rssUrl);

            SyndFeedInput input = new SyndFeedInput();
            URL feedUrl = new URL(rssUrl);
            reader = new XmlReader(feedUrl); // Initialize reader
            SyndFeed feed = input.build(reader);

            if (feed != null && feed.getEntries() != null) {
                System.out.println("LOG: Google News RSS success for '" + keyword + "' on site '" + site + "'. Found " + feed.getEntries().size() + " entries.");
                for (SyndEntry entry : feed.getEntries()) {
                    LocalDate publishedDate = null;
                    if (entry.getPublishedDate() != null) {
                        publishedDate = entry.getPublishedDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                    }
                    String sourceName = site.contains("timesofindia") ? "Times of India" : (site.contains("medium.com") ? "Medium" : "Google News");
                    saveArticle(entry.getTitle(), entry.getLink(), null, publishedDate, sourceName); // Add source // Pass date, no image
                }
            } else {
                System.out.println("LOG: Google News RSS call for '" + keyword + "' on site '" + site + "' returned null or no entries.");
            }
        } catch (Exception e) {
            System.err.println("CRITICAL ERROR fetching from Google News RSS for '" + keyword + "' on site '" + site + "': " + e.getMessage());
        } finally {
            // Ensure the reader is closed
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                    System.err.println("Error closing RSS reader: " + e.getMessage());
                }
            }
        }
    }
}