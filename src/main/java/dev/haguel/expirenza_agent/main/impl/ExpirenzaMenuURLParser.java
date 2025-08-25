package dev.haguel.expirenza_agent.main.impl;

import dev.haguel.expirenza_agent.entity.DishCategory;
import dev.haguel.expirenza_agent.entity.Dish;
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
public class ExpirenzaMenuURLParser extends ItemsBufferParser<String, List<Dish>> {
    public ExpirenzaMenuURLParser(ItemsBuffer<String> itemsBuffer) {
        super(itemsBuffer);
    }

    @Override
    public List<Dish> parseItemsFromBuffer() throws InvalidParseException {
        String url = itemsBuffer.take();
        WebDriverKit webDriverKit = setupWebDriverKit();
        return parseExpirenzaMenuFromUrl(url, webDriverKit);
    }

    private record PageCategory(String name, String link){}

    private record WebDriverKit(WebDriver webDriver, WebDriverWait webDriverWait){}

    private WebDriverKit setupWebDriverKit() {
        WebDriverManager.chromedriver().setup();
        WebDriver driver = new ChromeDriver();
        WebDriverWait webDriverWait = new WebDriverWait(driver, Duration.ofSeconds(10));
        return new WebDriverKit(driver, webDriverWait);
    }

    private List<Dish> parseExpirenzaMenuFromUrl(String baseUrl, WebDriverKit webDriverKit) throws InvalidParseException {
        List<Dish> dishes = new ArrayList<>();

        try {
            webDriverKit.webDriver.get(baseUrl);
            parseDishesFromWebDriverKit(webDriverKit, dishes);
        } catch (WebDriverException exception) {
            System.err.println("An error occurred during the Selenium execution.");
            exception.printStackTrace();
        } finally {
            if (webDriverKit.webDriver != null) webDriverKit.webDriver.quit();
        }

        return dishes;
    }

    private void parseDishesFromWebDriverKit(WebDriverKit webDriverKit, List<Dish> dishes) throws InvalidParseException {
        List<WebElement> menuLinks = findMenuLinksElements(webDriverKit);
        List<PageCategory> categories = getCategoriesFromMenuLinks(menuLinks);

        if (categories.isEmpty()) throw new InvalidParseException("Categories are empty");
        for (PageCategory pageCategory : categories) {
            parseDishesFromPageCategory(pageCategory, webDriverKit, dishes);
        }
    }


    private List<WebElement> findMenuLinksElements(WebDriverKit webDriverKit) {
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

    private void parseDishesFromPageCategory(PageCategory pageCategory, WebDriverKit webDriverKit, List<Dish> dishes) {
        String mainCategory = pageCategory.name();
        String categoryUrl = pageCategory.link();
        Document categoryDocument = getCategoryHTMLDocument(webDriverKit, categoryUrl);
        Elements subCategoryElements = categoryDocument.select("h2.dish-list--title");

        if (subCategoryElements.isEmpty()) {
            addDishesToList(categoryDocument.select("div.menu-list-item"), mainCategory, null, dishes);
        } else {
            addCategoryDishesToList(mainCategory, subCategoryElements, dishes);
        }
    }

    private Document getCategoryHTMLDocument(WebDriverKit webDriverKit, String categoryUrl) {
        webDriverKit.webDriver.get(categoryUrl);

        By dishSelector = By.cssSelector("div.menu-list-item");
        webDriverKit.webDriverWait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(dishSelector));

        String pageSource = webDriverKit.webDriver.getPageSource();
        return Jsoup.parse(pageSource);
    }

    private void addCategoryDishesToList(String mainCategory, Elements subCategoryElements, List<Dish> dishes) {
        for (Element subCategoryElement : subCategoryElements) {
            String subCategory = subCategoryElement.text().trim();
            Element nextElementAfterSubCategory = subCategoryElement.nextElementSibling();
            List<Element> dishElements = new ArrayList<>();

            while (isNotSubCategoryElement(nextElementAfterSubCategory)) {
                dishElements.addAll(nextElementAfterSubCategory.select("div.menu-list-item"));
                nextElementAfterSubCategory = nextElementAfterSubCategory.nextElementSibling();
            }

            addDishesToList(new Elements(dishElements), mainCategory, subCategory, dishes);
        }
    }

    private boolean isNotSubCategoryElement(Element element) {
        return element != null && !element.tagName().equals("h2") && !element.hasClass("dish-list--title");
    }

    private void addDishesToList(Elements dishElements, String mainCategory, String subCategory, List<Dish> dishes) {
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

            dishes.add(dish);
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