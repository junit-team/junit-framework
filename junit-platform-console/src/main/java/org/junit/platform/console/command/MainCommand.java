/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.console.command;

import static java.util.Objects.requireNonNull;

import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.junit.platform.console.options.AnsiColorOptionMixin;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.IExitCodeGenerator;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Spec;

@Command(//
		name = "junit", //
		abbreviateSynopsis = true, //
		synopsisSubcommandLabel = "COMMAND", //
		sortOptions = false, //
		usageHelpWidth = 95, //
		showAtFileInUsageHelp = true, //
		usageHelpAutoWidth = true, //
		description = "Launches the JUnit Platform for test discovery and execution.", //
		footerHeading = "%n", //
		footer = "For more information, please refer to the JUnit User Guide at%n" //
				+ "@|underline https://docs.junit.org/${junit.docs.version}/|@", //
		scope = CommandLine.ScopeType.INHERIT, //
		exitCodeOnInvalidInput = ExitCode.INVALID_INPUT, //
		exitCodeOnExecutionException = ExitCode.ANY_ERROR, //
		versionProvider = ManifestVersionProvider.class //
)
class MainCommand implements Runnable, IExitCodeGenerator {

	private final ConsoleTestExecutor.Factory consoleTestExecutorFactory;

	@Option(names = { "-h", "--help" }, help = true, description = "Display help information.")
	private boolean helpRequested;

	@Option(names = "--version", versionHelp = true, description = "Display version information.")
	private boolean versionRequested;

	@Mixin
	AnsiColorOptionMixin ansiColorOption;

	@Spec
	CommandSpec commandSpec;

	@Nullable
	CommandResult<?> commandResult;

	MainCommand(ConsoleTestExecutor.Factory consoleTestExecutorFactory) {
		this.consoleTestExecutorFactory = consoleTestExecutorFactory;
	}

	@Override
	public void run() {
		if (helpRequested) {
			commandSpec.commandLine().usage(commandSpec.commandLine().getOut());
			commandResult = CommandResult.success();
		}
		else if (versionRequested) {
			commandSpec.commandLine().printVersionHelp(commandSpec.commandLine().getOut());
			commandResult = CommandResult.success();
		}
		else {
			throw new ParameterException(commandSpec.commandLine(), "Missing required subcommand");
		}
	}

	@Override
	public int getExitCode() {
		return requireNonNull(commandResult).getExitCode();
	}

	CommandResult<?> run(String[] args,
			@SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<OutputStreamConfig> outputStreamConfig) {
		CommandLine commandLine = new CommandLine(this) //
				.addSubcommand(new DiscoverTestsCommand(consoleTestExecutorFactory)) //
				.addSubcommand(new ExecuteTestsCommand(consoleTestExecutorFactory)) //
				.addSubcommand(new ListTestEnginesCommand());
		return runCommand(commandLine, args, outputStreamConfig);
	}

	private static CommandResult<?> runCommand(CommandLine commandLine, String[] args,
			@SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<OutputStreamConfig> outputStreamConfig) {
		BaseCommand.initialize(commandLine);
		outputStreamConfig.ifPresent(it -> it.applyTo(commandLine));
		int exitCode = commandLine.execute(args);
		return CommandResult.create(exitCode, getLikelyExecutedCommand(commandLine).getExecutionResult());
	}

	/**
	 * Get the most likely executed subcommand, if any, or the main command otherwise.
	 * @see <a href="https://picocli.info/#_executing_commands_with_subcommands">Executing Commands with Subcommands</a>
	 */
	private static CommandLine getLikelyExecutedCommand(final CommandLine commandLine) {
		return Optional.ofNullable(commandLine.getParseResult().subcommand()) //
				.map(parseResult -> parseResult.commandSpec().commandLine()) //
				.orElse(commandLine);
	}

}
