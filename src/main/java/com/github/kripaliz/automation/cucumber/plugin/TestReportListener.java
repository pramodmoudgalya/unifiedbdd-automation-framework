/**
 *
 */
package com.github.kripaliz.automation.cucumber.plugin;

import java.io.ByteArrayInputStream;
import java.util.Arrays;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import com.github.kripaliz.automation.AutomationApplication;

import cucumber.api.PickleStepTestStep;
import cucumber.api.Result;
import cucumber.api.event.ConcurrentEventListener;
import cucumber.api.event.EventPublisher;
import cucumber.api.event.TestCaseStarted;
import cucumber.api.event.TestStepFinished;
import io.qameta.allure.Allure;

/**
 * A cucumber plugin that handles events to:
 * <ul>
 * <li>save test name on start of a test (this is used in the config map of
 * webDriver)</li>
 * <li>capture screenshots on test failure and for steps which contains
 * 'print'</li>
 * </ul>
 *
 * @author kkurian
 *
 */
public class TestReportListener implements ConcurrentEventListener {

	private static ThreadLocal<String> TEST_NAME = new ThreadLocal<>();

	public static String getTestName() {
		return TEST_NAME.get();
	}

	@Override
	public void setEventPublisher(final EventPublisher publisher) {
		publisher.registerHandlerFor(TestCaseStarted.class, this::handleTestCaseStarted);

		publisher.registerHandlerFor(TestStepFinished.class, this::handleTestStepFinished);
	}

	/**
	 * Handle cucumber TestCaseStarted event
	 *
	 * @param event
	 */
	private void handleTestCaseStarted(final TestCaseStarted event) {
		TEST_NAME.set(event.testCase.getName());
	}

	/**
	 * Handle cucumber TestStepFinished event
	 *
	 * @param event
	 */
	private void handleTestStepFinished(final TestStepFinished event) {
		final WebDriver webDriver = AutomationApplication.getWebDriver();
		if (webDriver instanceof TakesScreenshot && (isNotOk(event) || isPrint(event))) {
			saveScreenshot((TakesScreenshot) webDriver);
		}
	}

	/**
	 * Test result is not OK
	 *
	 * @param event
	 * @return
	 */
	private boolean isNotOk(final TestStepFinished event) {
		return !event.result.isOk(true);
	}

	/**
	 * Step succeeded and has print keyword
	 *
	 * @param event
	 * @return
	 */
	private boolean isPrint(final TestStepFinished event) {
		return event.result.isOk(true) && !Arrays.asList(Result.Type.SKIPPED).contains(event.result.getStatus())
				&& event.testStep instanceof PickleStepTestStep
				&& ((PickleStepTestStep) event.testStep).getStepText().contains("print");
	}

	/**
	 * Save screenshot to allure report
	 *
	 * @param webDriver
	 */
	private void saveScreenshot(final TakesScreenshot webDriver) {
		Allure.addAttachment("Screenshot", "image/png",
				new ByteArrayInputStream(webDriver.getScreenshotAs(OutputType.BYTES)), ".png");
	}
}
