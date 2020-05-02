package com.alvin.androideatit;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alvin.androideatit.Adapter.MySearchAdapter;
import com.alvin.androideatit.Common.Common;
import com.alvin.androideatit.Model.FoodModel;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mancj.materialsearchbar.MaterialSearchBar;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class SearchActivity extends AppCompatActivity {

    @BindView(R.id.searchBar)
    MaterialSearchBar searchBar;
    @BindView(R.id.recycler_search)
    RecyclerView recycler_search;
    @BindView(R.id.search_null)
    TextView search_null;
    @BindView(R.id.tool_bar)
    Toolbar toolbar;

    Unbinder unbinder;

    List<String> suggestList = new ArrayList<>();
    List<FoodModel> localDataSource = new ArrayList<>();
    MySearchAdapter adapter, searchAdapter;

    List<FoodModel> tempList = null;
    List<FoodModel> result = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        unbinder = ButterKnife.bind(this);

        //initToolsBar();

        recycler_search.setLayoutManager(new GridLayoutManager(this, 1));

        localDataSource.clear();
        loadAllFoods();

        searchBar.setHint("Search your food");
        searchBar.setCardViewElevation(10);
        searchBar.addTextChangeListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                List<String> suggest = new ArrayList<>();
                suggest.clear();
                for (String search : suggestList) {
                    if (search.toLowerCase().contains(searchBar.getText().toLowerCase())) ;
                    suggest.add(search);
                }
                searchBar.setLastSuggestions(suggest);

            }

            @Override
            public void afterTextChanged(Editable s) {
                search_null.setVisibility(View.GONE);
            }
        });

        searchBar.setOnSearchActionListener(new MaterialSearchBar.OnSearchActionListener() {
            @Override
            public void onSearchStateChanged(boolean enabled) {
                if (!enabled)
                    recycler_search.setAdapter(adapter); // Restore all list of Food

            }

            @Override
            public void onSearchConfirmed(CharSequence text) {
                startSearchFoods(text);
            }

            @Override
            public void onButtonClicked(int buttonCode) {
            }
        });
    }

    private void initToolsBar() {
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        //getSupportActionBar().setTitle("Search");
    }


    private void loadAllFoods() {
        tempList = new ArrayList<>();
        DatabaseReference foodRef = FirebaseDatabase.getInstance().getReference(Common.ALLFOODS_REF);

        foodRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds:dataSnapshot.getChildren()){
                    FoodModel model;
                    model = ds.getValue(FoodModel.class);

                    tempList.add(model);

                    displayListFood(tempList);
                    //buildSuggestList(tempList);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    private void buildSuggestList(List<FoodModel> tempList) {
        for (FoodModel food:tempList)
            suggestList.add(food.getName());

        searchBar.setLastSuggestions(suggestList);
        searchBar.setMaxSuggestionCount(4);
    }


    private void displayListFood(List<FoodModel> tempList) {
        localDataSource = tempList;
        adapter = new MySearchAdapter(this, tempList);
        recycler_search.setAdapter(adapter);
    }

    private void startSearchFoods(CharSequence text) {
        result = new ArrayList<>();
        for (FoodModel foodModel:localDataSource)
            if (foodModel.getName().toLowerCase().contains(text) || foodModel.getName().contains(text)){
                result.add(foodModel);
                search_null.setVisibility(View.GONE);
            }
            else
                search_null.setVisibility(View.VISIBLE);

        searchAdapter = new MySearchAdapter(this, result);
        recycler_search.setAdapter(searchAdapter);
    }

    @Override
    protected void onStop() {

        adapter.clearCompositeDisposable();
        DatabaseReference.goOffline(); // Clear Reference
        suggestList.clear();
        //searchBar.clearSuggestions();

        localDataSource.clear();
        tempList.clear();

        finish();

        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        //searchBar.clearSuggestions();

        suggestList.clear();
        localDataSource.clear();
        tempList.clear();
    }
}
