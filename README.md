# 🎓 Student Result Batch Processing System

A **Java Spring Boot** enterprise application that processes student exam records in bulk using **Spring Batch**, stores results in **MySQL via JDBC**, and exposes a complete **REST API** for result queries and reporting.

Built to demonstrate: **Java 17 · Spring Batch · Spring Boot · Maven · JDBC · MySQL · REST API · OOP · GitLab CI/CD**

---

## 🏗️ Tech Stack

| Technology | Role |
|---|---|
| Java 17 | Core language (enterprise OOP patterns) |
| Spring Boot 3.2 | Application framework |
| **Spring Batch** | Bulk record processing (Job → Step → Reader/Processor/Writer) |
| Maven | Dependency management & build |
| MySQL | Persistent storage for students and results |
| JdbcTemplate | Direct JDBC for rank computation and reporting |
| Spring Data JPA | ORM-based data access |
| GitLab CI/CD | Automated build, test, package pipeline |

---

## 🚀 Quick Start

### Prerequisites
Java 17+, Maven 3.8+, MySQL 8.0+

```bash
git clone https://github.com/yourusername/student-result-processor.git
cd student-result-processor
```

Edit `src/main/resources/application.properties`:
```properties
spring.datasource.password=YOUR_MYSQL_PASSWORD
```

```bash
mvn spring-boot:run
```

### Demo in 3 API calls (use Postman or curl)

```bash
# 1. Load sample student data (10 students across CSE, IT, AIML)
POST http://localhost:8082/api/results/seed

# 2. Run the batch job — processes all records, computes grades, ranks
POST http://localhost:8082/api/results/run-batch

# 3. View full summary report
GET  http://localhost:8082/api/results/report
```

---

## 📡 API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/results/seed` | Load 10 sample student records |
| POST | `/api/results/run-batch` | Trigger Spring Batch processing job |
| GET | `/api/results` | All results sorted by percentage |
| GET | `/api/results/report` | Summary: pass rate, grade distribution, toppers |
| GET | `/api/results/toppers?n=5` | Top N students overall |
| GET | `/api/results/department/{dept}` | Results for CSE / IT / AIML |
| GET | `/api/results/failed` | All failed students |
| POST | `/api/results/students` | Add a new student record |

---

## ⚙️ How the Batch Job Works

```
POST /api/results/run-batch
        │
        ▼
  Spring Batch Job: "processStudentResultsJob"
        │
        ▼
  Step: "processResultsStep"  (chunk size = 10)
    ┌───────────────────────────────────┐
    │  ItemReader                       │
    │  → reads unprocessed students     │
    │    from MySQL via JPA             │
    ├───────────────────────────────────┤
    │  ItemProcessor (per record)       │
    │  → calculates total marks         │
    │  → computes percentage            │
    │  → assigns grade (O/A+/A/B/C/F)  │
    │  → sets PASS/FAIL status          │
    ├───────────────────────────────────┤
    │  ItemWriter                       │
    │  → saves StudentResult to MySQL   │
    │  → commits every 10 records       │
    └───────────────────────────────────┘
        │
        ▼
  Post-processing (raw JDBC):
  → computes department rank per student
```

---

## 📊 Grading Scale

| Percentage | Grade | Status |
|---|---|---|
| 90 – 100% | O (Outstanding) | PASS |
| 75 – 89% | A+ (Excellent) | PASS |
| 60 – 74% | A (Very Good) | PASS |
| 50 – 59% | B (Good) | PASS |
| 40 – 49% | C (Average) | PASS |
| < 40% OR any subject < 40 | F (Fail) | FAIL |

---

## 🏗️ Project Structure

```
student-result-processor/
├── src/main/java/com/vaishnavi/resultprocessor/
│   ├── ResultProcessorApplication.java      # Entry point
│   ├── batch/
│   │   ├── BatchJobConfig.java             # Job/Step/Reader/Writer wiring
│   │   └── ResultItemProcessor.java        # Grade calculation logic
│   ├── controller/ResultController.java    # REST endpoints
│   ├── service/ResultProcessorService.java # Business logic, reporting
│   ├── repository/                         # JPA data access layer
│   └── model/
│       ├── Student.java                   # Input entity (raw marks)
│       └── StudentResult.java             # Output entity (processed results)
├── .gitlab-ci.yml                         # CI/CD pipeline
└── pom.xml                                # Maven dependencies
```

---

*Built by Vaishnavi Srivastava*
