package dev.haguel.expirenza_agent.main.impl;

import dev.haguel.expirenza_agent.entity.DishCategory;
import dev.haguel.expirenza_agent.entity.Dish;
import dev.haguel.expirenza_agent.entity.Restaraunt;
import dev.haguel.expirenza_agent.exception.InvalidParseException;
import dev.haguel.expirenza_agent.main.ItemsBuffer;
import dev.haguel.expirenza_agent.main.ItemsBufferParser;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;


@Component
public class ExpirenzaMenuURLParser extends ItemsBufferParser<String, Restaraunt> {
    private final Restaraunt.RestarauntBuilder restarauntBuilder;
    private final List<Dish> dishList;
    private final WebDriverKit webDriverKit;

    public ExpirenzaMenuURLParser(ItemsBuffer<String> itemsBuffer) {
        super(itemsBuffer);
        restarauntBuilder = Restaraunt.builder();
        dishList = new ArrayList<>();
        webDriverKit = setupWebDriverKit();
    }

    @Override
    public Restaraunt parseItemFromBuffer() throws InvalidParseException {
        String url = itemsBuffer.take();
        return parseExpirenzaMenuRestaurantFromUrl(url);
    }

    private record PageCategory(String name, String link){}

    private record WebDriverKit(WebDriver webDriver, WebDriverWait webDriverWait){}

    private WebDriverKit setupWebDriverKit() {
        WebDriverManager.chromedriver().setup();
        WebDriver driver = new ChromeDriver();
        WebDriverWait webDriverWait = new WebDriverWait(driver, Duration.ofSeconds(10));
        return new WebDriverKit(driver, webDriverWait);
    }


    private Restaraunt parseExpirenzaMenuRestaurantFromUrl(String baseUrl) throws InvalidParseException {
        try {
            webDriverKit.webDriver.get(baseUrl);
            parseRestaurantForDishes();
        } catch (WebDriverException exception) {
            System.err.println("An error occurred during the Selenium execution.");
            exception.printStackTrace();
        } finally {
            if (webDriverKit.webDriver != null) webDriverKit.webDriver.quit();
        }

        restarauntBuilder.dishes(dishList);
        return restarauntBuilder.build();
    }


    private void parseRestaurantForDishes() throws InvalidParseException {
        parseAndSetRestaurantName();
        List<WebElement> menuLinks = findMenuLinksElements();
        List<PageCategory> categories = getCategoriesFromMenuLinks(menuLinks);

        if (categories.isEmpty()) throw new InvalidParseException("Categories are empty");
        for (PageCategory pageCategory : categories) {
            parseDishesFromPageCategory(pageCategory);
        }
    }

    private void parseAndSetRestaurantName() {
        By restaurantTitle = By.cssSelector("h2.title");
        webDriverKit.webDriverWait.until(ExpectedConditions.visibilityOfElementLocated(restaurantTitle));
        WebElement element = webDriverKit.webDriver.findElement(restaurantTitle);

        restarauntBuilder.name(element.getText().trim());
    }

    private List<WebElement> findMenuLinksElements() {
        By menuLinkSelector = By.cssSelector("a.main-menu-item");
        webDriverKit.webDriverWait.until(ExpectedConditions.visibilityOfElementLocated(menuLinkSelector));

        return webDriverKit.webDriver.findElements(menuLinkSelector);
    }

    private List<PageCategory> getCategoriesFromMenuLinks(List<WebElement> menuLinks) {
        List<PageCategory> categories = new ArrayList<>();
        for (WebElement link : menuLinks) {
            String name = link.getText().trim();
            String href = link.getAttribute("href");
            categories.add(new PageCategory(name, href));
        }

        return categories;
    }

    private void parseDishesFromPageCategory(PageCategory pageCategory) {
        String mainCategory = pageCategory.name();
        String categoryUrl = pageCategory.link();
        Document categoryDocument = getCategoryHTMLDocument(categoryUrl);
        Elements subCategoryElements = categoryDocument.select("h2.dish-list--title");

        if (subCategoryElements.isEmpty()) {
            addDishesToDishList(categoryDocument.select("div.menu-list-item"), mainCategory, null);
        } else {
            addDishesFromCategoryToDishList(mainCategory, subCategoryElements);
        }
    }

    private Document getCategoryHTMLDocument(String categoryUrl) {
        webDriverKit.webDriver.get(categoryUrl);

        By dishSelector = By.cssSelector("div.menu-list-item");
        webDriverKit.webDriverWait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(dishSelector));

        String pageSource = webDriverKit.webDriver.getPageSource();
        return Jsoup.parse(pageSource);
    }

    private void addDishesFromCategoryToDishList(String mainCategory, Elements subCategoryElements) {
        for (Element subCategoryElement : subCategoryElements) {
            String subCategory = subCategoryElement.text().trim();
            Element nextElementAfterSubCategory = subCategoryElement.nextElementSibling();
            List<Element> dishElements = new ArrayList<>();

            while (isNotSubCategoryElement(nextElementAfterSubCategory)) {
                dishElements.addAll(nextElementAfterSubCategory.select("div.menu-list-item"));
                nextElementAfterSubCategory = nextElementAfterSubCategory.nextElementSibling();
            }

            addDishesToDishList(new Elements(dishElements), mainCategory, subCategory);
        }
    }

    private boolean isNotSubCategoryElement(Element element) {
        return element != null && !element.tagName().equals("h2") && !element.hasClass("dish-list--title");
    }

    private void addDishesToDishList(Elements dishElements, String mainCategory, String subCategory) {
        for (Element dishElement : dishElements) {
            Element nameElement = dishElement.selectFirst("h4.item-title");
            String name = nameElement != null ? nameElement.text().trim() : "NO NAME";

            Element descElement = dishElement.selectFirst("div.item-description p");
            String description = descElement != null ? descElement.text().trim() : "NO DESCRIPTION";

            Element priceElement = dishElement.selectFirst("div.price");
            String priceStr = priceElement != null ? priceElement.text().trim() : "NO PRICE";
            BigDecimal price = parsePrice(priceStr);

            DishCategory dishCategory = DishCategory.builder()
                    .category(mainCategory)
                    .subCategory(subCategory)
                    .build();

            Dish dish = Dish.builder()
                    .name(name)
                    .dishCategory(dishCategory)
                    .description(description)
                    .price(price)
                    .build();

            dishList.add(dish);
        }
    }

    private BigDecimal parsePrice(String priceString) {
        if (isInvalidPrice(priceString)) return BigDecimal.ZERO;

        return getBigDecimalPriceFromString(priceString);
    }

    private boolean isInvalidPrice(String priceString) {
        return priceString == null || priceString.isEmpty();
    }

    private BigDecimal getBigDecimalPriceFromString(String priceString) {
        String validatedPriceString = priceString.replaceAll("[^\\d\\.]", "");
        try {
            return new BigDecimal(validatedPriceString);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}