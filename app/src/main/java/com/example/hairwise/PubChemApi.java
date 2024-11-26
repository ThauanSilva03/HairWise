package com.example.hairwise;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface PubChemApi {
    @GET("{quimico}/description/JSON")
    Call<CompoundResponse> getDescription(@Path("quimico") String quimico);
}
