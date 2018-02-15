package ru.lanit.at.driver;

import org.apache.log4j.Logger;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.winium.DesktopOptions;
import org.openqa.selenium.winium.WiniumDriver;
import org.testng.Assert;
import ru.lanit.at.FrameworkConstants;
import ru.lanit.at.exceptions.FrameworkRuntimeException;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Для запуска WINIUM используются следующие переменные окружения:
 * winium (true/false)
 * winium.app.path (path to executable)
 * winium.hub.url (http://localhost:9999 as default)
 */
public class DriverManager {
    private Logger log = Logger.getLogger(DriverManager.class);
    private ThreadLocal<WebDriver> driver = new ThreadLocal<>();
    private static final String DEFAULT_BROWSER = "chrome";
    private static final String DEFAULT_HUB_URL = "http://localhost:4444/wd/hub";
    private static final String DEFAULT_WINIUM_HUB_URL = "http://localhost:9999";

    static {
        System.setProperty("webdriver.gecko.driver", "src/main/resources/drivers/geckodriver.exe");
        System.setProperty("webdriver.chrome.driver", "src/main/resources/drivers/chromedriver.exe");
    }

// todo создать выбор браузера по версии и т.п.

    public WebDriver getDriver(String browserName) {
        if (driver.get() == null) {
            driver.set(getNewDriverInstance(browserName));
        } else {
            log.error("Уже запущен драйвер " + driver.get().getClass().getSimpleName() + "!");
        }
        return driver.get();
    }

    private WebDriver getNewDriverInstance(String browserName) {
        if ("true".equalsIgnoreCase(System.getProperty("winium"))) {
            String appPath = System.getProperty("winium.app.path");
            DesktopOptions option = new DesktopOptions();
            option.setApplicationPath(appPath);
            // TODO: пока фабрика настраивается дефолтными значениями, возможности сконфигурять DesiredCapabilities нет.
            option.setDebugConnectToRunningApp(false);
            option.setLaunchDelay(2);
            String hubUrl = System.getProperty("winium.hub.url");
            if (hubUrl == null || hubUrl.isEmpty()) hubUrl = DEFAULT_WINIUM_HUB_URL;
            try {
                return new WiniumDriver(new URL(hubUrl), option);
            } catch (MalformedURLException e) {
                Assert.fail("Could not connect to driver!");
            }
        }
        if ("true".equalsIgnoreCase(System.getProperty("remote"))) {
            DesiredCapabilities desiredCapabilities = new DesiredCapabilities();
            desiredCapabilities.setBrowserName(browserName);
            String hubUrl = System.getProperty("selenium.hub.url");
            if (hubUrl == null || hubUrl.isEmpty()) hubUrl = DEFAULT_HUB_URL;
            try {
                return new RemoteWebDriver(new URL(hubUrl), desiredCapabilities);
            } catch (MalformedURLException e) {
                throw new FrameworkRuntimeException("Exception on remote web driver initialization", e);
            }
        } else {
            return LocalDriverFactory.createInstance(browserName);
        }
    }

    public boolean isActive() {
        return driver.get() != null;
    }

    public void shutdown() {
        driver.get().quit();
        driver.remove();
        log.info("Закрываем драйвер");
    }

    public WebDriver getDriver() {
        if (driver.get() == null) {
            driver.set(getNewDriverInstance(getBrowserName()));
        }
        return driver.get();
    }

    private String getBrowserName(){
        String browserSystemVariableStr = System.getProperty(FrameworkConstants.BROWSER_VARIABLE_NAME);
        if( browserSystemVariableStr == null || browserSystemVariableStr.isEmpty()) return DEFAULT_BROWSER;
        return browserSystemVariableStr;
    }

    public JavascriptExecutor getJSExecutor() {
        if (driver.get() != null) {
            return (JavascriptExecutor) driver.get();
        } else {
            log.error("Драйвер не запущен! Сначала инициализируйте драйвер");
            return null;
        }
    }

    public String executeScript(String jsCommand, Object... args) {
        return (String) getJSExecutor().executeScript(jsCommand, args);
    }

    public boolean isJSActive() {
        return !executeScript("return jQuery.active").equals("0");
    }

    public boolean isPageLoaded() {
        return executeScript("return document.readyState").equals("complete");
    }

    // страница загрузилась достаточно, чтобы с ней можно было взаимодействовать
    public boolean isPageInterable() {
        return isPageLoaded() || executeScript("return document.readyState").equals("interactive");
    }
}