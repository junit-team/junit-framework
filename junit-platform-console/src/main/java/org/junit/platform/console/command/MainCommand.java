/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.console.command;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

import org.junit.platform.console.options.AnsiColorOptionMixin;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.ColorScheme;
import picocli.CommandLine.IExitCodeGenerator;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Unmatched;

@Command(//
		name = "junit", //
		abbreviateSynopsis = true, //
		sortOptions = false, //
		usageHelpWidth = 95, //
		showAtFileInUsageHelp = true, //
		usageHelpAutoWidth = true, //
		description = "Launches the JUnit Platform for test discovery and execution.", //
		footerHeading = "%n", //
		footer = "For more information, please refer to the JUnit User Guide at%n" //
				+ "@|underline https://docs.junit.org/${junit.docs.version}/user-guide/|@", //
		scope = CommandLine.ScopeType.INHERIT, //
		exitCodeOnInvalidInput = CommandResult.FAILURE, //
		exitCodeOnExecutionException = CommandResult.FAILURE, //
		versionProvider = ManifestVersionProvider.class //
)
class MainCommand implements Callable<Object>, IExitCodeGenerator {

	private final ConsoleTestExecutor.Factory consoleTestExecutorFactory;

	@Option(names = { "-h", "--help" }, help = true, description = "Display help information.")
	private boolean helpRequested;

	@Option(names = { "--h", "-help" }, help = true, hidden = true)
	private boolean helpRequested2;

	@Option(names = "--version", versionHelp = true, description = "Display version information.")
	private boolean versionHelpRequested;

	@Mixin
	AnsiColorOptionMixin ansiColorOption;

	@Unmatched
	private List<String> allParameters = new ArrayList<>();

	@Spec
	CommandSpec commandSpec;

	CommandResult<?> commandResult;

	MainCommand(ConsoleTestExecutor.Factory consoleTestExecutorFactory) {
		this.consoleTestExecutorFactory = consoleTestExecutorFactory;
	}

	@Override
	public Object call() {
		if (helpRequested || helpRequested2) {
			commandSpec.commandLine().usage(commandSpec.commandLine().getOut());
			commandResult = CommandResult.success();
			return null;
		}
		if (versionHelpRequested) {
			commandSpec.commandLine().printVersionHelp(commandSpec.commandLine().getOut());
			commandResult = CommandResult.success();
			return null;
		}
		if (allParameters.contains("--list-engines")) {
			return runCommand("engines", Optional.of("--list-engines"));
		}
		return runCommand("execute", Optional.empty());
	}

	@Override
	public int getExitCode() {
		return commandResult.getExitCode();
	}

	@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
	private Object runCommand(String subcommand, Optional<String> triggeringOption) {
		CommandLine commandLine = commandSpec.commandLine();
		commandLine.setUnmatchedArgumentsAllowed(false);
		Object command = commandLine.getSubcommands().get(subcommand).getCommandSpec().userObject();

		List<String> args = new ArrayList<>(commandLine.getParseResult().expandedArgs());
		triggeringOption.ifPresent(args::remove);
		CommandResult<?> result = runCommand( //
			new CommandLine(command), //
			args.toArray(new String[0]), //
			Optional.of(new OutputStreamConfig(commandLine)) //
		);
		this.commandResult = result;

		printDeprecationWarning(subcommand, triggeringOption, commandLine);

		return result.getValue().orElse(null);
	}

	private static void printDeprecationWarning(String subcommand, Optional<String> triggeringOption,
			CommandLine commandLine) {
		PrintWriter err = commandLine.getErr();
		String reason = triggeringOption.map(it -> " due to use of '" + it + "'").orElse("");

		commandLine.getOut().flush();
		err.println();
		ColorScheme colorScheme = commandLine.getColorScheme();
		err.println(colorScheme.string(
			String.format("@|yellow,bold WARNING:|@ Delegated to the '%s' command%s.", subcommand, reason)));
		err.println(
			colorScheme.string("         This behaviour has been deprecated and will be removed in a future release."));
		err.println(colorScheme.string("         Please use the '" + subcommand + "' command directly."));
		err.flush();
	}

	CommandResult<?> run(String[] args, Optional<OutputStreamConfig> outputStreamConfig) {
		CommandLine commandLine = new CommandLine(this) //
				.addSubcommand(new DiscoverTestsCommand(consoleTestExecutorFactory)) //
				.addSubcommand(new ExecuteTestsCommand(consoleTestExecutorFactory)) //
				.addSubcommand(new ListTestEnginesCommand());
		return runCommand(commandLine, args, outputStreamConfig);
	}

	private static CommandResult<?> runCommand(CommandLine commandLine, String[] args,
			Optional<OutputStreamConfig> outputStreamConfig) {
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
