package dev.haguel.expirenza_agent.main.impl;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.*;
import dev.haguel.expirenza_agent.entity.Dish;
import dev.haguel.expirenza_agent.entity.DishCategory;
import dev.haguel.expirenza_agent.entity.Restaurant;
import dev.haguel.expirenza_agent.main.DataExporter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class ExpirenzaMenuDishesExporter implements DataExporter<Restaurant> {

    @Value("${google.sheets.spreadsheetId}")
    private String spreadsheetId;

    @Value("${google.sheets.serviceAccountKeyPath:/service-account-key.json}")
    private String serviceAccountKeyPath;

    private static final String APP_NAME = "Expirenza Menu Exporter";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS);
    private static final List<Object> HEADERS = List.of("Category", "SubCategory", "Name", "Description", "Price");

    private record PartitionedDishes(List<ValueRange> forUpdate, List<List<Object>> forAppend) {}

    @Override
    public void export(Restaurant restaurant) {
        try {
            Sheets sheetsService = createSheetsService();
            String sheetName = restaurant.getName();

            ensureSheetExists(sheetsService, sheetName);

            Map<String, Integer> existingDishRowMap = getExistingDishRows(sheetsService, sheetName);
            PartitionedDishes dishes = partitionDishes(restaurant.getDishes(), existingDishRowMap, sheetName);

            if (!dishes.forUpdate().isEmpty()) {
                performBatchUpdate(sheetsService, dishes.forUpdate());
            }
            if (!dishes.forAppend().isEmpty()) {
                performAppend(sheetsService, sheetName, dishes.forAppend());
            }

        } catch (IOException | GeneralSecurityException e) {
            System.err.println("An error occurred during Google Sheets export: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private PartitionedDishes partitionDishes(List<Dish> dishes, Map<String, Integer> existingDishRowMap, String sheetName) {
        List<ValueRange> updates = new ArrayList<>();
        List<List<Object>> newRows = new ArrayList<>();

        for (Dish dish : dishes) {
            Integer rowIndex = existingDishRowMap.get(dish.getName());
            List<Object> rowData = dishToRowData(dish);

            if (rowIndex != null) {
                String updateRange = String.format("%s!A%d", sheetName, rowIndex);
                updates.add(new ValueRange().setRange(updateRange).setValues(List.of(rowData)));
            } else {
                newRows.add(rowData);
            }
        }
        return new PartitionedDishes(updates, newRows);
    }

    private Sheets createSheetsService() throws IOException, GeneralSecurityException {
        final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        InputStream credentialsStream = ExpirenzaMenuDishesExporter.class.getResourceAsStream(serviceAccountKeyPath);
        if (credentialsStream == null) {
            throw new FileNotFoundException("Service account key not found: " + serviceAccountKeyPath);
        }

        GoogleCredential credential = GoogleCredential.fromStream(credentialsStream).createScoped(SCOPES);
        return new Sheets.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(APP_NAME)
                .build();
    }

    private void ensureSheetExists(Sheets service, String sheetName) throws IOException {
        Spreadsheet spreadsheet = service.spreadsheets().get(spreadsheetId).setFields("sheets.properties.title").execute();
        boolean sheetExists = spreadsheet.getSheets().stream()
                .anyMatch(s -> sheetName.equals(s.getProperties().getTitle()));

        if (!sheetExists) {
            AddSheetRequest addSheetRequest = new AddSheetRequest().setProperties(new SheetProperties().setTitle(sheetName));
            Request request = new Request().setAddSheet(addSheetRequest);
            BatchUpdateSpreadsheetRequest batchUpdateReq = new BatchUpdateSpreadsheetRequest().setRequests(List.of(request));
            service.spreadsheets().batchUpdate(spreadsheetId, batchUpdateReq).execute();

            performAppend(service, sheetName, List.of(HEADERS));
            System.out.println("Created new sheet with name: " + sheetName);
        }
    }

    private Map<String, Integer> getExistingDishRows(Sheets service, String sheetName) throws IOException {
        String range = sheetName + "!C:C";
        ValueRange response = service.spreadsheets().values().get(spreadsheetId, range).execute();
        List<List<Object>> values = response.getValues();

        if (values == null || values.isEmpty()) {
            return Collections.emptyMap();
        }

        return IntStream.range(0, values.size())
                .filter(i -> !values.get(i).isEmpty() && values.get(i).get(0) != null)
                .boxed()
                .collect(Collectors.toMap(
                        i -> values.get(i).get(0).toString(),
                        i -> i + 1,
                        (first, second) -> first
                ));
    }

    private List<Object> dishToRowData(Dish dish) {
        DishCategory category = dish.getDishCategory();
        String cat = category != null ? category.getCategory() : "";
        String sub = category != null ? category.getSubCategory() : "";
        String name = dish.getName() != null ? dish.getName() : "";
        String desc = dish.getDescription() != null ? dish.getDescription() : "";
        String price = dish.getPrice() != null ? dish.getPrice().toPlainString() : "0";
        return List.of(cat, sub, name, desc, price);
    }

    private void performBatchUpdate(Sheets service, List<ValueRange> updates) throws IOException {
        BatchUpdateValuesRequest body = new BatchUpdateValuesRequest()
                .setValueInputOption("RAW")
                .setData(updates);
        service.spreadsheets().values().batchUpdate(spreadsheetId, body).execute();
    }

    private void performAppend(Sheets service, String sheetName, List<List<Object>> rows) throws IOException {
        ValueRange body = new ValueRange().setValues(rows);
        service.spreadsheets().values()
                .append(spreadsheetId, sheetName, body)
                .setValueInputOption("RAW")
                .execute();
    }
}