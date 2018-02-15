package ru.lanit.at.driver;

import org.apache.log4j.Logger;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.LocalFileDetector;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.MalformedURLException;
import java.net.URL;

import static ru.lanit.at.FrameworkConstants.*;

public class RemoteDriverFactory {
    private static Logger log = Logger.getLogger(RemoteDriverFactory.class);

    static RemoteWebDriver createInstance(String browserName) {

        DesiredCapabilities capability = null;
        RemoteWebDriver driver = null;

        switch (browserName.toLowerCase()) {
            case "firefox":
                capability = CapabilitiesManager.getFirefoxCapabilities();
                break;
            case "chrome":
                capability = CapabilitiesManager.getChromeCapabibilities();
                capability.setCapability(ChromeOptions.CAPABILITY, CapabilitiesManager.getChromeOptions());
                break;
            default:
                return null;
        }

        if (isVNCEnabled()) {
            capability.setCapability(ENABLE_VNC, true);
            log.info("VNC подключено");
        } else {
            log.info("VNC не подключен");
        }

        if (isVideoEnabled()) {
            capability.setCapability(ENABLE_VIDEO, true);
            log.info("Запись видео включена.");
        } else {
            log.info("Запись видео не включена.");
        }

        URL hubUrl = getHubUrl();
        driver = new RemoteWebDriver(hubUrl, capability);
        log.info("Создан Remote драйвер " + browserName + " для " + hubUrl.toString());

        driver.manage().window().setSize(new Dimension(1920, 1080));
        log.info("Размер окна браузера установлен на 1920х1080");

        ((RemoteWebDriver) driver).setFileDetector(new LocalFileDetector());

        return driver;
    }

    private static URL getHubUrl() {
        String url = System.getProperty(HUB_URL, DEFAULT_HUB_URL);
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            log.error("Неверно задан адрес хаба selenoid/selenium: " + url);
            return null;
        }
    }

    private static boolean isVideoEnabled() {
        try {
            return System.getProperty(ENABLE_VIDEO).equalsIgnoreCase("true");
        } catch (NullPointerException npe) {
            return false;
        }

    }

    private static boolean isVNCEnabled() {
        try {
            return System.getProperty(ENABLE_VNC).equalsIgnoreCase("true");
        } catch (NullPointerException npe) {
            return false;
        }
    }
}