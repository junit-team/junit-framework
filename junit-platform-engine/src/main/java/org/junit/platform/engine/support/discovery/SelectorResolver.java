/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.support.discovery;

import static java.util.Collections.emptySet;
import static org.apiguardian.api.API.Status.MAINTAINED;
import static org.apiguardian.api.API.Status.STABLE;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apiguardian.api.API;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.ClasspathResourceSelector;
import org.junit.platform.engine.discovery.ClasspathRootSelector;
import org.junit.platform.engine.discovery.DirectorySelector;
import org.junit.platform.engine.discovery.FileSelector;
import org.junit.platform.engine.discovery.IterationSelector;
import org.junit.platform.engine.discovery.MethodSelector;
import org.junit.platform.engine.discovery.ModuleSelector;
import org.junit.platform.engine.discovery.NestedClassSelector;
import org.junit.platform.engine.discovery.NestedMethodSelector;
import org.junit.platform.engine.discovery.PackageSelector;
import org.junit.platform.engine.discovery.UniqueIdSelector;
import org.junit.platform.engine.discovery.UriSelector;

/**
 * A resolver that supports resolving one or multiple types of
 * {@link DiscoverySelector DiscoverySelectors}.
 *
 * <p>An implementation of a {@code resolve()} method is typically comprised
 * of the following steps:
 *
 * <ol>
 *     <li>
 *         Check whether the selector is applicable for the current
 *         {@link org.junit.platform.engine.TestEngine} and the current
 *         {@link org.junit.platform.engine.EngineDiscoveryRequest} (e.g.
 *         for a test class: is it relevant for the current engine and does
 *         it pass all filters in the request?).
 *     </li>
 *     <li>
 *         If so, use the supplied {@link Context Context}, to add one or
 *         multiple {@link TestDescriptor TestDescriptors} to the designated
 *         parent (see {@link Context Context} for details) and return a
 *         {@linkplain Resolution#match(Match) match} or multiple {@linkplain
 *         Resolution#matches(Set) matches}. Alternatively, convert the supplied
 *         selector into one or multiple other
 *         {@linkplain Resolution#selectors(Set) selectors} (e.g. a {@link
 *         PackageSelector} into a set of {@link ClassSelector ClassSelectors}).
 *         Otherwise, return {@link Resolution#unresolved() unresolved()}.
 *     </li>
 * </ol>
 *
 * @since 1.5
 */
@API(status = STABLE, since = "1.10")
public interface SelectorResolver {

	/**
	 * Resolve the supplied {@link ClasspathResourceSelector} using the supplied
	 * {@link Context Context}.
	 *
	 * <p>The default implementation delegates to {@link
	 * #resolve(DiscoverySelector, Context)}.
	 *
	 * @param selector the selector to be resolved; never {@code null}
	 * @param context the context to be used for resolving the selector; never
	 * {@code null}
	 * @return a {@link Resolution Resolution} of {@link Resolution#unresolved()
	 * unresolved()}, {@link Resolution#selectors(Set) selectors()}, or {@link
	 * Resolution#matches(Set) matches()}; never {@code null}
	 * @see #resolve(DiscoverySelector, Context)
	 */
	default Resolution resolve(ClasspathResourceSelector selector, Context context) {
		return resolve((DiscoverySelector) selector, context);
	}

	/**
	 * Resolve the supplied {@link ClasspathRootSelector} using the supplied
	 * {@link Context Context}.
	 *
	 * <p>The default implementation delegates to {@link
	 * #resolve(DiscoverySelector, Context)}.
	 *
	 * @param selector the selector to be resolved; never {@code null}
	 * @param context the context to be used for resolving the selector; never
	 * {@code null}
	 * @return a {@link Resolution Resolution} of {@link Resolution#unresolved()
	 * unresolved()}, {@link Resolution#selectors(Set) selectors()}, or {@link
	 * Resolution#matches(Set) matches()}; never {@code null}
	 * @see #resolve(DiscoverySelector, Context)
	 */
	default Resolution resolve(ClasspathRootSelector selector, Context context) {
		return resolve((DiscoverySelector) selector, context);
	}

	/**
	 * Resolve the supplied {@link ClassSelector} using the supplied
	 * {@link Context Context}.
	 *
	 * <p>The default implementation delegates to {@link
	 * #resolve(DiscoverySelector, Context)}.
	 *
	 * @param selector the selector to be resolved; never {@code null}
	 * @param context the context to be used for resolving the selector; never
	 * {@code null}
	 * @return a {@link Resolution Resolution} of {@link Resolution#unresolved()
	 * unresolved()}, {@link Resolution#selectors(Set) selectors()}, or {@link
	 * Resolution#matches(Set) matches()}; never {@code null}
	 * @see #resolve(DiscoverySelector, Context)
	 */
	default Resolution resolve(ClassSelector selector, Context context) {
		return resolve((DiscoverySelector) selector, context);
	}

	/**
	 * Resolve the supplied {@link NestedClassSelector} using the supplied
	 * {@link Context Context}.
	 *
	 * <p>The default implementation delegates to {@link
	 * #resolve(DiscoverySelector, Context)}.
	 *
	 * @param selector the selector to be resolved; never {@code null}
	 * @param context the context to be used for resolving the selector; never
	 * {@code null}
	 * @return a {@link Resolution Resolution} of {@link Resolution#unresolved()
	 * unresolved()}, {@link Resolution#selectors(Set) selectors()}, or {@link
	 * Resolution#matches(Set) matches()}; never {@code null}
	 * @see #resolve(DiscoverySelector, Context)
	 */
	default Resolution resolve(NestedClassSelector selector, Context context) {
		return resolve((DiscoverySelector) selector, context);
	}

	/**
	 * Resolve the supplied {@link DirectorySelector} using the supplied
	 * {@link Context Context}.
	 *
	 * <p>The default implementation delegates to {@link
	 * #resolve(DiscoverySelector, Context)}.
	 *
	 * @param selector the selector to be resolved; never {@code null}
	 * @param context the context to be used for resolving the selector; never
	 * {@code null}
	 * @return a {@link Resolution Resolution} of {@link Resolution#unresolved()
	 * unresolved()}, {@link Resolution#selectors(Set) selectors()}, or {@link
	 * Resolution#matches(Set) matches()}; never {@code null}
	 * @see #resolve(DiscoverySelector, Context)
	 */
	default Resolution resolve(DirectorySelector selector, Context context) {
		return resolve((DiscoverySelector) selector, context);
	}

	/**
	 * Resolve the supplied {@link FileSelector} using the supplied
	 * {@link Context Context}.
	 *
	 * <p>The default implementation delegates to {@link
	 * #resolve(DiscoverySelector, Context)}.
	 *
	 * @param selector the selector to be resolved; never {@code null}
	 * @param context the context to be used for resolving the selector; never
	 * {@code null}
	 * @return a {@link Resolution Resolution} of {@link Resolution#unresolved()
	 * unresolved()}, {@link Resolution#selectors(Set) selectors()}, or {@link
	 * Resolution#matches(Set) matches()}; never {@code null}
	 * @see #resolve(DiscoverySelector, Context)
	 */
	default Resolution resolve(FileSelector selector, Context context) {
		return resolve((DiscoverySelector) selector, context);
	}

	/**
	 * Resolve the supplied {@link MethodSelector} using the supplied
	 * {@link Context Context}.
	 *
	 * <p>The default implementation delegates to {@link
	 * #resolve(DiscoverySelector, Context)}.
	 *
	 * @param selector the selector to be resolved; never {@code null}
	 * @param context the context to be used for resolving the selector; never
	 * {@code null}
	 * @return a {@link Resolution Resolution} of {@link Resolution#unresolved()
	 * unresolved()}, {@link Resolution#selectors(Set) selectors()}, or {@link
	 * Resolution#matches(Set) matches()}; never {@code null}
	 * @see #resolve(DiscoverySelector, Context)
	 */
	default Resolution resolve(MethodSelector selector, Context context) {
		return resolve((DiscoverySelector) selector, context);
	}

	/**
	 * Resolve the supplied {@link NestedMethodSelector} using the supplied
	 * {@link Context Context}.
	 *
	 * <p>The default implementation delegates to {@link
	 * #resolve(DiscoverySelector, Context)}.
	 *
	 * @param selector the selector to be resolved; never {@code null}
	 * @param context the context to be used for resolving the selector; never
	 * {@code null}
	 * @return a {@link Resolution Resolution} of {@link Resolution#unresolved()
	 * unresolved()}, {@link Resolution#selectors(Set) selectors()}, or {@link
	 * Resolution#matches(Set) matches()}; never {@code null}
	 * @see #resolve(DiscoverySelector, Context)
	 */
	default Resolution resolve(NestedMethodSelector selector, Context context) {
		return resolve((DiscoverySelector) selector, context);
	}

	/**
	 * Resolve the supplied {@link ModuleSelector} using the supplied
	 * {@link Context Context}.
	 *
	 * <p>The default implementation delegates to {@link
	 * #resolve(DiscoverySelector, Context)}.
	 *
	 * @param selector the selector to be resolved; never {@code null}
	 * @param context the context to be used for resolving the selector; never
	 * {@code null}
	 * @return a {@link Resolution Resolution} of {@link Resolution#unresolved()
	 * unresolved()}, {@link Resolution#selectors(Set) selectors()}, or {@link
	 * Resolution#matches(Set) matches()}; never {@code null}
	 * @see #resolve(DiscoverySelector, Context)
	 */
	default Resolution resolve(ModuleSelector selector, Context context) {
		return resolve((DiscoverySelector) selector, context);
	}

	/**
	 * Resolve the supplied {@link PackageSelector} using the supplied
	 * {@link Context Context}.
	 *
	 * <p>The default implementation delegates to {@link
	 * #resolve(DiscoverySelector, Context)}.
	 *
	 * @param selector the selector to be resolved; never {@code null}
	 * @param context the context to be used for resolving the selector; never
	 * {@code null}
	 * @return a {@link Resolution Resolution} of {@link Resolution#unresolved()
	 * unresolved()}, {@link Resolution#selectors(Set) selectors()}, or {@link
	 * Resolution#matches(Set) matches()}; never {@code null}
	 * @see #resolve(DiscoverySelector, Context)
	 */
	default Resolution resolve(PackageSelector selector, Context context) {
		return resolve((DiscoverySelector) selector, context);
	}

	/**
	 * Resolve the supplied {@link UniqueIdSelector} using the supplied
	 * {@link Context Context}.
	 *
	 * <p>The default implementation delegates to {@link
	 * #resolve(DiscoverySelector, Context)}.
	 *
	 * @param selector the selector to be resolved; never {@code null}
	 * @param context the context to be used for resolving the selector; never
	 * {@code null}
	 * @return a {@link Resolution Resolution} of {@link Resolution#unresolved()
	 * unresolved()}, {@link Resolution#selectors(Set) selectors()}, or {@link
	 * Resolution#matches(Set) matches()}; never {@code null}
	 * @see #resolve(DiscoverySelector, Context)
	 */
	default Resolution resolve(UniqueIdSelector selector, Context context) {
		return resolve((DiscoverySelector) selector, context);
	}

	/**
	 * Resolve the supplied {@link UriSelector} using the supplied
	 * {@link Context Context}.
	 *
	 * <p>The default implementation delegates to {@link
	 * #resolve(DiscoverySelector, Context)}.
	 *
	 * @param selector the selector to be resolved; never {@code null}
	 * @param context the context to be used for resolving the selector; never
	 * {@code null}
	 * @return a {@link Resolution Resolution} of {@link Resolution#unresolved()
	 * unresolved()}, {@link Resolution#selectors(Set) selectors()}, or {@link
	 * Resolution#matches(Set) matches()}; never {@code null}
	 * @see #resolve(DiscoverySelector, Context)
	 */
	default Resolution resolve(UriSelector selector, Context context) {
		return resolve((DiscoverySelector) selector, context);
	}

	/**
	 * Resolve the supplied {@link IterationSelector} using the supplied
	 * {@link Context Context}.
	 *
	 * <p>The default implementation delegates to {@link
	 * #resolve(DiscoverySelector, Context)}.
	 *
	 * @param selector the selector to be resolved; never {@code null}
	 * @param context the context to be used for resolving the selector; never
	 * {@code null}
	 * @return a {@link Resolution Resolution} of {@link Resolution#unresolved()
	 * unresolved()}, {@link Resolution#selectors(Set) selectors()}, or {@link
	 * Resolution#matches(Set) matches()}; never {@code null}
	 * @see #resolve(DiscoverySelector, Context)
	 */
	@API(status = MAINTAINED, since = "1.13.3")
	default Resolution resolve(IterationSelector selector, Context context) {
		return resolve((DiscoverySelector) selector, context);
	}

	/**
	 * Resolve the supplied {@link DiscoverySelector} using the supplied
	 * {@link Context Context}.
	 *
	 * <p>This method is only called if none of the overloaded variants match.
	 *
	 * <p>The default implementation returns {@link Resolution#unresolved()
	 * unresolved()}.
	 *
	 * @param selector the selector to be resolved; never {@code null}
	 * @param context the context to be used for resolving the selector; never
	 * {@code null}
	 * @return a {@link Resolution Resolution} of {@link Resolution#unresolved()
	 * unresolved()}, {@link Resolution#selectors(Set) selectors()}, or {@link
	 * Resolution#matches(Set) matches()}; never {@code null}
	 * @see Context
	 */
	default Resolution resolve(DiscoverySelector selector, Context context) {
		return Resolution.unresolved();
	}

	/**
	 * The context for resolving a {@link DiscoverySelector} and adding it to
	 * the test tree.
	 *
	 * <p>The context is used to add resolved {@link TestDescriptor
	 * TestDescriptors} to the test tree if and only if the parent
	 * {@code TestDescriptor} could be found. Alternatively, a resolver may
	 * use the context to {@linkplain #resolve(DiscoverySelector) resolve} a
	 * certain {@code DiscoverySelector} into a {@code TestDescriptor} (e.g. for
	 * adding a filter and returning a {@linkplain Match#partial(TestDescriptor)
	 * partial match}).
	 *
	 * @since 1.5
	 * @see SelectorResolver
	 */
	@API(status = STABLE, since = "1.10")
	interface Context {

		/**
		 * Resolve the supplied {@link TestDescriptor}, if possible.
		 *
		 * <p>Calling this method has the same effect as returning a {@linkplain
		 * Match#partial(TestDescriptor) partial match} from a {@link
		 * SelectorResolver}: the children of the resulting {@link
		 * TestDescriptor} will only be resolved if a subsequent resolution
		 * finds an exact match that contains a {@code TestDescriptor} with the
		 * same {@linkplain TestDescriptor#getUniqueId() unique ID}.
		 *
		 * @param selector the selector to resolve
		 * @return the resolved {@code TestDescriptor}; never {@code null} but
		 * potentially empty
		 */
		Optional<TestDescriptor> resolve(DiscoverySelector selector);

		/**
		 * Add a {@link TestDescriptor} to an unspecified parent, usually the
		 * engine descriptor, by applying the supplied {@code Function} to the
		 * new parent.
		 *
		 * <p>The parent will be the engine descriptor unless another parent has
		 * already been determined, i.e. if the selector that is being resolved
		 * is the result of {@linkplain Match#expand() expanding} a {@link
		 * Match}.
		 *
		 * <p>If the result of applying the {@code Function} is {@linkplain
		 * Optional#isPresent() present}, it will be added as a child of the
		 * parent {@code TestDescriptor} unless a descriptor with the same
		 * {@linkplain TestDescriptor#getUniqueId() unique ID} was added
		 * earlier.
		 *
		 * @param creator {@code Function} that will be called with the new
		 * parent to determine the new {@code TestDescriptor} to be added; must
		 * not return {@code null}
		 * @param <T> the type of the new {@code TestDescriptor}
		 * @return the new {@code TestDescriptor} or the previously existing one
		 * with the same unique ID; never {@code null} but potentially empty
		 * @throws ClassCastException if the previously existing {@code
		 * TestDescriptor} is not an instance of {@code T}
		 */
		<T extends TestDescriptor> Optional<T> addToParent(Function<TestDescriptor, Optional<T>> creator);

		/**
		 * Add a {@link TestDescriptor} to a parent, specified by the {@link
		 * DiscoverySelector} returned by the supplied {@code Supplier}, by
		 * applying the supplied {@code Function} to the new parent.
		 *
		 * <p>Unless another parent has already been determined, i.e. if the
		 * selector that is being resolved is the result of {@linkplain
		 * Match#expand() expanding} a {@link Match}, the {@link
		 * DiscoverySelector} returned by the supplied {@code Supplier} will
		 * be used to determine the parent. If no parent is found, the supplied
		 * {@code Function} will not be called. If there are multiple potential
		 * parents, an exception will be thrown. Otherwise, the resolved
		 * {@code TestDescriptor} will be used as the parent and passed to the
		 * supplied {@code Function}.
		 *
		 * <p>If the result of applying the {@code Function} is {@linkplain
		 * Optional#isPresent() present}, it will be added as a child of the
		 * parent {@code TestDescriptor} unless a descriptor with the same
		 * {@linkplain TestDescriptor#getUniqueId() unique ID} was added
		 * earlier.
		 *
		 * @param creator {@code Function} that will be called with the new
		 * parent to determine the new {@code TestDescriptor} to be added; must
		 * not return {@code null}
		 * @param <T> the type of the new {@code TestDescriptor}
		 * @return the new {@code TestDescriptor} or the previously existing one
		 * with the same unique ID; never {@code null} but potentially empty
		 * @throws ClassCastException if the previously existing {@code
		 * TestDescriptor} is not an instance of {@code T}
		 */
		<T extends TestDescriptor> Optional<T> addToParent(Supplier<DiscoverySelector> parentSelectorSupplier,
				Function<TestDescriptor, Optional<T>> creator);

	}

	/**
	 * The result of an attempt to resolve a {@link DiscoverySelector}.
	 *
	 * <p>A resolution is either {@linkplain #unresolved unresolved}, contains a
	 * {@linkplain #match match} or multiple {@linkplain #matches}, or a set of
	 * {@linkplain #selectors selectors}.
	 *
	 * @since 1.5
	 * @see SelectorResolver
	 */
	@API(status = STABLE, since = "1.10")
	class Resolution {

		private static final Resolution UNRESOLVED = new Resolution(emptySet(), emptySet());

		private final Set<Match> matches;
		private final Set<? extends DiscoverySelector> selectors;

		/**
		 * Factory for creating <em>unresolved</em> resolutions.
		 *
		 * @return an <em>unresolved</em> resolution; never {@code null}
		 */
		public static Resolution unresolved() {
			return UNRESOLVED;
		}

		/**
		 * Factory for creating a resolution that contains the supplied
		 * {@link Match Match}.
		 *
		 * @param match the resolved {@code Match}; never {@code null}
		 * @return a resolution that contains the supplied {@code Match}; never
		 * {@code null}
		 */
		public static Resolution match(Match match) {
			Preconditions.notNull(match, "match must not be null");
			return new Resolution(Set.of(match), emptySet());
		}

		/**
		 * Factory for creating a resolution that contains the supplied
		 * {@link Match Matches}.
		 *
		 * @param matches the resolved {@code Matches}; never {@code null} or
		 * empty
		 * @return a resolution that contains the supplied {@code Matches};
		 * never {@code null}
		 */
		public static Resolution matches(Set<Match> matches) {
			Preconditions.notNull(matches, "matches must not be null");
			Preconditions.notEmpty(matches, "matches must not be empty");
			Preconditions.containsNoNullElements(matches, "matches must not contain null elements");
			return new Resolution(matches, emptySet());
		}

		/**
		 * Factory for creating a resolution that contains the supplied
		 * {@link DiscoverySelector DiscoverySelectors}.
		 *
		 * @param selectors the resolved {@code DiscoverySelectors}; never
		 * {@code null} or empty
		 * @return a resolution that contains the supplied
		 * {@code DiscoverySelectors}; never {@code null}
		 */
		public static Resolution selectors(Set<? extends DiscoverySelector> selectors) {
			Preconditions.notNull(selectors, "selectors must not be null");
			Preconditions.notEmpty(selectors, "selectors must not be empty");
			Preconditions.containsNoNullElements(selectors, "selectors must not contain null elements");
			return new Resolution(emptySet(), selectors);
		}

		private Resolution(Set<Match> matches, Set<? extends DiscoverySelector> selectors) {
			this.matches = matches;
			this.selectors = selectors;
		}

		/**
		 * Whether this resolution contains matches or selectors.
		 *
		 * @return {@code true} if this resolution contains matches or selectors
		 */
		public boolean isResolved() {
			return this != UNRESOLVED;
		}

		/**
		 * Returns the matches contained by this resolution.
		 *
		 * @return the set of matches; never {@code null} but potentially empty
		 */
		public Set<Match> getMatches() {
			return matches;
		}

		/**
		 * Returns the selectors contained by this resolution.
		 *
		 * @return the set of selectors; never {@code null} but potentially empty
		 */
		public Set<? extends DiscoverySelector> getSelectors() {
			return selectors;
		}

	}

	/**
	 * An exact or partial match for resolving a {@link DiscoverySelector} into
	 * a {@link TestDescriptor}.
	 *
	 * <p>A match is <em>exact</em> if the {@link DiscoverySelector} directly
	 * represents the resulting {@link TestDescriptor}, e.g. if a
	 * {@link ClassSelector} was resolved into the {@link TestDescriptor} that
	 * represents the test class. It is <em>partial</em> if the matching
	 * {@link TestDescriptor} does not directly correspond to the resolved
	 * {@link DiscoverySelector}, e.g. when resolving a {@link UniqueIdSelector}
	 * that represents a dynamic child of the resolved {@link TestDescriptor}.
	 *
	 * <p>In addition to the {@link TestDescriptor}, a match may contain a
	 * {@code Supplier} of {@link DiscoverySelector DiscoverySelectors} that may
	 * be used to discover the children of the {@link TestDescriptor}. The
	 * algorithm implemented by {@link EngineDiscoveryRequestResolver}
	 * {@linkplain #expand() expands} all exact matches immediately, i.e. it
	 * resolves all of their children. Partial matches will only be expanded in
	 * case a subsequent resolution finds an exact match that contains a {@link
	 * TestDescriptor} with the same {@linkplain TestDescriptor#getUniqueId()
	 * unique ID}.
	 *
	 * @since 1.5
	 * @see SelectorResolver
	 * @see Resolution#match(Match)
	 * @see Resolution#matches(Set)
	 */
	@API(status = STABLE, since = "1.10")
	class Match {

		private final TestDescriptor testDescriptor;
		private final Supplier<Set<? extends DiscoverySelector>> childSelectorsSupplier;
		private final Type type;

		/**
		 * Factory for creating an exact match without any children.
		 *
		 * @param testDescriptor the resolved {@code TestDescriptor}; never
		 * {@code null}
		 * @return a match that contains the supplied {@code TestDescriptor};
		 * never {@code null}
		 */
		public static Match exact(TestDescriptor testDescriptor) {
			return exact(testDescriptor, Collections::emptySet);
		}

		/**
		 * Factory for creating an exact match with potential children.
		 *
		 * @param testDescriptor the resolved {@code TestDescriptor}; never
		 * {@code null}
		 * @param childSelectorsSupplier a {@code Supplier} of children
		 * selectors that will be resolved when this match is expanded; never
		 * {@code null}
		 * @return a match that contains the supplied {@code TestDescriptor};
		 * never {@code null}
		 */
		public static Match exact(TestDescriptor testDescriptor,
				Supplier<Set<? extends DiscoverySelector>> childSelectorsSupplier) {
			return new Match(testDescriptor, childSelectorsSupplier, Type.EXACT);
		}

		/**
		 * Factory for creating a partial match without any children.
		 *
		 * @param testDescriptor the resolved {@code TestDescriptor}; never
		 * {@code null}
		 * @return a match that contains the supplied {@code TestDescriptor};
		 * never {@code null}
		 */
		public static Match partial(TestDescriptor testDescriptor) {
			return partial(testDescriptor, Collections::emptySet);
		}

		/**
		 * Factory for creating a partial match with potential children.
		 *
		 * @param testDescriptor the resolved {@code TestDescriptor}; never
		 * {@code null}
		 * @param childSelectorsSupplier a {@code Supplier} of children
		 * selectors that will be resolved when this match is expanded; never
		 * {@code null}
		 * @return a match that contains the supplied {@code TestDescriptor};
		 * never {@code null}
		 */
		public static Match partial(TestDescriptor testDescriptor,
				Supplier<Set<? extends DiscoverySelector>> childSelectorsSupplier) {
			return new Match(testDescriptor, childSelectorsSupplier, Type.PARTIAL);
		}

		private Match(TestDescriptor testDescriptor, Supplier<Set<? extends DiscoverySelector>> childSelectorsSupplier,
				Type type) {
			this.testDescriptor = Preconditions.notNull(testDescriptor, "testDescriptor must not be null");
			this.childSelectorsSupplier = Preconditions.notNull(childSelectorsSupplier,
				"childSelectorsSupplier must not be null");
			this.type = type;
		}

		/**
		 * Whether this match is exact.
		 *
		 * @return {@code true} if this match is exact; {@code false} if it's
		 * partial
		 */
		public boolean isExact() {
			return type == Type.EXACT;
		}

		/**
		 * Get the contained {@link TestDescriptor}.
		 *
		 * @return the contained {@code TestDescriptor}; never {@code null}
		 */
		public TestDescriptor getTestDescriptor() {
			return testDescriptor;
		}

		/**
		 * Expand this match in order to resolve the children of the contained
		 * {@link TestDescriptor}.
		 *
		 * @return the set of {@code DiscoverySelectors} that represent the
		 * children of the contained {@code TestDescriptor}; never {@code null}
		 */
		public Set<? extends DiscoverySelector> expand() {
			return childSelectorsSupplier.get();
		}

		private enum Type {
			EXACT, PARTIAL
		}

	}

}
