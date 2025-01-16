package com.example.ecommerceapp;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;

import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;

import java.util.ArrayList;
import java.util.List;

public class PlaceAutoSuggestAdapter extends ArrayAdapter<String> implements Filterable {

    private List<String> suggestions;
    private final PlacesClient placesClient;

    public PlaceAutoSuggestAdapter(Context context, PlacesClient placesClient) {
        super(context, android.R.layout.simple_dropdown_item_1line);
        this.placesClient = placesClient;
        this.suggestions = new ArrayList<>();
    }

    @Override
    public int getCount() {
        return suggestions.size();
    }

    @Override
    public String getItem(int position) {
        return suggestions.get(position);
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults filterResults = new FilterResults();
                if (constraint != null) {
                    suggestions = getPlaceSuggestions(constraint.toString());
                    filterResults.values = suggestions;
                    filterResults.count = suggestions.size();
                }
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if (results != null && results.count > 0) {
                    notifyDataSetChanged();
                } else {
                    notifyDataSetInvalidated();
                }
            }
        };
    }

    private List<String> getPlaceSuggestions(String query) {
        List<String> placeSuggestions = new ArrayList<>();

        FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                .setQuery(query)
                .setCountries("CA") // Restrict to Canada
                .build();

        placesClient.findAutocompletePredictions(request).addOnSuccessListener(response -> {
            for (AutocompletePrediction prediction : response.getAutocompletePredictions()) {
                placeSuggestions.add(prediction.getFullText(null).toString());
            }
            notifyDataSetChanged();
        }).addOnFailureListener(Throwable::printStackTrace);

        return placeSuggestions;
    }
}
