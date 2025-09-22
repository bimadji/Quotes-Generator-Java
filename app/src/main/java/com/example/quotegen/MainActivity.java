package com.example.quotegen;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    Button btnGetQuote, btnSaveFav, btnShowFav;
    TextView tvQuote, tvAuthor;
    ListView listViewFav;

    OkHttpClient client = new OkHttpClient();
    SharedPreferences prefs;
    ArrayList<String> favList = new ArrayList<>();
    JSONObject currentQuote = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnGetQuote = findViewById(R.id.btnGetQuote);
        btnSaveFav = findViewById(R.id.btnSaveFav);
        btnShowFav = findViewById(R.id.btnShowFav);
        tvQuote = findViewById(R.id.tvQuote);
        tvAuthor = findViewById(R.id.tvAuthor);
        listViewFav = findViewById(R.id.listViewFav);

        prefs = getSharedPreferences("quotes_prefs", MODE_PRIVATE);
        loadFavorites();

        btnGetQuote.setOnClickListener(v -> getQuote());

        btnSaveFav.setOnClickListener(v -> {
            if (currentQuote != null) {
                favList.add(currentQuote.toString());
                saveFavorites();
                Toast.makeText(this, "Quote ditambahkan ke Favorite", Toast.LENGTH_SHORT).show();
            }
        });

        btnShowFav.setOnClickListener(v -> showFavorites());
    }

    private void getQuote() {
        String url = "http://api.quotable.io/random";

        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(MainActivity.this, "Gagal Koneksi!", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();

                    try {
                        currentQuote = new JSONObject(responseData);
                        String content = currentQuote.getString("content");
                        String author = currentQuote.getString("author");

                        runOnUiThread(() -> {
                            tvQuote.setText("\"" + content + "\"");
                            tvAuthor.setText("- " + author);

                            tvQuote.setVisibility(View.VISIBLE);
                            tvAuthor.setVisibility(View.VISIBLE);
                            listViewFav.setVisibility(View.GONE);
                        });

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void saveFavorites() {
        JSONArray arr = new JSONArray();
        for (String s : favList) {
            arr.put(s);
        }
        prefs.edit().putString("favorites", arr.toString()).apply();
    }

    private void loadFavorites() {
        favList.clear();
        String json = prefs.getString("favorites", "[]");
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                favList.add(arr.getString(i));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void showFavorites() {
        loadFavorites();
        ArrayList<JSONObject> data = new ArrayList<>();

        try {
            for (String s : favList) {
                JSONObject obj = new JSONObject(s);
                data.add(obj);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        ArrayAdapter<JSONObject> adapter = new ArrayAdapter<JSONObject>(this, R.layout.item_favorite, data) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_favorite, parent, false);
                }

                TextView tvContent = convertView.findViewById(R.id.tvQuoteItem);
                TextView tvAuthor = convertView.findViewById(R.id.tvAuthorItem);
                Button btnDelete = convertView.findViewById(R.id.btnDelete);

                JSONObject obj = getItem(position);
                if (obj != null) {
                    String content = obj.optString("content", "");
                    String author = obj.optString("author", "");
                    tvContent.setText("\"" + content + "\"");
                    tvAuthor.setText("- " + author);
                }

                btnDelete.setOnClickListener(v -> {
                    favList.remove(position);
                    saveFavorites();
                    remove(obj);
                    notifyDataSetChanged();
                    Toast.makeText(MainActivity.this, "Quote dihapus", Toast.LENGTH_SHORT).show();
                });

                return convertView;
            }
        };

        listViewFav.setAdapter(adapter);

        listViewFav.setVisibility(View.VISIBLE);
        tvQuote.setVisibility(View.GONE);
        tvAuthor.setVisibility(View.GONE);
    }
}
