# QServer Log Analysis: Design & Documentation

A comprehensive guide to designing, implementing, and deploying a Java-based solution for analyzing QServer Transaction CSV logs in **IntelliJ IDEA**.

---

## 1. Introduction

### 1.1 Purpose

This solution aims to **parse**, **process**, and **analyze** a large number of **QServer Transaction** CSV logs to provide insights into:

- Transaction performance (execution time, waiting time, etc.)
- Thread utilization (heavily loaded threads)
- Anomaly detection (via standard deviation and Z-score)
- Correlation analysis (e.g., waiting time vs. processing time)
- Reporting and visualization (charts, PDF/HTML export, dashboards)

### 1.2 Scope

1. **Data Ingestion**: Load hundreds of CSV files from a specified directory.
2. **Data Transformation**: Convert string columns to `LocalDateTime` (for timestamps) and `double` (for numeric values).
3. **Filtering & Grouping**: By transaction ID, thread ID, `actionelementtype`, or time intervals (hour, minute, second).
4. **Analytics**:
    - Identify slow transactions
    - Detect high waiting times
    - Find heavily loaded threads
    - Compute correlation and detect anomalies
5. **Visualization & Reporting**:
    - Charts (e.g., bar, line, histogram)
    - Summaries (console or file-based)
    - Optional real-time dashboards

### 1.3 Goals

- **Scalability**: Efficiently handle large log files.
- **Flexibility**: Support custom filtering, grouping, and analytics.
- **Extensibility**: Easily add new analysis modules or chart types.
- **Usability**: Provide clear outputs, interactive charts, or integrated dashboards.

---

## 2. Architecture & High-Level Design

+-----------------------+ | CSV Log Files (Dir) | +----------+------------+ | v +-----------------------+ +-----------------------------+ | Parsing & Loading | | In-Memory Data Structures | | (Apache Commons CSV) | ---> | (List<QServerTransRecord>) | +----------+------------+ +-----------------------------+ | v +-----------------------+ | Analysis Module | | (Filtering, Grouping, | | Stats, Anomalies, etc)| +----------+------------+ | v +-----------------------+ | Visualization & | | Reporting (Charts, | | PDF/HTML, etc.) | +-----------------------+

                ┌────────────────────────┐
                │  CSV Log Files (Dir)   │
                └────────────────────────┘
                           │
                           ▼
                ┌────────────────────────┐
                │   Parsing & Loading    │
                │ (Apache Commons CSV)   │
                └────────────────────────┘
                           │
                           ▼
                ┌────────────────────────┐
                │     In-Memory Model    │
                │  (QServerTransRecord)  │
                └────────────────────────┘
                           │
                           ▼
                ┌────────────────────────┐
                │    Analysis Module     │
                │  (Filtering, Grouping, │
                │   Stats, Anomalies,    │
                │   Correlation, etc.)   │
                └────────────────────────┘
                           │
                           ▼
                ┌────────────────────────┐
                │  Visualization &       │
                │  Reporting (Charts,    │
                │  PDF/HTML, etc.)       │
                └────────────────────────┘


1. **CSV Parser**
    - Reads each CSV file using **Apache Commons CSV**.
    - Converts columns to the correct data types.
    - Aggregates all records into an in-memory list.

2. **Data Model**
    - `QServerTransRecord` as a central POJO.
    - Fields: `transactionid`, `threadid`, `starttime`, `endtime`, `waitingtime`, etc.

3. **Analysis**
    - **Filtering**: by thread ID, transaction ID, or time range.
    - **Grouping**: by transaction ID, hour/minute, etc.
    - **Performance Metrics**: slow transactions, heavily loaded threads.
    - **Anomaly Detection**: standard deviation, Z-score.
    - **Correlation**: e.g., waiting time vs. processing time.

4. **Visualization & Reporting**
    - Basic charts (JFreeChart) or advanced dashboards (JavaFX, Vaadin).
    - Summaries in console or as PDF/HTML exports.

---

## 3. Data Model

Create a POJO representing one row in the QServer Transaction log:

```java
public class QServerTransRecord {
    private String transactionId;
    private String transactionKind;
    private String threadName;
    private String threadId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    // Durations
    private double length;
    private double waitingTime;
    private double procTime;
    private double funcTime;
    private double dbTime;
    private double deltaSetFinalize;
    private double ioDef;
    private double notifications;
    private double mrSend;
    private double streamTime;

    // Counters
    private double nrDatasets;
    private double size;
    private double constructions;
    private double destructions;
    private double changes;

    // Additional
    private String clientId;
    private String ipClient;
    private String username;
    private String actionElementType;
    private String actionElementName;
    private String actionElementKey;
    private String description;
    private String messageId;
    private String lockProfile;

    // Memory usage
    private double procMem;
    private double funcMem;
    private double dbMem;

    // Getters & setters omitted for brevity...
}

