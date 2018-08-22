/*
 * Copyright (C) 2013-2014 Dominik Schürmann <dominik@dominikschuermann.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sufficientlysecure.htmltextview;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RawRes;
import android.text.Editable;
import android.text.Html;
import android.text.Spannable;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.AttributeSet;

import java.io.InputStream;
import java.util.Scanner;

import co.zipperstudios.supporthtml.SupportHtml;

public class HtmlEditText extends JellyBeanSpanFixEditText {

    public static final String TAG = "HtmlTextView";
    public static final boolean DEBUG = false;

    @Nullable
    private ClickableTableSpan clickableTableSpan;
    @Nullable
    private DrawTableLinkSpan drawTableLinkSpan;
    private float indent = 24.0f; // Default to 24px.

    private boolean removeTrailingWhiteSpace = true;

    public HtmlEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public HtmlEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public HtmlEditText(Context context) {
        super(context);
    }

    /**
     * @see org.sufficientlysecure.htmltextview.HtmlEditText#setHtml(int)
     */
    public void setHtml(@RawRes int resId) {
        setHtml(resId, null);
    }

    /**
     * @see org.sufficientlysecure.htmltextview.HtmlEditText#setHtml(String)
     */
    public void setHtml(@NonNull String html) {
        setHtml(html, null);
    }

    /**
     * Loads HTML from a raw resource, i.e., a HTML file in res/raw/.
     * This allows translatable resource (e.g., res/raw-de/ for german).
     * The containing HTML is parsed to Android's Spannable format and then displayed.
     *
     * @param resId       for example: R.raw.help
     * @param imageGetter for fetching images. Possible ImageGetter provided by this library:
     *                    HtmlLocalImageGetter and HtmlRemoteImageGetter
     */
    public void setHtml(@RawRes int resId, @Nullable Html.ImageGetter imageGetter) {
        InputStream inputStreamText = getContext().getResources().openRawResource(resId);

        setHtml(convertStreamToString(inputStreamText), imageGetter);
    }

    /**
     * Parses String containing HTML to Android's Spannable format and displays it in this TextView.
     * Using the implementation of Html.ImageGetter provided.
     *
     * @param html        String containing HTML, for example: "<b>Hello world!</b>"
     * @param imageGetter for fetching images. Possible ImageGetter provided by this library:
     *                    HtmlLocalImageGetter and HtmlRemoteImageGetter
     */
    public void setHtml(@NonNull String html, @Nullable Html.ImageGetter imageGetter) {
        final HtmlTagHandler htmlTagHandler = new HtmlTagHandler(getPaint());
        htmlTagHandler.setClickableTableSpan(clickableTableSpan);
        htmlTagHandler.setDrawTableLinkSpan(drawTableLinkSpan);
        htmlTagHandler.setListIndentPx(indent);

        html = htmlTagHandler.overrideTags(html);

        Spanned spannedHtml;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            spannedHtml = Html.fromHtml(html, Html.FROM_HTML_SEPARATOR_LINE_BREAK_PARAGRAPH, imageGetter, htmlTagHandler);
        } else {
            spannedHtml = SupportHtml.fromHtml(html, SupportHtml.FROM_HTML_SEPARATOR_LINE_BREAK_PARAGRAPH, imageGetter, htmlTagHandler);
        }

        if (removeTrailingWhiteSpace) {
            setText(removeHtmlBottomPadding(spannedHtml));
        } else {
            setText(spannedHtml);
        }

        // make links work
        setMovementMethod(LocalLinkMovementMethod.getInstance());

        setStyleAndUnderlineSpansExclusiveInclusive();
    }

    private void setStyleAndUnderlineSpansExclusiveInclusive() {
        Editable editable = getText();
        StyleSpan[] styleSpans = editable.getSpans(0, editable.length(), StyleSpan.class);
        for (StyleSpan characterStyleSpan : styleSpans) {
            int start = editable.getSpanStart(characterStyleSpan);
            int end = editable.getSpanEnd(characterStyleSpan);

            editable.removeSpan(characterStyleSpan);
            editable.setSpan(new StyleSpan(characterStyleSpan.getStyle()), start, end,
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        }

        UnderlineSpan[] underlineSpans = editable.getSpans(0, editable.length(), UnderlineSpan.class);
        for (UnderlineSpan underlineSpan : underlineSpans) {
            int start = editable.getSpanStart(underlineSpan);
            int end = editable.getSpanEnd(underlineSpan);

            editable.removeSpan(underlineSpan);
            editable.setSpan(new UnderlineSpan(), start, end,
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        }
    }

    /**
     * The Html.fromHtml method has the behavior of adding extra whitespace at the bottom
     * of the parsed HTML displayed in for example a TextView. In order to remove this
     * whitespace call this method before setting the text with setHtml on this TextView.
     *
     * @param removeTrailingWhiteSpace true if the whitespace rendered at the bottom of a TextView
     *                                 after setting HTML should be removed.
     */
    public void setRemoveTrailingWhiteSpace(boolean removeTrailingWhiteSpace) {
        this.removeTrailingWhiteSpace = removeTrailingWhiteSpace;
    }

    /**
     * The Html.fromHtml method has the behavior of adding extra whitespace at the bottom
     * of the parsed HTML displayed in for example a TextView. In order to remove this
     * whitespace call this method before setting the text with setHtml on this TextView.
     *
     * This method is deprecated, use setRemoveTrailingWhiteSpace instead.
     *
     * @param removeFromHtmlSpace true if the whitespace rendered at the bottom of a TextView
     *                            after setting HTML should be removed.
     */
    @Deprecated()
    public void setRemoveFromHtmlSpace(boolean removeFromHtmlSpace) {
        this.removeTrailingWhiteSpace = removeFromHtmlSpace;
    }

    public void setClickableTableSpan(@Nullable ClickableTableSpan clickableTableSpan) {
        this.clickableTableSpan = clickableTableSpan;
    }

    public void setDrawTableLinkSpan(@Nullable DrawTableLinkSpan drawTableLinkSpan) {
        this.drawTableLinkSpan = drawTableLinkSpan;
    }

    /**
     * Add ability to increase list item spacing. Useful for configuring spacing based on device
     * screen size. This applies to ordered and unordered lists.
     *
     * @param px    pixels to indent.
     */
    public void setListIndentPx(float px) {
        this.indent = px;
    }

    /**
     * http://stackoverflow.com/questions/309424/read-convert-an-inputstream-to-a-string
     */
    @NonNull
    static private String convertStreamToString(@NonNull InputStream is) {
        Scanner s = new Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    /**
     * Html.fromHtml sometimes adds extra space at the bottom.
     * This methods removes this space again.
     * See https://github.com/SufficientlySecure/html-textview/issues/19
     */
    @Nullable
    static private CharSequence removeHtmlBottomPadding(@Nullable CharSequence text) {
        if (text == null) {
            return null;
        }

        while (text.length() > 0 && text.charAt(text.length() - 1) == '\n') {
            text = text.subSequence(0, text.length() - 1);
        }
        return text;
    }
}
