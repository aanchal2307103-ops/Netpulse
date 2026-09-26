# NetPulse — Network Diagnostic & Monitoring Dashboard

A desktop JavaFX application that lets you add multiple servers, websites,
and IP endpoints, then monitors their uptime, response latency, and
connection stability in real time — without freezing the GUI — using
background multithreading, a SQLite database, and JSON import/export.

## How the project maps to the weekly plan

| Week | Topic | Where it shows up in this project |
|---|---|---|
| 1 | C/Java compilation, Java syntax, OOP | `Main.java` entry point; `model/` package (classes, enums, a record, encapsulation) |
| 2 | Git & version control | Project is structured as a normal Maven repo, ready to `git init`; see **Next steps** below |
| 3 | Desktop GUI with JavaFX | `ui/MainView.java`, `ui/AddEndpointDialog.java` — Stage/Scene, BorderPane/HBox/VBox layouts, TableView, LineChart, event handling, with styling handled directly through JavaFX APIs in Java code |
| 4 | Multithreading & concurrency | `service/NetworkProbeService.java` — `ScheduledExecutorService`, one repeating task per endpoint, `Platform.runLater` for safe background→UI updates |
| 6 | SQLite + JavaFX | `service/DatabaseManager.java` — JDBC connection, table creation, insert/update/delete/query for endpoints and probe history |
| 7 | JSON parsing & API handling | `service/JsonConfigManager.java` — import/export endpoint lists as JSON, plus an ad-hoc JSON API probe (parses a live HTTP JSON response) |

## Project structure

```
NetPulse/
├── pom.xml
├── .gitignore
├── README.md
└── src/main/
    ├── java/com/netpulse/
    │   ├── Main.java
    │   ├── model/
    │   │   ├── Endpoint.java
    │   │   ├── EndpointType.java
    │   │   ├── EndpointStatus.java
    │   │   └── ProbeResult.java
    │   ├── service/
    │   │   ├── DatabaseManager.java
    │   │   ├── NetworkProbeService.java
    │   │   └── JsonConfigManager.java
    │   └── ui/
    │       ├── MainView.java
    │       └── AddEndpointDialog.java
    └── resources/config/
        └── sample-endpoints.json
```

## Requirements

- JDK 17+
- Maven 3.8+
- Internet access the first time you build (Maven needs to download the
  JavaFX, SQLite JDBC, and org.json dependencies listed in `pom.xml`)

## How to run

```bash
cd NetPulse
mvn javafx:run
```

## How to build a runnable jar

```bash
mvn clean package
java -jar target/netpulse-1.0.0.jar
```

## Using the app

1. Click **Add Endpoint** and fill in a name, host/IP, port, type, and how
   often (in seconds) it should be checked.
2. The app immediately starts probing it on a background thread; the table
   and latency chart update automatically as results come in.
3. Select a row to see its latency trend chart.
4. **Check Now** forces an immediate probe of the selected endpoint.
5. **Import JSON / Export JSON** load or save your endpoint list — try
   importing `src/main/resources/config/sample-endpoints.json` to see three
   endpoints added at once.
6. **Probe JSON API…** lets you test any JSON REST API URL and see its
   status code, latency, and a snippet of the parsed response.
7. All endpoints and every probe result are saved to a local `netpulse.db`
   SQLite file, so history survives a restart.

## Next steps

You asked about adding this project to Git — happy to walk through
`git init`, staging, committing, and pushing to a remote (e.g. GitHub) in
the next message whenever you're ready, since your Week 2 topic covers
exactly that.
