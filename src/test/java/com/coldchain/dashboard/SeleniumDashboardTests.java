
package com.coldchain.dashboard;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.extension.TestWatcher;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SeleniumDashboardTests {

    private WebDriver driver;
    private WebDriverWait wait;

    private final String baseUrl = System.getProperty(
            "selenium.baseUrl", "http://localhost:8089");

    private final Path screenshotDir =
            Path.of("target", "selenium-screenshots");

    @RegisterExtension
    final TestWatcher screenshotWatcher = new TestWatcher() {
        @Override
        public void testFailed(
                ExtensionContext context, Throwable cause) {
            captureFailureScreenshot(context);
            closeBrowser();
        }

        @Override
        public void testSuccessful(ExtensionContext context) {
            closeBrowser();
        }

        @Override
        public void testAborted(
                ExtensionContext context, Throwable cause) {
            closeBrowser();
        }

        private void captureFailureScreenshot(
                ExtensionContext context) {
            if (driver == null) {
                return;
            }

            try {
                Files.createDirectories(screenshotDir);

                String fileName = context.getRequiredTestMethod()
                        .getName()
                        .replaceAll("[^a-zA-Z0-9_-]", "_");

                Path destination =
                        screenshotDir.resolve(fileName + ".png");

                Files.copy(
                        ((TakesScreenshot) driver)
                                .getScreenshotAs(OutputType.FILE)
                                .toPath(),
                        destination,
                        StandardCopyOption.REPLACE_EXISTING);

                System.err.println(
                        "Failure screenshot saved: "
                                + destination.toAbsolutePath());

            } catch (Exception e) {
                System.err.println(
                        "Screenshot capture failed: " + e.getMessage());
            }
        }

        private void closeBrowser() {
            if (driver != null) {
                driver.quit();
                driver = null;
            }
        }
    };

    @BeforeEach
    void setUp() {
        WebDriverManager.chromedriver().setup();
        
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--disable-notifications");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");


        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        driver.get(baseUrl);

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.tagName("h1")));
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
            driver = null;
        }
    }

    // TC-01: Verify that the dashboard loads.
    @Test
    void dashboardLoadsSuccessfully() {
        assertTrue(driver.getTitle().contains("ColdChain"));

        assertEquals(
                "Monitoring overview",
                driver.findElement(By.tagName("h1")).getText());

        assertTrue(
                driver.getPageSource().contains("Temperature events"));

        assertTrue(driver.findElement(
                By.cssSelector("form.filters")).isDisplayed());
    }

    // TC-02: Search for an existing sample reading.
    @Test
    void searchFindsMatchingReading() {
        driver.findElement(By.name("q")).sendKeys("VAX-001");

        driver.findElement(By.cssSelector(
                "form.filters button[type='submit']")).click();

        wait.until(ExpectedConditions.urlContains("q="));

        List<WebElement> units = driver.findElements(
                By.cssSelector("tbody td.unit"));

        assertFalse(units.isEmpty(),
                "Expected at least one matching reading.");

        assertTrue(units.stream().anyMatch(
                unit -> unit.getText().equals("VAX-001")));

        assertTrue(units.stream().allMatch(
                unit -> unit.getText().contains("VAX-001")));
    }

    // TC-03: Filter readings by HIGH status.
    @Test
    void filterShowsOnlyHighStatusReadings() {
        Select status = new Select(
                driver.findElement(By.name("status")));

        status.selectByValue("HIGH");

        driver.findElement(By.cssSelector(
                "form.filters button[type='submit']")).click();

        wait.until(ExpectedConditions.urlContains("status=HIGH"));

        List<WebElement> statuses = driver.findElements(
                By.cssSelector("tbody .status"));

        assertFalse(statuses.isEmpty(),
                "Expected at least one HIGH reading.");

        assertTrue(statuses.stream().allMatch(
                element -> element.getText().equals("HIGH")));
    }

    // TC-04: Add a reading and verify it appears.
    @Test
    void addReadingAndVerifyItAppears() {
        String unitId = "SELENIUM-" + System.currentTimeMillis();

        driver.findElement(By.name("unitId")).sendKeys(unitId);
        driver.findElement(By.name("location"))
                .sendKeys("Selenium Test Store");

        replaceFieldValue("temperatureC", "4.5");
        replaceFieldValue("minTempC", "2");
        replaceFieldValue("maxTempC", "8");

        driver.findElement(By.name("notes"))
                .sendKeys("Automated Selenium test reading");

        driver.findElement(By.cssSelector(
                "form.event-form button[type='submit']")).click();

        try {
            wait.until(ExpectedConditions.or(
                    ExpectedConditions.visibilityOfElementLocated(
                            By.cssSelector(".notice.success")),
                    ExpectedConditions.visibilityOfElementLocated(
                            By.cssSelector(".notice.error")),
                    ExpectedConditions.urlContains("/error")
            ));
        } catch (RuntimeException e) {
            captureCurrentPageDetails();
            throw e;
        }

        if (!driver.findElements(
                By.cssSelector(".notice.success")).isEmpty()) {

            assertTrue(driver.findElement(
                    By.cssSelector(".notice.success")).getText()
                    .contains("Temperature event saved successfully."));

        } else if (!driver.findElements(
                By.cssSelector(".notice.error")).isEmpty()) {

            fail("The application rejected the reading: "
                    + driver.findElement(
                    By.cssSelector(".notice.error")).getText());

        } else {
            captureCurrentPageDetails();
            fail("Submission did not return to the dashboard. URL: "
                    + driver.getCurrentUrl());
        }

        driver.get(baseUrl);

        WebElement search = driver.findElement(By.name("q"));
        search.sendKeys(unitId);

        driver.findElement(By.cssSelector(
                "form.filters button[type='submit']")).click();

        wait.until(ExpectedConditions.or(
                ExpectedConditions.visibilityOfElementLocated(
                        By.cssSelector("tbody td.unit")),
                ExpectedConditions.visibilityOfElementLocated(
                        By.cssSelector("td.empty"))
        ));

        List<WebElement> units = driver.findElements(
                By.cssSelector("tbody td.unit"));

        assertTrue(units.stream().anyMatch(
                unit -> unit.getText().equals(unitId)),
                "The saved reading should appear in search results.");
    }

    private void replaceFieldValue(String fieldName, String value) {
        WebElement field = driver.findElement(By.name(fieldName));
        field.clear();
        field.sendKeys(value);
    }

    private void captureCurrentPageDetails() {
        System.err.println("Current URL: " + driver.getCurrentUrl());
        System.err.println("Current title: " + driver.getTitle());

        try {
            System.err.println("Current page body:\n"
                    + driver.findElement(By.tagName("body")).getText());
        } catch (Exception e) {
            System.err.println("Could not read page body: "
                    + e.getMessage());
        }
    }

    // TC-05: Verify the empty state for an unknown search.
    @Test
    void nonexistentSearchShowsEmptyState() {
        driver.findElement(By.name("q"))
                .sendKeys("NO-SUCH-UNIT-SELENIUM-999999");

        driver.findElement(By.cssSelector(
                "form.filters button[type='submit']")).click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("td.empty")));

        assertEquals(
                "No matching events. Add a reading or change your search.",
                driver.findElement(By.cssSelector("td.empty")).getText());

        assertTrue(driver.findElements(
                By.cssSelector("tbody td.unit")).isEmpty());
    }
}
