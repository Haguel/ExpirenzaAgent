package dev.haguel.expirenza_agent.main.impl;

import dev.haguel.expirenza_agent.entity.Dish;
import dev.haguel.expirenza_agent.entity.DishCategory;
import dev.haguel.expirenza_agent.entity.Restaurant;
import dev.haguel.expirenza_agent.exception.InvalidParseException;
import dev.haguel.expirenza_agent.main.ItemParser;
import io.github.bonigarcia.wdm.WebDriverManager;
import jakarta.annotation.PreDestroy;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class ExpirenzaMenuURLParser implements ItemParser<String, Restaurant> {

    private final List<WebDriverKit> allDrivers = Collections.synchronizedList(new ArrayList<>());
    private final ThreadLocal<WebDriverKit> driverThreadLocal = ThreadLocal.withInitial(this::createWebDriverKit);

    private record PageCategory(String name, String url) {}
    private record WebDriverKit(WebDriver webDriver, WebDriverWait webDriverWait) {}

    @Override
    public Restaurant parse(String url) throws InvalidParseException {
        return scrapeRestaurantData(driverThreadLocal.get(), url);
    }

    @PreDestroy
    public void cleanup() {
        for (WebDriverKit kit : allDrivers) {
            try {
                kit.webDriver.quit();
            } catch (Exception e) {
                System.err.println("Error quitting a WebDriver instance: " + e.getMessage());
            }
        }
    }

    private WebDriverKit createWebDriverKit() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless", "--disable-gpu", "--no-sandbox", "--disable-dev-shm-usage");

        WebDriver driver = new ChromeDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

        WebDriverKit kit = new WebDriverKit(driver, wait);
        allDrivers.add(kit);
        return kit;
    }

    private Restaurant scrapeRestaurantData(WebDriverKit kit, String baseUrl) throws InvalidParseException {
        try {
            kit.webDriver.get(baseUrl);
            Restaurant.RestaurantBuilder restaurantBuilder = Restaurant.builder();
            List<Dish> dishes = new ArrayList<>();

            String restaurantName = scrapeRestaurantName(kit);
            restaurantBuilder.name(restaurantName);

            List<PageCategory> categories = scrapeMenuCategories(kit);
            if (categories.isEmpty()) {
                throw new InvalidParseException("No menu categories found for URL: " + baseUrl);
            }

            for (PageCategory category : categories) {
                dishes.addAll(scrapeDishesFromCategoryPage(kit, category));
            }

            return restaurantBuilder.dishes(dishes).build();
        } catch (WebDriverException e) {
            throw new InvalidParseException("A WebDriver error occurred while parsing " + baseUrl);
        }
    }

    private String scrapeRestaurantName(WebDriverKit kit) {
        By titleSelector = By.cssSelector("h2.title");
        kit.webDriverWait.until(ExpectedConditions.visibilityOfElementLocated(titleSelector));
        return kit.webDriver.findElement(titleSelector).getText().trim();
    }

    private List<PageCategory> scrapeMenuCategories(WebDriverKit kit) {
        By menuLinkSelector = By.cssSelector("a.main-menu-item");
        kit.webDriverWait.until(ExpectedConditions.visibilityOfElementLocated(menuLinkSelector));

        return kit.webDriver.findElements(menuLinkSelector).stream()
                .map(link -> new PageCategory(link.getText().trim(), link.getAttribute("href")))
                .collect(Collectors.toList());
    }

    private List<Dish> scrapeDishesFromCategoryPage(WebDriverKit kit, PageCategory category) {
        kit.webDriver.get(category.url());
        kit.webDriverWait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("div.menu-list-item")));

        Document page = Jsoup.parse(kit.webDriver.getPageSource());
        Elements subCategoryTitles = page.select("h2.dish-list--title");

        if (subCategoryTitles.isEmpty()) {
            return extractDishesFromElements(page.select("div.menu-list-item"), category.name(), null);
        }

        List<Dish> dishes = new ArrayList<>();
        for (Element titleElement : subCategoryTitles) {
            String subCategoryName = titleElement.text().trim();
            Elements dishElements = new Elements();
            Element nextSibling = titleElement.nextElementSibling();

            // Collect all sibling elements until the next subcategory title
            while (nextSibling != null && !nextSibling.is("h2.dish-list--title")) {
                dishElements.addAll(nextSibling.select("div.menu-list-item"));
                nextSibling = nextSibling.nextElementSibling();
            }
            dishes.addAll(extractDishesFromElements(dishElements, category.name(), subCategoryName));
        }
        return dishes;
    }

    private List<Dish> extractDishesFromElements(Elements dishElements, String mainCategory, String subCategory) {
        List<Dish> dishes = new ArrayList<>();
        for (Element dishElement : dishElements) {
            String name = Optional.ofNullable(dishElement.selectFirst("h4.item-title")).map(Element::text).orElse("").trim();
            String description = Optional.ofNullable(dishElement.selectFirst("div.item-description p")).map(Element::text).orElse("").trim();
            String priceStr = Optional.ofNullable(dishElement.selectFirst("div.price")).map(Element::text).orElse("0").trim();

            DishCategory dishCategory = DishCategory.builder()
                    .category(mainCategory)
                    .subCategory(subCategory)
                    .build();

            dishes.add(Dish.builder()
                    .name(name)
                    .dishCategory(dishCategory)
                    .description(description)
                    .price(parsePrice(priceStr))
                    .build());
        }
        return dishes;
    }

    private BigDecimal parsePrice(String priceString) {
        if (priceString == null || priceString.isEmpty()) return BigDecimal.ZERO;
        try {
            String sanitizedPrice = priceString.replaceAll("[^\\d.]", "");
            return new BigDecimal(sanitizedPrice);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}