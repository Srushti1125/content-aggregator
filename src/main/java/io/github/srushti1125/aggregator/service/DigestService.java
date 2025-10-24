package io.github.srushti1125.aggregator.service;

import io.github.srushti1125.aggregator.model.Article;
import io.github.srushti1125.aggregator.model.User;
import io.github.srushti1125.aggregator.repository.ArticleRepository;
import io.github.srushti1125.aggregator.repository.UserRepository;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value; // Import Value
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList; // Import ArrayList
import java.util.HashSet;   // Import HashSet
import java.util.List;
import java.util.Set;       // Import Set
import java.util.stream.Collectors;

@Service
public class DigestService {

    @Autowired
    private JavaMailSender mailSender;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ArticleRepository articleRepository;

    // Get 'from' address from properties for consistency
    @Value("${spring.mail.username}")
    private String mailFromAddress;

    // Test: 1 min delay, then hourly. Prod: Daily at 8 AM
//    @Scheduled(initialDelay = 60000, fixedRate = 3600000)
    @Scheduled(cron = "0 0 8 * * ?")
    public void sendDailyDigest() {
        System.out.println("Preparing daily digests for the last 7 days...");
        LocalDate sevenDaysAgo = LocalDate.now().minusDays(7);

        // Find all potentially relevant articles (unsent, within last 7 days)
        List<Article> articlesToSend = articleRepository.findBySentInDigestFalseAndPublishedDateGreaterThanEqual(sevenDaysAgo);
        if (articlesToSend.isEmpty()) {
            System.out.println("No new articles from the last 7 days found.");
            return;
        }
        System.out.println("Found " + articlesToSend.size() + " potential articles from the last 7 days.");

        List<User> users = userRepository.findAll();
        if (users.isEmpty()) {
            System.out.println("No users registered to send digests to.");
            // Don't mark as sent if no users, maybe someone registers later
            return;
        }

        int totalEmailsSent = 0;
        // Keep track of articles successfully included in ANY email this run
        Set<Article> articlesSuccessfullySent = new HashSet<>();

        for (User user : users) {
            // Skip users without keywords
            if (user.getKeywords() == null || user.getKeywords().isEmpty()) {
                System.out.println("Skipping user " + user.getEmail() + " as they have no keywords.");
                continue;
            }

            // Filter the potential articles to find ones relevant JUST to this user
            List<Article> relevantArticlesForUser = articlesToSend.stream()
                    .filter(article -> matchesUserKeywords(article, user))
                    .collect(Collectors.toList());

            if (!relevantArticlesForUser.isEmpty()) {
                // Try to send the email
                boolean emailSent = sendDigestEmail(user, relevantArticlesForUser);
                if (emailSent) {
                    // If sending succeeded, add the articles from THIS email to the set
                    articlesSuccessfullySent.addAll(relevantArticlesForUser);
                    totalEmailsSent++;
                }
            } else {
                System.out.println("No relevant articles found in last 7 days for user: " + user.getEmail());
            }
        }

        // After looping through all users, mark ONLY the articles that were successfully sent
        if (!articlesSuccessfullySent.isEmpty()) {
            markArticlesAsSent(new ArrayList<>(articlesSuccessfullySent)); // Convert Set to List for saveAll
        } else {
            System.out.println("No relevant articles were sent in any digests this run.");
        }
        System.out.println("Digest process finished. Sent " + totalEmailsSent + " emails.");
    }

    // Helper method to mark a list of articles as sent
    private void markArticlesAsSent(List<Article> articles) {
        if (articles != null && !articles.isEmpty()) { // Add null check
            for (Article article : articles) {
                article.setSentInDigest(true);
            }
            articleRepository.saveAll(articles);
            System.out.println("Marked " + articles.size() + " articles as sent.");
        }
    }

    // Helper method to check if article title matches user keywords
    private boolean matchesUserKeywords(Article article, User user) {
        // Add null checks for safety
        if (article == null || article.getTitle() == null || user == null || user.getKeywords() == null) {
            return false;
        }
        String titleLower = article.getTitle().toLowerCase();
        for (String keyword : user.getKeywords()) {
            // Check keyword isn't null/empty before matching
            if (keyword != null && !keyword.trim().isEmpty() && titleLower.contains(keyword.trim().toLowerCase())) {
                return true; // Match found
            }
        }
        return false; // No match found
    }

    // Updated sendDigestEmail with improved HTML and boolean return
    private boolean sendDigestEmail(User user, List<Article> articles) {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "utf-8"); // true = multipart, utf-8 encoding
            helper.setTo(user.getEmail());
            helper.setFrom(mailFromAddress); // Use configured 'from' address
            helper.setSubject("Your Tech Digest"); // Keep subject concise

            // --- Build Improved HTML ---
            StringBuilder htmlBody = new StringBuilder();
            // Add Doctype, basic structure, and CSS styles in <head>
            htmlBody.append("<!DOCTYPE html><html><head><meta charset='utf-8'>")
                    .append("<style>")
                    .append("body{font-family: Arial, sans-serif; line-height: 1.6; color: #333; background-color: #f8f8f8; padding: 20px;} ")
                    .append(".container{max-width: 600px; margin: 0 auto; background-color: #fff; padding: 30px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);} ")
                    .append("h1{color: #2c3e50; border-bottom: 2px solid #3498db; padding-bottom: 10px; margin-top: 0;} ")
                    .append("h2{color: #3498db; margin-top: 30px; font-size: 1.2em;} ")
                    .append(".article-item{margin-bottom: 25px; padding-bottom: 15px; border-bottom: 1px solid #eee; overflow: hidden;} ") // overflow:hidden acts as clearfix
                    .append(".article-item:last-child{border-bottom: none;} ") // Remove border on last item
                    .append(".article-img{float: right; margin-left: 20px; margin-bottom: 10px; width: 120px; height: 80px; object-fit: cover; border-radius: 4px; border: 1px solid #ddd;} ") // Style for image
                    .append(".article-content{overflow: hidden;} ") // Div containing text, allows float wrapping
                    .append(".article-title a{text-decoration: none; color: #1a0dab; font-size: 1.1em; font-weight: bold; display: block; margin-bottom: 5px;} ") // Title style
                    .append(".article-title a:hover{text-decoration: underline;} ")
                    .append(".article-source{font-size: 0.85em; color: #555; margin-top: 5px;}") // Source style
                    .append(".footer{font-size: 0.8em; color: #888; margin-top: 30px; text-align: center; border-top: 1px solid #eee; padding-top: 15px;}") // Footer style
                    .append("</style></head><body>");

            // Start main content container
            htmlBody.append("<div class='container'>");
            htmlBody.append("<h1>Your Tech Digest</h1>");
            htmlBody.append("<h2>Highlights from the Last 7 Days</h2>");

            // Loop through articles for this user
            for (Article article : articles) {
                htmlBody.append("<div class='article-item'>"); // Start article item div

                // Add image if available
                if (article.getImageUrl() != null && !article.getImageUrl().isEmpty()) {
                    htmlBody.append("<img src='").append(article.getImageUrl()).append("' alt='' class='article-img' />"); // Empty alt is acceptable for decorative images
                }

                // Add content div (for text)
                htmlBody.append("<div class='article-content'>");

                // Add title as a link
                htmlBody.append("<div class='article-title'><a href='").append(article.getUrl()).append("' target='_blank'>"); // target='_blank' opens in new tab
                htmlBody.append(article.getTitle() != null ? article.getTitle() : "No Title"); // Handle potential null title gracefully
                htmlBody.append("</a></div>");

                // Add source if available
                if (article.getSource() != null && !article.getSource().isEmpty()) {
                    htmlBody.append("<div class='article-source'>Source: ").append(article.getSource()).append("</div>");
                }

                htmlBody.append("</div>"); // End article-content
                htmlBody.append("</div>"); // End article-item
            }

            // Add footer
            htmlBody.append("<div class='footer'><p>Generated by Aggregator Bot</p></div>");
            htmlBody.append("</div>"); // End container
            htmlBody.append("</body></html>");
            // --- End HTML ---

            // Set the content as HTML
            helper.setText(htmlBody.toString(), true); // true indicates this is HTML

            // Send the email
            mailSender.send(mimeMessage);
            System.out.println("Sent HTML digest to: " + user.getEmail());
            return true; // Email sent successfully

        } catch (MessagingException e) {
            System.err.println("CRITICAL ERROR sending HTML email to " + user.getEmail() + ": " + e.getMessage());
            return false; // Email sending failed
        } catch (Exception e) {
            // Catch any other unexpected errors during email creation/sending
            System.err.println("Unexpected error sending HTML email to " + user.getEmail() + ": " + e.getMessage());
            e.printStackTrace(); // Print stack trace for debugging
            return false; // Email sending failed
        }
    }
}