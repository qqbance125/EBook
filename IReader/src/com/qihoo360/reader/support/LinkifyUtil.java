
package com.qihoo360.reader.support;

import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.text.util.Linkify;
import android.text.util.Linkify.MatchFilter;
import android.text.util.Linkify.TransformFilter;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LinkifyUtil {
    public enum Type {
        phone, web, email
    }

    public static final String GOOD_IRI_CHAR =
            "a-zA-Z0-9\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF";
    public static final String TOP_LEVEL_DOMAIN_STR_FOR_WEB_URL =
            "(?:"
                    + "(?:aero|arpa|asia|a[cdefgilmnoqrstuwxz])"
                    + "|(?:biz|b[abdefghijmnorstvwyz])"
                    + "|(?:cat|com|coop|c[acdfghiklmnoruvxyz])"
                    + "|d[ejkmoz]"
                    + "|(?:edu|e[cegrstu])"
                    + "|f[ijkmor]"
                    + "|(?:gov|g[abdefghilmnpqrstuwy])"
                    + "|h[kmnrtu]"
                    + "|(?:info|int|i[delmnoqrst])"
                    + "|(?:jobs|j[emop])"
                    + "|k[eghimnprwyz]"
                    + "|l[abcikrstuvy]"
                    + "|(?:mil|mobi|museum|m[acdeghklmnopqrstuvwxyz])"
                    + "|(?:name|net|n[acefgilopruz])"
                    + "|(?:org|om)"
                    + "|(?:pro|p[aefghklmnrstwy])"
                    + "|qa"
                    + "|r[eosuw]"
                    + "|s[abcdeghijklmnortuvyz]"
                    + "|(?:tel|travel|t[cdfghjklmnoprtvwz])"
                    + "|u[agksyz]"
                    + "|v[aceginu]"
                    + "|w[fs]"
                    + "|(?:xn\\-\\-0zwm56d|xn\\-\\-11b5bs3a9aj6g|xn\\-\\-80akhbyknj4f|xn\\-\\-9t4b11yi5a|xn\\-\\-deba0ad|xn\\-\\-g6w251d|xn\\-\\-hgbk6aj7f53bba|xn\\-\\-hlcj6aya9esc7a|xn\\-\\-jxalpdlp|xn\\-\\-kgbechtv|xn\\-\\-zckzah)"
                    + "|y[etu]"
                    + "|z[amw]))";
    public static final Pattern WEB_URL = Pattern
            .compile(
                    "((?:(http|https|Http|Https|rtsp|Rtsp):\\/\\/(?:(?:[a-zA-Z0-9\\$\\-\\_\\.\\+\\!\\*\\'\\(\\)"
                            + "\\,\\;\\?\\&\\=]|(?:\\%[a-fA-F0-9]{2})){1,64}(?:\\:(?:[a-zA-Z0-9\\$\\-\\_"
                            + "\\.\\+\\!\\*\\'\\(\\)\\,\\;\\?\\&\\=]|(?:\\%[a-fA-F0-9]{2})){1,25})?\\@)?)?"
                            + "((?:(?:[" + GOOD_IRI_CHAR + "][" + GOOD_IRI_CHAR + "\\-]{0,64}\\.)+" // named
                                                                                                    // host
                            + TOP_LEVEL_DOMAIN_STR_FOR_WEB_URL
                            + "|(?:(?:25[0-5]|2[0-4]" // or ip address
                            + "[0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(?:25[0-5]|2[0-4][0-9]"
                            + "|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(?:25[0-5]|2[0-4][0-9]|[0-1]"
                            + "[0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}"
                            + "|[1-9][0-9]|[0-9])))"
                            + "(?:\\:\\d{1,5})?)" // plus option port number
                            + "(\\/(?:(?:[" + GOOD_IRI_CHAR + "\\;\\/\\?\\:\\@\\&\\=\\#\\~" // plus
                                                                                            // option
                                                                                            // query
                                                                                            // params
                            + "\\-\\.\\+\\!\\*\\'\\(\\)\\,\\_])|(?:\\%[a-fA-F0-9]{2}))*)?"
                            + "(?:\\b|$)"); // and finally, a word boundary or
                                            // end of
                                            // input. This is to stop foo.sure
                                            // from
                                            // matching as foo.su

    public static final Pattern PHONE = Pattern.compile( // sdd = space, dot, or
                                                         // dash
            "(\\+[0-9]+[\\- \\.]*)?" // +<digits><sdd>*
                    + "(\\([0-9]+\\)[\\- \\.]*)?" // (<digits>)<sdd>*
                    + "([0-9][0-9\\- \\.][0-9\\- \\.]+[0-9])"); // <digit><digit|sdd>+<digit>

    public static final Pattern EMAIL_ADDRESS = Pattern.compile(
            "[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" +
                    "\\@" +
                    "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                    "(" +
                        "\\." +
                        "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
                    ")+"

            );

    private static String[] webScheme = {
            "http://", "https://", "rtsp://"
    };

    public static CharSequence addIntentLink(OnLinkClickListener listener, CharSequence source,
            final String text, final int start, final int end, final Type type) {
        if (source instanceof Spanned) {
            IntentSpan[] spans = ((Spanned) source).getSpans(start, end, IntentSpan.class);
            if (spans.length > 0) {
                return source;
            }
        }

        SpannableString spannableString = new SpannableString(source);
        spannableString.setSpan(new IntentSpan(listener, text, type), start, end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spannableString;
    }
    
    /** 
     * 收集相关的链接
     * @param source
     * @param m
     * @param l
     * @param filter
     * @param type
     */
    private static void gatherLinks(CharSequence source, Matcher m, ArrayList<LinkSpec> l,
            MatchFilter filter, Type type) {
        while (m.find()) {
            if ((null == filter) || (filter.acceptMatch(source, m.start(), m.end()))) {
                l.add(new LinkSpec(m.group(), m.start(), m.end(), type));
            }
        }
    }

    public static CharSequence matchLinks(OnLinkClickListener listener, CharSequence source) {
        ArrayList<LinkSpec> l = new ArrayList<LinkSpec>();

        //gatherLinks(source, PHONE.matcher(source), l, Linkify.sPhoneNumberMatchFilter, Type.phone);
        gatherLinks(source, WEB_URL.matcher(source), l, Linkify.sUrlMatchFilter, Type.web);

        gatherLinks(source, EMAIL_ADDRESS.matcher(source), l, null, Type.email);

        pruneOverlaps(l);
        for (LinkSpec i : l) {
            source = LinkifyUtil.addIntentLink(listener, source, i.text, i.start, i.end, i.type);
        }
        return source;
    }
    
    /**
     * 删除重叠的
     * @param links
     */
    private static final void pruneOverlaps(ArrayList<LinkSpec> links) {
        Comparator<LinkSpec> c = new Comparator<LinkSpec>() {
            public final int compare(LinkSpec a, LinkSpec b) {
                if (a.start < b.start) {
                    return -1;
                }

                if (a.start > b.start) {
                    return 1;
                }

                if (a.end < b.end) {
                    return 1;
                }

                if (a.end > b.end) {
                    return -1;
                }

                return 0;
            }

            public final boolean equals(Object o) {
                return false;
            }
        };

        Collections.sort(links, c);

        int len = links.size();
        int i = 0;

        while (i < len - 1) {
            LinkSpec a = links.get(i);
            LinkSpec b = links.get(i + 1);
            int remove = -1;

            if ((a.start <= b.start) && (a.end > b.start)) {
                if (b.end <= a.end) {
                    remove = i + 1;
                } else if ((a.end - a.start) > (b.end - b.start)) {
                    remove = i + 1;
                } else if ((a.end - a.start) < (b.end - b.start)) {
                    remove = i;
                }

                if (remove != -1) {
                    links.remove(remove);
                    len--;
                    continue;
                }
            }
            i++;
        }
    }

    public interface OnLinkClickListener {
        void OnLinkClick(String text, LinkifyUtil.Type type);
    }

    public static final String makeUrl(String url) {
        return makeUrl(url, webScheme, null, null);
    }

    /**
     * 生成url
     * @param url
     * @param prefixes  前缀
     * @param m	
     * @param filter
     * @return
     */
    public static final String makeUrl(String url, String[] prefixes,
            Matcher m, TransformFilter filter) {
        if (filter != null) {
            url = filter.transformUrl(m, url);
        }

        // 是否有前缀
        boolean hasPrefix = false;

        for (int i = 0; i < prefixes.length; i++) {
            if (url.regionMatches(true, 0, prefixes[i], 0,
                                  prefixes[i].length())) {
                hasPrefix = true;

                // Fix capitalization if necessary
                if (!url.regionMatches(false, 0, prefixes[i], 0,
                                       prefixes[i].length())) {
                    url = prefixes[i] + url.substring(prefixes[i].length());
                }

                break;
            }
        }

        if (!hasPrefix) {
            url = prefixes[0] + url;
        }

        return url;
    }

}
/**
 *	 文本的链接地址
 */
class IntentSpan extends ClickableSpan {

    LinkifyUtil.Type type;
    String text;

    @Override
    public void onClick(View view) {
        this.onLinkClickListener.OnLinkClick(this.text, this.type);
    }

    public IntentSpan(LinkifyUtil.OnLinkClickListener listener, final String text,
            final LinkifyUtil.Type type) {
        this.type = type;
        this.text = text;
        this.onLinkClickListener = listener;
    }

    private LinkifyUtil.OnLinkClickListener onLinkClickListener = null;
}

class LinkSpec {
    String text;
    int start;
    int end;
    LinkifyUtil.Type type;

    public LinkSpec(String text, int start, int end, LinkifyUtil.Type type) {
        this.text = text;
        this.start = start;
        this.end = end;
        this.type = type;
    }
}
