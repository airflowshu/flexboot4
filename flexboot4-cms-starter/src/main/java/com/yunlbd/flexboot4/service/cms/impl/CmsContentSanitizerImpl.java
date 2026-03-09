package com.yunlbd.flexboot4.service.cms.impl;

import com.yunlbd.flexboot4.entity.cms.CmsArticle;
import com.yunlbd.flexboot4.service.cms.CmsContentSanitizer;
import com.yunlbd.flexboot4.util.LogDesensitizationUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class CmsContentSanitizerImpl implements CmsContentSanitizer {

    private static final Pattern PHONE_PATTERN = Pattern.compile("(?<!\\d)1\\d{10}(?!\\d)");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("(?<![0-9A-Za-z])[1-9]\\d{5}(?:19|20)\\d{2}(?:0[1-9]|1[0-2])(?:0[1-9]|[12]\\d|3[01])\\d{3}[0-9Xx](?![0-9A-Za-z])");

    private static final Safelist CMS_HTML_SAFELIST = Safelist.relaxed()
            .addTags("section", "article", "aside", "header", "footer", "figure", "figcaption")
            .addAttributes(":all", "class", "style")
            .addAttributes("img", "src", "alt", "title", "width", "height")
            .addProtocols("img", "src", "http", "https", "data");

    @Override
    public void sanitizeForPersistence(CmsArticle article) {
        if (article == null) {
            return;
        }
        if (article.getTitle() != null) {
            article.setTitle(desensitizeText(Jsoup.clean(article.getTitle(), Safelist.none())));
        }
        if (article.getSummary() != null) {
            article.setSummary(desensitizeText(Jsoup.clean(article.getSummary(), Safelist.none())));
        }
        if (article.getAuthor() != null) {
            article.setAuthor(desensitizeText(Jsoup.clean(article.getAuthor(), Safelist.none())));
        }
        if (article.getContent() != null) {
            article.setContent(sanitizeAndDesensitizeHtml(article.getContent()));
        }
    }

    private String sanitizeAndDesensitizeHtml(String html) {
        String cleanHtml = Jsoup.clean(html, CMS_HTML_SAFELIST);
        Document document = Jsoup.parseBodyFragment(cleanHtml);
        for (Element element : document.getAllElements()) {
            for (TextNode textNode : element.textNodes()) {
                textNode.text(desensitizeText(textNode.text()));
            }
        }
        return document.body().html();
    }

    private String desensitizeText(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }

        String result = replaceWithMask(text, PHONE_PATTERN, LogDesensitizationUtils::maskPhone);
        result = replaceWithMask(result, EMAIL_PATTERN, LogDesensitizationUtils::maskEmail);
        result = replaceWithMask(result, ID_CARD_PATTERN, LogDesensitizationUtils::maskIdCard);
        return result;
    }

    private String replaceWithMask(String source, Pattern pattern, Masker masker) {
        Matcher matcher = pattern.matcher(source);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String match = matcher.group();
            String masked = masker.mask(match);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(masked));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    @FunctionalInterface
    private interface Masker {
        String mask(String raw);
    }
}

