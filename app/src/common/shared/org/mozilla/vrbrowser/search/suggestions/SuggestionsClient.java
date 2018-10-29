package org.mozilla.vrbrowser.search.suggestions;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.function.Function;

import cz.msebera.android.httpclient.Header;
import mozilla.components.browser.search.SearchEngine;

public class SuggestionsClient {

    private static AsyncHttpClient client = new AsyncHttpClient();

    public static void getSuggestions(SearchEngine mEngine, String aQuery, Function<List<String>, Void> callback) {
        client.cancelAllRequests(true);
        client.get(aQuery, null, new JsonHttpResponseHandler("ISO-8859-1") {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                callback.apply(SuggestionParser.selectObjectResponseParser(mEngine).apply(response));
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                callback.apply(SuggestionParser.selectArrayResponseParser(mEngine).apply(response, 1));
            }
        });
    }
}
