package com.ruby.ruby.integration;

/**
 * @param title    e.g. "A. P. J. Abdul Kalam"
 * @param summary  the article's opening paragraph
 * @param imageUrl thumbnail, or null
 * @param pageUrl  link to the full article
 */
public record Article(String title, String summary, String imageUrl, String pageUrl) {
}
