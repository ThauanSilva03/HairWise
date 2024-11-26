package com.example.hairwise;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private EditText editTextQuimico;
    private Button buttonBuscar;
    private TextView textViewResultado;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Vinculando os componentes do layout
        editTextQuimico = findViewById(R.id.editTextQuimico);
        buttonBuscar = findViewById(R.id.buttonBuscar);
        textViewResultado = findViewById(R.id.textViewResultado);

        // Configurando o clique do botão
        buttonBuscar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String nomeComposto = editTextQuimico.getText().toString().trim();

                if (nomeComposto.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Por favor, insira o nome do composto.", Toast.LENGTH_SHORT).show();
                } else {
                    buscarDadosDoComposto(nomeComposto);
                }
            }
        });
    }

    private void buscarDadosDoComposto(String nomeComposto) {
        // Configurando Retrofit
        PubChemApi apiService = ApiClient.getRetrofitInstance().create(PubChemApi.class);

        // Fazendo a chamada para a API
        Call<CompoundResponse> call = apiService.getDescription(nomeComposto);

        call.enqueue(new Callback<CompoundResponse>() {
            @Override
            public void onResponse(Call<CompoundResponse> call, Response<CompoundResponse> response) {
                if (response.isSuccessful()) {
                    CompoundResponse compoundResponse = response.body();

                    if (compoundResponse != null) {
                        // Acessando a lista de informações
                        List<CompoundResponse.Information> informationList = compoundResponse.getInformationList().getInformation();

                        if (informationList != null && !informationList.isEmpty()) {
                            // Pegando o primeiro item da lista (você pode alterar isso conforme necessário)
                            CompoundResponse.Information info2 = informationList.get(0);
                            CompoundResponse.Information info = informationList.get(1); // index 1 para pegar o item com a descrição

                            String titulo = info2.getTitle(); // Nome do composto
                            String descricao = info.getDescription(); // Descrição do composto

                            Log.d("API Response", "Título: " + titulo);
                            Log.d("API Response", "Descrição: " + descricao);

                            if (descricao != null && !descricao.isEmpty()) {
                                textViewResultado.setText("Nome: " + titulo + "\n\nDescrição: " + descricao);
                            } else {
                                textViewResultado.setText("Descrição não encontrada.");
                            }
                        } else {
                            textViewResultado.setText("Nenhuma informação encontrada.");
                        }
                    } else {
                        textViewResultado.setText("Resposta da API é null.");
                    }
                } else {
                    textViewResultado.setText("Erro na API: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<CompoundResponse> call, Throwable t) {
                textViewResultado.setText("Falha na conexão: " + t.getMessage());
            }
        });
    }
}

