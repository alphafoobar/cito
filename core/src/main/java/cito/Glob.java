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

import java.util.Map;
import java.util.WeakHashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.annotation.Nonnull;

/**
 * A class for POSIX GLOB pattern with brace expansions.
 *
 * Altered to be useful with message destinations rather than pure File paths.
 */
public class Glob {
	private static final char BACKSLASH = '\\';
	private static final Map<String, Glob> GLOBS = new WeakHashMap<>();

	private Pattern compiled;
	private boolean hasWildcard;

	/**
	 * Construct the glob pattern object with a glob pattern string
	 *
	 * @param globPattern the glob pattern string
	 */
	public Glob(@Nonnull String globPattern) {
		set(globPattern);
	}

	/**
	 * @return the compiled pattern
	 */
	public Pattern compiled() {
		return compiled;
	}

	/**
	 * Match input against the compiled glob pattern
	 *
	 * @param s input chars
	 * @return true for successful matches
	 */
	public boolean matches(CharSequence s) {
		return s != null && compiled.matcher(s).matches();
	}

	/**
	 * Set and compile a glob pattern
	 *
	 * @param glob  the glob pattern string
	 */
	public void set(@Nonnull String glob) {
		StringBuilder regex = new StringBuilder();
		int setOpen = 0;
		int curlyOpen = 0;
		int len = glob.length();
		hasWildcard = false;

		for (int i = 0; i < len; i++) {
			char c = glob.charAt(i);

			switch (c) {
			case BACKSLASH:
				if (++i >= len) {
					throw new PatternSyntaxException("Missing escaped character", glob, i);
				}
				regex.append(c).append(glob.charAt(i));
				break;
			case '.':
			case '$':
			case '(':
			case ')':
			case '|':
			case '+':
				// escape regex special chars that are not glob special chars
				regex.append(BACKSLASH).append(c);
				break;
			case '*':
				regex.append('.').append(c);
				hasWildcard = true;
				break;
			case '?':
				regex.append('.');
				hasWildcard = true;
				break;
			case '{': // start of a group
				regex.append("(?<"); // non-capturing
				curlyOpen++;
				hasWildcard = true;
				break;
			case ',':
				if (curlyOpen > 0) {
					throw new PatternSyntaxException("Invalid comma", glob, i);
				}
				regex.append(c);
				break;
			case '}':
				if (curlyOpen <= 0) {
					throw new PatternSyntaxException("Unexpected group close", glob, i);
				}
				// end of a group
				curlyOpen--;
				regex.append(">[A-Za-z0-9\\-\\_]*)");
				break;
			case '[':
				if (setOpen > 0) {
					throw new PatternSyntaxException("Unexpected character class", glob, i);
				}
				setOpen++;
				hasWildcard = true;
				regex.append(c);
				break;
			case '^': // ^ inside [...] can be unescaped
				if (setOpen == 0) {
					regex.append(BACKSLASH);
				}
				regex.append(c);
				break;
			case '!': // [! needs to be translated to [^
				char previousChar = i == 0 ?  0 : glob.charAt(i - 1);
				regex.append(setOpen > 0 && '[' == previousChar ? '^' : '!');
				break;
			case ']':
				// Many set errors like [][] could not be easily detected here,
				// as []], []-] and [-] are all valid POSIX glob and java regex.
				// We'll just let the regex compiler do the real work.
				setOpen = 0;
				regex.append(c);
				break;
			default:
				regex.append(c);
				break;
			}
		}

		if (setOpen > 0) {
			throw new PatternSyntaxException("Unclosed character class", glob, len);
		}
		if (curlyOpen > 0) {
			throw new PatternSyntaxException("Unclosed group", glob, len);
		}
		this.compiled = Pattern.compile(regex.toString());
	}

	/**
	 * @return true if this is a wildcard pattern (with special chars)
	 */
	public boolean hasWildcard() {
		return hasWildcard;
	}


	// --- Static Methods ---

	/**
	 * Compile glob pattern string
	 *
	 * @param globPattern the glob pattern
	 * @return the pattern object
	 */
	public static Pattern compile(String globPattern) {
		return new Glob(globPattern).compiled();
	}

	/**
	 *
	 * @param glob
	 * @param s
	 * @return
	 */
	public static boolean matches(String glob, CharSequence s) {
		return new Glob(glob).matches(s);
	}

	/**
	 * Returns a {@link Glob} from a weak cache of known instances.
	 *
	 * @param pattern
	 * @return
	 */
	public static Glob from(String pattern) {
		return GLOBS.computeIfAbsent(pattern, k -> new Glob(pattern));
	}
}
