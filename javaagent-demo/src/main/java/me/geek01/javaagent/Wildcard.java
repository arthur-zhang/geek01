package me.geek01.javaagent;


/**
 * Checks whether a string or path matches a given wildcard pattern.
 * Possible patterns allow to match single characters ('?') or any count of
 * characters ('*'). Wildcard characters can be escaped (by an '\').
 * When matching path, deep tree wildcard also can be used ('**').
 * <p>
 * This method uses recursive matching, as in linux or windows. regexp works the same.
 * This method is very fast, comparing to similar implementations.
 */
public class Wildcard {

	/**
	 * Checks whether a string matches a given wildcard pattern.
	 *
	 * @param string  input string
	 * @param pattern pattern to match
	 * @return <code>true</code> if string matches the pattern, otherwise <code>false</code>
	 */
	public static boolean match(CharSequence string, CharSequence pattern) {
		return match(string, pattern, 0, 0);
	}

	/**
	 * Checks if two strings are equals or if they {@link #match(CharSequence, CharSequence)}.
	 * Useful for cases when matching a lot of equal strings and speed is important.
	 */
	public static boolean equalsOrMatch(CharSequence string, CharSequence pattern) {
		if (string.equals(pattern)) {
			return true;
		}
		return match(string, pattern, 0, 0);
	}

	/**
	 * Internal matching recursive function.
	 */
	private static boolean match(CharSequence string, CharSequence pattern, int sNdx, int pNdx) {
		int pLen = pattern.length();
		if (pLen == 1) {
			if (pattern.charAt(0) == '*') {     // speed-up
				return true;
			}
		}
		int sLen = string.length();
		boolean nextIsNotWildcard = false;

		while (true) {

			// check if end of string and/or pattern occurred
			if ((sNdx >= sLen)) {        // end of string still may have pending '*' in pattern
				while ((pNdx < pLen) && (pattern.charAt(pNdx) == '*')) {
					pNdx++;
				}
				return pNdx >= pLen;
			}
			if (pNdx >= pLen) {                    // end of pattern, but not end of the string
				return false;
			}
			char p = pattern.charAt(pNdx);        // pattern char

			// perform logic
			if (!nextIsNotWildcard) {

				if (p == '\\') {
					pNdx++;
					nextIsNotWildcard = true;
					continue;
				}
				if (p == '?') {
					sNdx++;
					pNdx++;
					continue;
				}
				if (p == '*') {
					char pNext = 0;                        // next pattern char
					if (pNdx + 1 < pLen) {
						pNext = pattern.charAt(pNdx + 1);
					}
					if (pNext == '*') {                    // double '*' have the same effect as one '*'
						pNdx++;
						continue;
					}
					int i;
					pNdx++;

					// find recursively if there is any substring from the end of the
					// line that matches the rest of the pattern !!!
					for (i = string.length(); i >= sNdx; i--) {
						if (match(string, pattern, i, pNdx)) {
							return true;
						}
					}
					return false;
				}
			} else {
				nextIsNotWildcard = false;
			}

			// check if pattern char and string char are equals
			if (p != string.charAt(sNdx)) {
				return false;
			}

			// everything matches for now, continue
			sNdx++;
			pNdx++;
		}
	}


	// ---------------------------------------------------------------- utilities

	/**
	 * Matches string to at least one pattern.
	 * Returns index of matched pattern, or <code>-1</code> otherwise.
	 *
	 * @see #match(CharSequence, CharSequence)
	 */
	public static int matchOne(String src, String[] patterns) {
		for (int i = 0; i < patterns.length; i++) {
			if (match(src, patterns[i])) {
				return i;
			}
		}
		return -1;
	}
}
