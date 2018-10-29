package org.mozilla.vrbrowser.search.suggestions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import mozilla.components.browser.search.SearchEngine;

public class SuggestionParser {

    private static final String AZERDICT = "Azerdict";
    private static final String DAUM = "다음지도";
    private static final String QWANT = "Qwant";

    public static BiFunction<JSONArray, Integer, List<String>> selectArrayResponseParser(SearchEngine mEngine) {
        return defaultSuggestionParser;
    }

    public static Function<JSONObject, List<String>> selectObjectResponseParser(SearchEngine mEngine) {
        if (mEngine.getName().equals(AZERDICT)) {
            // TODO: Wait until A-C is fixed

        } else if (mEngine.getName().equals(DAUM)) {
            // TODO: Wait until A-C is fixed

        } else if (mEngine.getName().equals(QWANT)) {
            // TODO: Wait until A-C is fixed
        }

        return null;
    }

    private static BiFunction<JSONArray, Integer, List<String>> defaultSuggestionParser = (jsonArray, resultIndex) -> {
        List<String> list = new ArrayList<>();
        try {
            JSONArray array = jsonArray.getJSONArray(resultIndex);
            if (array != null) {
                int len = array.length();
                for (int i=0; i<len; i++){
                    list.add(array.get(i).toString());
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return list;
    };

}
