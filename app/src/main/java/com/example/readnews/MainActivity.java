package com.example.readnews;

import androidx.appcompat.app.AppCompatActivity;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ArrayList<String> titles = new ArrayList<>();
    ArrayList<String> content = new ArrayList<>();

    ArrayAdapter arrayAdapter;

    SQLiteDatabase newsDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        newsDB = this.openOrCreateDatabase("News", MODE_PRIVATE, null);

        newsDB.execSQL("CREATE TABLE IF NOT EXISTS news (id INTEGER PRIMARY KEY, newsID, INTEGER, title VARCHAR, content VARCHAR)");

        updateListView();

        DownloadData dlData = new DownloadData();
        try {

            dlData.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");

        } catch (Exception e) {

        }

        ListView listView = findViewById(R.id.listView);
        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, titles);
        listView.setAdapter(arrayAdapter);
    }

    public void updateListView() {
        Cursor cursor = newsDB.rawQuery("SELECT * FROM news", null);
        int contentIndex = cursor.getColumnIndex("content");
        int titleIndex = cursor.getColumnIndex("title");

        if (cursor.moveToFirst()) {
            titles.clear();
            content.clear();

            do {

                titles.add(cursor.getString(titleIndex));
                content.add(cursor.getString(contentIndex));

            } while (cursor.moveToFirst());

            arrayAdapter.notifyDataSetChanged();
        }
    }

    public class DownloadData extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... urls) {

            String result = "";
            URL url;
            HttpURLConnection urlConnect = null;

            try {

                url = new URL(urls[0]);

                urlConnect = (HttpURLConnection) url.openConnection();

                InputStream inputStream = urlConnect.getInputStream();

                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);

                int data = inputStreamReader.read();

                while (data != -1) {

                    char current = (char) data;
                    result += current;
                    data = inputStreamReader.read();
                }

                JSONArray jsonArray = new JSONArray(result);

                int numberOfNews = 20;

                if (jsonArray.length() < 20) {
                    numberOfNews = jsonArray.length();
                }

                newsDB.execSQL("DELETE FROM news");

                for (int i=0; i<numberOfNews; i++) {
                    String articleId = jsonArray.getString(i);
                    url = new URL("https://hacker-news.firebaseio.com/v0/item/" + articleId + ".json?print=pretty");
                    urlConnect = (HttpURLConnection) url.openConnection();

                    inputStream = urlConnect.getInputStream();
                    inputStreamReader = new InputStreamReader(inputStream);

                    data = inputStreamReader.read();

                    String articleInfo = "";

                    while (data != -1) {

                        char current = (char) data;
                        articleInfo += current;
                        data = inputStreamReader.read();
                    }

                    JSONObject jsonObject = new JSONObject(articleInfo);

                    if (!jsonObject.isNull("title") && !jsonObject.isNull("url")) {
                        String articleTitle = jsonObject.getString("title");
                        String articleUrl = jsonObject.getString("url");

                        url = new URL(articleUrl);
                        urlConnect = (HttpURLConnection) url.openConnection();
                        inputStream = urlConnect.getInputStream();
                        inputStreamReader = new InputStreamReader(inputStream);
                        data = inputStreamReader.read();
                        String articleContent = "";
                        while (data != -1) {
                            char current = (char) data;
                            articleContent += current;
                            data = inputStreamReader.read();
                        }

                        Log.i("HTML", articleContent);

                        String sql = "INSERT INTO news (newsId, title, content) VALUES (?, ?, ?)";
                        SQLiteStatement statement = newsDB.compileStatement(sql);
                        statement.bindString(1, articleId);
                        statement.bindString(2, articleTitle);
                        statement.bindString(3, articleContent);
                        statement.execute();

                    }
                }

                Log.i("URL content", result);
                return result;

            }catch (Exception e) {

                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            updateListView();
        }
    }

}