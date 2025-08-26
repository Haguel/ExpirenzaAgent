# Expirenza Agent

The Expirenza Agent is a Spring Boot application designed to scrape restaurant menu information from the Expirenza.menu website. It extracts details about dishes, including their categories, descriptions, and prices, and then exports this data to a Google Sheet for further analysis and use.

The application is built with a modular architecture, separating concerns into distinct components for producing URLs, parsing content, and exporting data. It leverages Selenium for web scraping, Google Sheets API for data export, and Spring's scheduling capabilities to automate the process.

## How to set up the project

To set up and run the Expirenza Agent, you'll need to have Java 21 and Maven installed. Follow these steps:

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/Haguel/ExpirenzaAgent.git
    cd ExpirenzaAgent
    ```
2.  **Configure Google Sheets API access:**
    * Follow the Google Cloud documentation to create a service account and download the JSON key file.
    * Place the downloaded `service-account-key.json` file in the `src/main/resources` directory.
    * In the `src/main/resources/application-test.properties` file, update the `google.sheets.spreadsheetId` with your Google Sheet ID.
    * Give Editor role to your service account in the Google Spreadsheet 
3.  **Build and run the application:**
    * You can run the application using the Maven wrapper included in the project:
        ```bash
        ./mvnw spring-boot:run
        ```
    * The agent will start, and you will see log output in your console.

## How it works

The Expirenza Agent operates in a producer-consumer pattern, orchestrated by a central `Agent` component. Here's a breakdown of the workflow:

1.  **URL Production:**
    * The `ExpirenzaMenuURLProducer` is responsible for providing the URLs of the restaurant menus to be scraped.
    * It maintains a queue of URLs, which are then fed into the parsing stage.

2.  **Menu Parsing:**
    * The `ExpirenzaMenuURLParser` takes a URL from the producer and uses Selenium with a headless Chrome browser to scrape the menu data.
    * It navigates through the menu categories, extracts details for each dish, and constructs `Restaurant` and `Dish` objects.

3.  **Data Exporting:**
    * The parsed `Restaurant` object is then passed to the `ExpirenzaMenuDishesExporter`.
    * This component connects to the Google Sheets API, ensures a sheet with the restaurant's name exists, and then populates it with the dish information.
    * It intelligently handles both updating existing dishes and appending new ones.

The entire process is managed by the `Agent` class, which uses separate thread pools for each stage to ensure efficient and non-blocking operation. The agent is scheduled to run at a fixed rate, ensuring that the menu data is kept up-to-date.

## Configuration

The application's configuration is managed through property files located in `src/main/resources`.

* `application.properties`:
    * `spring.application.name`: Sets the application name.
    * `spring.profiles.active`: Defines the active Spring profile (e.g., `test`).

* `application-test.properties` (or other profile-specific files):
    * `google.sheets.spreadsheetId`: The ID of the Google Sheet where data will be exported.
    * `google.sheets.serviceAccountKeyPath`: The path to the Google service account key file.

## Logging

The application uses SLF4J for logging and includes a `LoggingAspect` that provides detailed logs for method entry, exit, and exceptions across the entire application. This allows for easy debugging and monitoring of the agent's behavior.

The logs include information such as:
* Method execution time.
* Arguments passed to methods.
* Return values from methods.
* Detailed error information in case of exceptions.