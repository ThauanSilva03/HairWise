package com.example.hairwise;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

public class CompostoDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "compostos.db";
    private static final int DATABASE_VERSION = 1;
    private static final String PREFS_NAME = "DatabasePrefs";
    private static final String LAST_JSON_HASH = "last_json_hash";

    public static final String TABLE_COMPOSTOS = "compostos";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_NOME = "nome";
    public static final String COLUMN_DESCRICAO = "descricao";
    public static final String COLUMN_FUNCAO = "funcao";

    private Context context;
    private SharedPreferences prefs;

    private static final String CREATE_TABLE_COMPOSTOS = "CREATE TABLE " + TABLE_COMPOSTOS + " (" +
            COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_NOME + " TEXT, " +
            COLUMN_DESCRICAO + " TEXT, " +
            COLUMN_FUNCAO + " TEXT);";

    public CompostoDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_COMPOSTOS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_COMPOSTOS);
        onCreate(db);
    }

    private String calculateJsonHash(String jsonContent) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(jsonContent.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (Exception e) {
            Log.e("CompostosDB", "Erro ao calcular hash do JSON", e);
            return "";
        }
    }

    private String readJsonContent() {
        try {
            InputStream inputStream = context.getAssets().open("compostos.json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder stringBuilder = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }

            return stringBuilder.toString();
        } catch (Exception e) {
            Log.e("CompostosDB", "Erro ao ler arquivo JSON", e);
            return "";
        }
    }

    public void verificarEAtualizarDados() {
        String jsonContent = readJsonContent();
        String currentHash = calculateJsonHash(jsonContent);
        String savedHash = prefs.getString(LAST_JSON_HASH, "");

        if (!currentHash.equals(savedHash)) {
            Log.d("CompostosDB", "JSON modificado, atualizando banco de dados...");
            SQLiteDatabase db = this.getWritableDatabase();

            try {
                db.beginTransaction();

                // Limpa a tabela atual
                db.delete(TABLE_COMPOSTOS, null, null);

                // Importa os novos dados
                JSONArray jsonArray = new JSONArray(jsonContent);

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    ContentValues values = new ContentValues();

                    values.put(COLUMN_NOME, jsonObject.getString("nome"));
                    values.put(COLUMN_DESCRICAO, jsonObject.getString("descricao"));
                    values.put(COLUMN_FUNCAO, jsonObject.getString("funcao"));

                    db.insert(TABLE_COMPOSTOS, null, values);
                }

                db.setTransactionSuccessful();

                // Salva o novo hash
                prefs.edit().putString(LAST_JSON_HASH, currentHash).apply();
                Log.d("CompostosDB", "Banco de dados atualizado com sucesso");

            } catch (Exception e) {
                Log.e("CompostosDB", "Erro ao atualizar banco de dados", e);
            } finally {
                if (db.inTransaction()) {
                    db.endTransaction();
                }
            }
        } else {
            Log.d("CompostosDB", "JSON nÃ£o foi modificado, mantendo dados existentes");
        }
    }

    public void adicionarComposto(Composto composto) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_NOME, composto.getNome());
        values.put(COLUMN_DESCRICAO, composto.getDescricao());
        values.put(COLUMN_FUNCAO, composto.getFuncao());

        db.insert(TABLE_COMPOSTOS, null, values);
        db.close();
    }

    public List<String> getAllCompostosNomes() {
        List<String> compostosNomes = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_COMPOSTOS, new String[]{COLUMN_NOME}, null, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String nomeComposto = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOME));
                compostosNomes.add(nomeComposto);

                // Log para verificar se o nome está sendo adicionado
                Log.d("CompostosDB", "Composto encontrado: " + nomeComposto);
            } while (cursor.moveToNext());
            cursor.close();
        }

        // Log para verificar a lista completa de compostos
        Log.d("CompostosDB", "Compostos encontrados: " + compostosNomes.toString());

        return compostosNomes;
    }


    public List<Composto> getAllCompostos() {
        List<Composto> compostos = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        try {
            Cursor cursor = db.query(TABLE_COMPOSTOS, null, null, null, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                int nomeIndex = cursor.getColumnIndexOrThrow(COLUMN_NOME);
                int descricaoIndex = cursor.getColumnIndexOrThrow(COLUMN_DESCRICAO);
                int funcaoIndex = cursor.getColumnIndexOrThrow(COLUMN_FUNCAO);

                do {
                    String nome = cursor.getString(nomeIndex);
                    String descricao = cursor.getString(descricaoIndex);
                    String funcao = cursor.getString(funcaoIndex);

                    compostos.add(new Composto(nome, descricao, funcao));
                } while (cursor.moveToNext());
            }

            if (cursor != null) {
                cursor.close();
            }
        } catch (Exception e) {
            Log.e("Database Error", "Error fetching compostos: " + e.getMessage());
        }

        return compostos;
    }

    public Composto getCompostoByNome(String nome) {
        SQLiteDatabase db = this.getReadableDatabase();
        Composto composto = null;

        try {
            String[] columns = {COLUMN_NOME, COLUMN_DESCRICAO, COLUMN_FUNCAO};
            String selection = "LOWER(" + COLUMN_NOME + ") = LOWER(?)";
            String[] selectionArgs = {nome};

            Cursor cursor = db.query(TABLE_COMPOSTOS, columns, selection, selectionArgs, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                int nomeIndex = cursor.getColumnIndexOrThrow(COLUMN_NOME);
                int descricaoIndex = cursor.getColumnIndexOrThrow(COLUMN_DESCRICAO);
                int funcaoIndex = cursor.getColumnIndexOrThrow(COLUMN_FUNCAO);

                composto = new Composto(
                        cursor.getString(nomeIndex),
                        cursor.getString(descricaoIndex),
                        cursor.getString(funcaoIndex)
                );
            }

            if (cursor != null) {
                cursor.close();
            }
        } catch (Exception e) {
            Log.e("Database Error", "Error fetching composto by name: " + e.getMessage());
        }

        return composto;
    }
}
