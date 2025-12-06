plugins {
	id("org.openrewrite.rewrite")
}

dependencies {
	rewrite("org.openrewrite.recipe:rewrite-static-analysis:2.22.0")
}

rewrite {
	activeRecipe("org.junit.openrewrite.SanityCheck")
	configFile = project.getRootProject().file("gradle/config/rewrite.yml")
	exclusion(
		// JupiterBestPractices: class scope issue;
		"**AggregatorIntegrationTests.java",
		"**BeforeAndAfterSuiteTests.java",
		"**BridgeMethods.java",
		"**CsvArgumentsProvider.java",
		"**DefaultArgumentsAccessor.java",
		"**DiscoverySelectorResolverTests.java",
		"**DiscoveryTests.java",
		"**DisplayNameGenerationTests.java",
		"**DynamicNodeGenerationTests.java",
		"**DynamicTestTests.java",
		"**EngineDiscoveryResultValidatorTests.java", // fixable with @DisabledOnOs(WINDOWS)
		"**ExceptionHandlingTests.java",
		"**ExecutionCancellationTests.java",
		"**ExtensionRegistrationViaParametersAndFieldsTests.java",
		"**InvocationInterceptorTests.java",
		"**IsTestMethodTests.java",
		"**IsTestTemplateMethodTests.java",
		"**JupiterTestDescriptorTests.java",
		"**LifecycleMethodUtilsTests.java",
		"**MultipleTestableAnnotationsTests.java",
		"**NestedContainerEventConditionTests.java",
		"**ParallelExecutionIntegrationTests.java",
		"**ParameterResolverTests.java",
		"**ParameterizedTestIntegrationTests.java",
		"**RepeatedTestTests.java",
		"**StaticPackagePrivateBeforeMethod.java",
		"**SubclassedAssertionsTests.java",
		"**TempDirectoryCleanupTests.java",
		"**TestCase.java",
		"**TestCases.java",
		"**TestExecutionExceptionHandlerTests.java",
		"**TestInstanceFactoryTests.java",
		"**TestTemplateInvocationTestDescriptorTests.java",
		"**TestTemplateInvocationTests.java",
		"**TestTemplateTestDescriptorTests.java",
		"**TestWatcherTests.java",
		"**TimeoutExtensionTests.java",
		"**UniqueIdTrackingListenerIntegrationTests.java",
		"**WorkerThreadPoolHierarchicalTestExecutorServiceTests.java",
		"**org/junit/jupiter/engine/bridge**",
		// trivial import fix.
		"**Assert**AssertionsTests.java",
		"**DynamicContainerTests.java",
		// legacy
		"**documentation/src/test/java/example**",
		"**testFixtures/java/org/junit/vintage/engine/samples**",
	)
	setExportDatatables(true)
	setFailOnDryRunResults(true)
}
