Digest Drop: Personalized Content Aggregator
This project is a full-stack, automated Java web service designed to combat information overload. It securely manages user profiles, aggregates content from multiple external sources based on user-defined keywords, and delivers a daily HTML email digest.

Key Features & Technical Highlights
This application demonstrates proficiency in several core enterprise Java concepts:

1. Secure Authentication & Web Layer
Full Security Implementation: Uses Spring Security to handle user registration, login, and access control.
Password Hashing: Passwords are secured using the BCrypt algorithm via Spring's PasswordEncoder.
Custom Authentication: Implements a custom JpaUserDetailsService to authenticate users against the database, showcasing mastery of the security chain override process.
Web Interface: User dashboard, registration, and login pages are built using Thymeleaf templates.

2. Automated Data Pipelines
Scheduled Tasks (@Scheduled): Content fetching runs hourly in the background, and the email delivery task runs daily at 8:00 AM (configurable via cron).
Multi-Source Aggregation: Simultaneously pulls, cleans, and processes data from 5 distinct sources including:
REST Clients: Hacker News, NewsAPI, and Reddit.
RSS Feeds: Times of India (TOI) and Medium (via ROME library).

3. Smart Filtering and Persistence
Personalized Filtering: Implements business logic to ensure users only receive articles that match their exact saved keywords.
Recency Filter: The system only sends content published within the last 7 days to ensure relevance.
Deduplication: Prevents sending the same article multiple times by tracking URLs via Spring Data JPA.

Project Architecture (Tech Stack)
| Component | Technology | Role |
| :--- | :--- | :--- |
| **Core Framework** | Spring Boot 3.x | Provides rapid setup and configuration. |
| **Persistence** | Spring Data JPA & H2 | Used for data modeling (User, Article) and local database storage. |
| **Security** | Spring Security | Handles authentication, authorization, and password hashing. |
| **Scheduling** | Spring Scheduler | Manages automated fetching and emailing jobs. |
| **Data Fetching** | RestTemplate/WebClient | Used for external REST API calls (JSON). |
| **RSS Parsing** | ROME Library | Handles reading and parsing RSS XML data. |
| **Frontend** | Thymeleaf | Server-side template engine for rendering dynamic HTML pages. |
| **Email** | Spring Mail | Used with MimeMessageHelper to send professional HTML digests. |


Local Setup and Run Instructions
Prerequisites
Java Development Kit (JDK) 17 or higher
Apache Maven (Used for dependency management)

1. Configuration (The application.properties File)
For security, the live API keys are excluded via .gitignore. You must create your own configuration file.
Obtain Keys: Get a free API key from newsapi.org and generate a 16-digit App Password for a dedicated Gmail account.
Create File: In your project's src/main/resources folder, create a file named application.properties.
Paste Configuration: Fill in your details:
# --- Mail Configuration ---
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=YOUR_SERVICE_EMAIL@gmail.com
spring.mail.password=YOUR_16_DIGIT_APP_PASSWORD 
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# --- API Keys ---
newsapi.key=YOUR_NEWSAPI_KEY_HERE

# --- H2 Database (Persisted) ---
spring.datasource.url=jdbc:h2:file:~/aggregator-db 
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
spring.jpa.hibernate.ddl-auto=update

2. Run the Application
Open your Terminal (or IntelliJ Terminal) in the root project directory (/aggregator).

Run the Spring Boot application:

Bash

./mvnw spring-boot:run
The server will start on port 8080


. Use the Web Interface
Open your web browser to: http://localhost:8080/register
Register a new account (Email and Password).
Log in with your new credentials.
Navigate to the Dashboard and save your desired keywords (e.g., Java, Kubernetes, AI).
Wait for the scheduled job (or restart the app to trigger the initial 5-second fetch job) to begin receiving personalized email digests.
