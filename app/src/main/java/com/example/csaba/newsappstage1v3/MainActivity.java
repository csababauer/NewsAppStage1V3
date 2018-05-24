package com.example.csaba.newsappstage1v3;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {

    private static final String requestUrl = "https://content.guardianapis.com/search?q=bitcoin&from-date=2014-01-01&show-tags=contributor&order-by=newest&api-key=1da394a8-c369-4807-bc2a-a49b797f8e62";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /** Kick off an  AsyncTask to perform the network request*/
        GuardianAsyncTask task = new GuardianAsyncTask();
        task.execute();
    }

    private class GuardianAsyncTask extends AsyncTask<URL, Void, ArrayList<Event>> {

        @Override
        protected ArrayList<Event> doInBackground(URL... urls) {
            // Create URL object
            URL url = createUrl(requestUrl);

            // Perform HTTP request to the URL and receive a JSON response back
            String jsonResponse = "";
            try {
                jsonResponse = makeHttpRequest(url);
            } catch (IOException e) {
            }

            // Extract relevant fields from the JSON response and create an {@link Event} object
            ArrayList<Event> news = extractNewsFromJson(jsonResponse);

            // Return the object as the result fo the {@link TsunamiAsyncTask}
            return news;
        }

        @Override
        protected void onPostExecute(ArrayList<Event> news) {
            if (news == null) {
                return;
            }
            UpdateUi(news);
        }
    }


    /**
     * create URL
     */
    private URL createUrl(String stringUrl) {
        URL url = null;
        try {
            url = new URL(stringUrl);
        } catch (MalformedURLException exception) {
            return null;
        }
        return url;
    }

    /**
     * make Http request
     */
    private String makeHttpRequest(URL url) throws IOException {
        String jsonResponse = "";
        // If the URL is null, then return early.
        if (url == null) {
            return jsonResponse;
        }
        HttpURLConnection urlConnection = null;
        InputStream inputStream = null;
        try {
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setReadTimeout(10000 /* milliseconds */);
            urlConnection.setConnectTimeout(15000 /* milliseconds */);
            urlConnection.connect();

            // If the request was successful (response code 200),
            // then read the input stream and parse the response.
            if (urlConnection.getResponseCode() == 200) {
                inputStream = urlConnection.getInputStream();
                jsonResponse = readFromStream(inputStream);
            } else {
                //Log.e(LOG_TAG, "Error response code: " + urlConnection.getResponseCode());
            }
        } catch (IOException e) {
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (inputStream != null) {
                // function must handle java.io.IOException here
                inputStream.close();
            }
        }
        return jsonResponse;
    }

    /**
     * read from stream
     */
    private String readFromStream(InputStream inputStream) throws IOException {
        StringBuilder output = new StringBuilder();
        if (inputStream != null) {
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));
            BufferedReader reader = new BufferedReader(inputStreamReader);
            String line = reader.readLine();
            while (line != null) {
                output.append(line);
                line = reader.readLine();
            }
        }
        return output.toString();
    }


    /**
     * the fetching - extractBookFromJson
     */
    private ArrayList<Event> extractNewsFromJson(String newsJson) {
        if (TextUtils.isEmpty(newsJson)) {
            return null;
        }

        ArrayList<Event> news = new ArrayList<>();

        try {
            // Create a JSONObject from the JSON response string
            JSONObject baseJsonResponse = new JSONObject(newsJson);

            JSONObject responseJSONObject = baseJsonResponse.getJSONObject("response");

            // Extract the JSONArray associated with the key called "results"
            JSONArray resultsArray = responseJSONObject.getJSONArray("results");

            int length = resultsArray.length();

            // If there are results in the features array
            if (resultsArray.length() > 0) {

                for (int i = 0; i < length; i++) {

                    String category = "";
                    String urlJsonLink = "";
                    String author = "";

                    // Get a single article at position i within the list of articles
                    JSONObject currentArticle = resultsArray.getJSONObject(i);

                    // Extract out the title, time, and values
                    String webTitle = currentArticle.getString("webTitle");
                    String webUrl = currentArticle.getString("webUrl");
                    String sectionName = currentArticle.getString("sectionName");
                    String date = currentArticle.getString("webPublicationDate");
                    /**split date string to data and time. I am using date only*/
                    String[] parts = date.split("T");
                    /**new date is the part before the "T" */
                    date = parts[0];

                    /**get tags array and author object*/
                    if (currentArticle.has("tags")) {
                        JSONArray tagsArray = currentArticle.getJSONArray("tags");
                        if (tagsArray.length() > 0) {
                            JSONObject newsTag = tagsArray.getJSONObject(0);
                            if (newsTag.has("webTitle")) {
                                author = newsTag.getString("webTitle");
                            }
                        }
                    }

                    news.add(new Event(webTitle, webUrl, sectionName, date, author));

                }
            }
        } catch (JSONException e) {
            Log.e("QueryUtils", "Problem parsing the earthquake JSON results", e);

        }

        return news;
    }


    /**
     * update UI
     */
    private void UpdateUi(ArrayList<Event> news) {

        ListView newsListView = (ListView) findViewById(R.id.list);
        final EventAdapter adapter = new EventAdapter(this, news);

        /**check connectivity*/
        // Get a reference to the ConnectivityManager to check state of network connectivity
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        // Get details on the currently active default data network
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        /** If there is no result, send a message*/
        if (news.isEmpty() || networkInfo == null) {

            // First, hide loading indicator so error message will be visible
            View loadingIndicator = findViewById(R.id.loading_indicator);
            loadingIndicator.setVisibility(View.GONE);
            TextView mEmptyStateTextView = (TextView) findViewById(R.id.empty_view);
            mEmptyStateTextView.setVisibility(View.VISIBLE);
            mEmptyStateTextView.setText(getString(R.string.no_internet_connection));

        } else {
            // First, hide loading indicator
            View loadingIndicator = findViewById(R.id.loading_indicator);
            loadingIndicator.setVisibility(View.GONE);

            newsListView.setAdapter(adapter);
        }


        /**make list item clickable*/
        newsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                // Find the current news that was clicked on
                Event currentArticle = adapter.getItem(position);

                // Convert the String URL into a URI object (to pass into the Intent constructor)
                Uri articleUri = Uri.parse(currentArticle.getUrl());

                // Create a new intent to view
                Intent websiteIntent = new Intent(Intent.ACTION_VIEW, articleUri);

                // Send the intent to launch a new activity
                startActivity(websiteIntent);
            }
        });
    }

    /**
     * hide loading indicator show empty textview
     */
    private void NoConnection() {
        // First, hide loading indicator so error message will be visible
        View loadingIndicator = findViewById(R.id.loading_indicator);
        loadingIndicator.setVisibility(View.GONE);

        TextView mEmptyStateTextView = (TextView) findViewById(R.id.empty_view);
        mEmptyStateTextView.setText(R.string.no_internet_connection);
    }
}








