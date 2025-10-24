
-----

#  Digest Drop: Personalized Content Aggregator

[](https://www.java.com/)
[](https://spring.io/projects/spring-boot)
[](https://spring.io/projects/spring-security)
[](https://opensource.org/licenses/MIT)

This project is a full-stack, automated Java web service designed to combat information overload. It securely manages user profiles, aggregates content from multiple external sources based on **user-defined keywords**, and delivers a daily **HTML email digest**.

-----

## ✨ Key Features & Technical Highlights

This application demonstrates proficiency in several core enterprise Java concepts:

1.  **Secure Authentication & Web Layer:** Full security implementation uses **Spring Security** to handle user registration, login, and access control. Passwords are secured using **BCrypt hashing**.
2.  **Automated Data Pipelines (`@Scheduled`):** Content fetching runs hourly in the background, and the email delivery task runs daily at **8:00 AM** (configurable via cron).
3.  **Multi-Source Aggregation:** Simultaneously pulls, cleans, and processes data from **5 distinct sources** including: **REST Clients** (Hacker News, NewsAPI, and Reddit) and **RSS Feeds** (Times of India (TOI) and Medium via ROME library).
4.  **Smart Filtering and Persistence:** Implements business logic to ensure users only receive articles that match their exact saved keywords. The system only sends content published within the last 7 days. Deduplication prevents sending the same article multiple times by tracking URLs via **Spring Data JPA**.

-----

## 🛠️ Project Architecture (Tech Stack)

| Component | Technology | Role |
| :--- | :--- | :--- |
| **Core Framework** | **Spring Boot 3.x** | Provides rapid setup and configuration. |
| **Persistence** | **Spring Data JPA & H2** | Used for data modeling (User, Article) and local database storage. |
| **Security** | **Spring Security** | Handles authentication, authorization, and password hashing. |
| **Scheduling** | **Spring Scheduler** | Manages automated fetching and emailing jobs. |
| **Data Fetching** | RestTemplate/WebClient | Used for external REST API calls (JSON). |
| **RSS Parsing** | ROME Library | Handles reading and parsing RSS XML data. |
| **Frontend** | Thymeleaf | Server-side template engine for rendering dynamic HTML pages. |
| **Email** | Spring Mail | Used with `MimeMessageHelper` to send professional HTML digests. |

-----

## ⚙️ Local Setup and Run Instructions

### Prerequisites

  * Java Development Kit (JDK) 17 or higher
  * Apache Maven (Used for dependency management)

### 1\. Configuration (The `application.properties` File)

For security, the live API keys are excluded via `.gitignore`. You must create your own configuration file.

  * **Obtain Keys:** Get a free API key from `newsapi.org` and generate a 16-digit App Password for a dedicated Gmail account.
  * **Create File:** In your project's `src/main/resources` folder, create a file named `application.properties`.
  * **Paste Configuration:**

<!-- end list -->

```properties
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
```

### 2\. Run the Application

Run the Spring Boot application from the root project directory (`/aggregator`).

```bash
./mvnw spring-boot:run
```

The server will start on port 8080.

### 3\. Use the Web Interface

1.  Open your web browser to: `http://localhost:8080/register`
2.  Register a new account (Email and Password). Log in with your new credentials.
3.  Navigate to the Dashboard and save your desired keywords (e.g., `Java, Kubernetes, AI`).
4.  Wait for the scheduled job (or restart the app to trigger the initial 5-second fetch job) to begin receiving personalized email digests.

-----

## 📄 License

This project is licensed under the MIT License.

```
```
