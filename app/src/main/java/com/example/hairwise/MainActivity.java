package com.example.hairwise;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private EditText editTextQuimico;
    private Button buttonBuscar, btnVoltar;
    private TextView textViewResultadoLocal;
    private TextView textViewResultadoAPI;
    private CompostoDatabaseHelper dbHelper;
    private LinearLayout linearLayoutAPI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editTextQuimico = findViewById(R.id.editTextQuimico);
        buttonBuscar = findViewById(R.id.buttonBuscar);
        textViewResultadoLocal = findViewById(R.id.textViewResultadoLocal);
        textViewResultadoAPI = findViewById(R.id.textViewResultadoAPI);
        linearLayoutAPI = findViewById(R.id.apiResponseLayout);
        btnVoltar = findViewById(R.id.buttonVoltar);

        initializeDatabase();

        btnVoltar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, Home.class);
                startActivity(intent);
            }
        });

        buttonBuscar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String nomeComposto = editTextQuimico.getText().toString().trim();

                if (nomeComposto.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Por favor, insira o nome do composto.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Verifica e atualiza o banco antes de cada busca
                dbHelper.verificarEAtualizarDados();

                textViewResultadoLocal.setText("Buscando...");
                textViewResultadoAPI.setText("Buscando...");

                buscarDadosDoComposto(nomeComposto);
                buscarDadosNaAPI(nomeComposto);
            }
        });
    }

    private void initializeDatabase() {
        dbHelper = new CompostoDatabaseHelper(this);
        dbHelper.verificarEAtualizarDados();
    }

    private void buscarDadosDoComposto(String nomeComposto) {
        Composto compostoLocal = dbHelper.getCompostoByNome(nomeComposto);
        if (compostoLocal != null) {
            String resultado = String.format(
                    "Nome: %s\n\n" +
                            "Descrição: %s\n\n" +
                            "Função: %s",
                    compostoLocal.getNome(),
                    compostoLocal.getDescricao(),
                    compostoLocal.getFuncao()
            );
            textViewResultadoLocal.setText(resultado);
            Log.d("MainActivity", "Composto encontrado localmente: " + compostoLocal.getNome());
        } else {
            textViewResultadoLocal.setText("Composto não encontrado no banco de dados local.");
            Log.d("MainActivity", "Composto não encontrado localmente: " + nomeComposto);
        }
    }

    private void buscarDadosNaAPI(String nomeComposto) {
        Log.d("BuscaAPI", "Buscando na API pelo composto: " + nomeComposto);
        PubChemApi apiService = ApiClient.getRetrofitInstance().create(PubChemApi.class);

        Call<CompoundResponse> call = apiService.getDescription(nomeComposto);
        call.enqueue(new Callback<CompoundResponse>() {
            @Override
            public void onResponse(Call<CompoundResponse> call, Response<CompoundResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    CompoundResponse compoundResponse = response.body();
                    List<CompoundResponse.Information> informationList =
                            compoundResponse.getInformationList().getInformation();

                    if (informationList != null && informationList.size() >= 2) {
                        CompoundResponse.Information info2 = informationList.get(0);
                        CompoundResponse.Information info = informationList.get(1);

                        String titulo = info2.getTitle();
                        String descricao = info.getDescription();

                        Log.d("API Response", "Título: " + titulo);
                        Log.d("API Response", "Descrição: " + descricao);

                        if (descricao != null && !descricao.isEmpty()) {
                            linearLayoutAPI.setVisibility(View.VISIBLE);
                            String resultado = String.format(
                                    "Nome: %s\n\n" +
                                            "Descrição: %s",
                                    titulo,
                                    descricao
                            );
                            textViewResultadoAPI.setText(resultado);
                        } else {
                            linearLayoutAPI.setVisibility(View.GONE);
                            textViewResultadoAPI.setText("Descrição não encontrada na API.");
                        }
                    } else {
                        linearLayoutAPI.setVisibility(View.GONE);
                        textViewResultadoAPI.setText("Informações insuficientes encontradas na API.");
                    }
                } else {
                    linearLayoutAPI.setVisibility(View.GONE);
                    textViewResultadoAPI.setText("Erro na API: " + response.code());
                    Log.e("API Error", "Erro " + response.code() + " na API para o composto: " + nomeComposto);
                }
            }

            @Override
            public void onFailure(Call<CompoundResponse> call, Throwable t) {
                textViewResultadoAPI.setText("Falha na conexão com a API: " + t.getMessage());
                Log.e("API Error", "Falha na conexão com a API: " + t.getMessage());
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}