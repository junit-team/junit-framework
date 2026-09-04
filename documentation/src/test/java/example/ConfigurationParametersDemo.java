package example;

import org.junit.platform.configuration.api.ConfigurationParameter;
import org.junit.platform.configuration.api.ConfigurationParameter.Value;

public class ConfigurationParametersDemo {

	enum ExecutionMode {
		FIXED, DYNAMIC, CUSTOM
	}
	// tag::user_guide[]
	/**
	 * A brief description of this property.
	 */
	@ConfigurationParameter(type = ExecutionMode.class, defaultValue = @Value(stringValue = "fixed"))
	public static final String EXECUTION_MODE_PROPERTY_NAME = "org.example.execution-mode";
	// end::user_guide[]
}
