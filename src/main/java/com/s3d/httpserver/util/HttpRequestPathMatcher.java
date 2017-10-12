package com.s3d.httpserver.util;

import io.netty.handler.codec.http.HttpMethod;
import com.s3d.httpserver.request.ServerRequest;

public final class HttpRequestPathMatcher {
    private static final String MATCH_ALL = "/**";

    private final Matcher matcher;
    private final String pattern;
    private final HttpMethod httpMethod;

    /**
     * Creates a matcher with the specific pattern which will match all HTTP methods.
     *
     * @param pattern the ant pattern to use for matching
     */
    public HttpRequestPathMatcher(String pattern) {
        this(pattern, null);
    }

    /**
     * Creates a matcher with the supplied pattern which will match all HTTP methods.
     *
     * @param pattern the ant pattern to use for matching
     * @param httpMethod the HTTP method. The {@code matches} method will return false if the incoming request doesn't
     * have the same method.
     */
    public HttpRequestPathMatcher(String pattern, String httpMethod) {
        Assert.hasText(pattern, "Pattern cannot be null or empty");

        if (pattern.equals(MATCH_ALL) || pattern.equals("**")) {
            pattern = MATCH_ALL;
            matcher = null;
        } else {
            pattern = pattern.toLowerCase();

            // If the pattern ends with {@code /**} and has no other wildcards, then optimize to a sub-path match
            if (pattern.endsWith(MATCH_ALL) && pattern.indexOf('?') == -1 &&
                    pattern.indexOf("*") == pattern.length() - 2) {
                matcher = new SubpathMatcher(pattern.substring(0, pattern.length() - 3));
            } else {
                matcher = new SpringAntMatcher(pattern);
            }
        }

        this.pattern = pattern;
        this.httpMethod = StringUtils.hasText(httpMethod) ? HttpMethod.valueOf(httpMethod) : null;
    }
    
    public boolean matches(String url) {
    	return matcher.matches(url);
    }
    /**
     * Returns true if the configured pattern (and HTTP-Method) match those of the supplied request.
     *
     * @param request the request to match against. The ant pattern will be matched against the
     *    {@code servletPath} + {@code pathInfo} of the request.
     */
    public boolean matches(ServerRequest request) {
        if (httpMethod != null && request.getMethod() != null && httpMethod != request.getMethod()) {
            return false;
        }

        if (pattern.equals(MATCH_ALL)) {
            return true;
        }
        String url = getRequestPath(request);
        return matcher.matches(url);
    }

    private String getRequestPath(ServerRequest request) {
        //String url = request.getServletPath();
    	String url = request.getHandlerUri();
        if (request.getPathInfo() != null) {
            url += request.getPathInfo();
        }

        url = url.toLowerCase();

        return url;
    }

    public String getPattern() {
        return pattern;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof HttpRequestPathMatcher)) {
            return false;
        }
        HttpRequestPathMatcher other = (HttpRequestPathMatcher)obj;
        return this.pattern.equals(other.pattern) &&
            this.httpMethod == other.httpMethod;
    }

    @Override
    public int hashCode() {
        int code = 31 ^ pattern.hashCode();
        if (httpMethod != null) {
            code ^= httpMethod.hashCode();
        }
        return code;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Ant [pattern='").append(pattern).append("'");

        if (httpMethod != null) {
            sb.append(", ").append(httpMethod);
        }

        sb.append("]");

        return sb.toString();
    }

    private static interface Matcher {
        boolean matches(String path);
    }

    private static class SpringAntMatcher implements Matcher {
        private static final AntPathMatcher antMatcher = new AntPathMatcher();

        private final String pattern;

        private SpringAntMatcher(String pattern) {
            this.pattern = pattern;
        }

        public boolean matches(String path) {
            return antMatcher.match(pattern, path);
        }
    }

    /**
     * Optimized matcher for trailing wildcards
     */
    private static class SubpathMatcher implements Matcher {
        private final String subpath;
        private final int length;

        private SubpathMatcher(String subpath) {
            assert !subpath.contains("*");
            this.subpath = subpath;
            this.length = subpath.length();
        }

        public boolean matches(String path) {
            return path.startsWith(subpath) && (path.length() == length || path.charAt(length) == '/');
        }
    }
}
