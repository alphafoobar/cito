/*
 * Copyright 2016-2017 Daniel Siviter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cito;

import static java.util.Objects.requireNonNull;

import java.util.Objects;
import java.util.regex.Matcher;

import javax.annotation.Nonnull;

import cito.annotation.PathParam;

/**
 * Utility for parsing paths and performing parameters expansion.
 * 
 * @author Daniel Siviter
 * @since v1.0 [17 Jan 2017]
 * @see Glob
 * @see PathParam
 */
public class PathParser {
	private final String pattern;
	private final Glob glob;

	/**
	 * Create a parser.
	 * 
	 * @param pattern the pattern to parse.
	 */
	public PathParser(@Nonnull String pattern) {
		this.glob = Glob.from(requireNonNull(this.pattern = pattern));
	}

	/**
	 * 
	 * @param path the path to check.
	 * @return the parse result.
	 */
	public Result parse(CharSequence path) {
		final Matcher matcher = this.glob.compiled().matcher(path);
		return new Result(matcher);
	}

	@Override
	public String toString() {
		return super.toString() + "[" + this.pattern + "]";
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.pattern);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null ||getClass() != obj.getClass())
			return false;
		PathParser other = (PathParser) obj;
		return Objects.equals(this.pattern, other.pattern);
	}


	// --- Static Methods ---

	/**
	 * 
	 * @param pattern
	 * @return
	 */
	public static PathParser create(String pattern) {
		return new PathParser(pattern);
	}

	/**
	 * 
	 * @param pattern
	 * @param path
	 * @return
	 */
	public static Result parse(String pattern, String path) {
		return create(pattern).parse(path);
	}


	// --- Inner Classes ---

	/**
	 * 
	 * @author Daniel Siviter
	 * @since v1.0 [17 Jan 2017]
	 */
	public static class Result {
		private final Matcher matcher;

		/**
		 * 
		 * @param matcher
		 */
		public Result(Matcher matcher) {
			this.matcher = requireNonNull(matcher);
		}

		/**
		 * 
		 * @return
		 */
		public boolean isSuccess() {
			return matcher.matches();
		}

		/**
		 * 
		 * @param name
		 * @return
		 */
		public String get(String name) {
			return this.matcher.group(name);
		}
	}
}
