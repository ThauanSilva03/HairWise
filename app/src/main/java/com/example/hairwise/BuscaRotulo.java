package com.example.hairwise;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class BuscaRotulo extends AppCompatActivity {

    private Button scannerBtn, copyBtn;
    private TextView textView_data;
    private String currentPhotoPath;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<Uri> takePictureLauncher;
    private final CompoundList cl = new CompoundList();
    private final List<String> keywords = cl.getKeywords();
    private CompostoDatabaseHelper dbHelper;
    private GridLayout gridLayout;
    private int columnCount = 3;
    private ImageView btnHome, btnSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_busca_rotulo);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        scannerBtn = findViewById(R.id.scannerBtn);
        copyBtn = findViewById(R.id.copyBtn);
        textView_data = findViewById(R.id.textView_data);
        gridLayout = findViewById(R.id.gridLayout);
        btnSearch = findViewById(R.id.btnSearch);
        btnHome = findViewById(R.id.btnHome);


        gridLayout.setColumnCount(columnCount);


        btnHome.setOnClickListener(view -> {
            Intent intent = new Intent(BuscaRotulo.this, Home.class);
            startActivity(intent);
        });

        btnSearch.setOnClickListener(view -> {
            Intent intent = new Intent(BuscaRotulo.this, MainActivity.class);
            startActivity(intent);
        });

        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if(isGranted){
                        captureImage();
                    }else{
                        Toast.makeText(this, "Permissão para a câmera negada", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if(success){
                        Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath);
                        //cameraImage.setImageBitmap(bitmap); (ImageView)
                        gridLayout.removeAllViews();
                        recognizeText(bitmap);
                    }
                }
        );

        scannerBtn.setOnClickListener(view ->
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        );
    }
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile("JPEG_"+timeStamp+"_",".jpg",storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void captureImage(){
        File photoFile = null;
        try{
            photoFile = createImageFile();
        } catch (IOException ex){
            Toast.makeText(this, "Aconteceu um erro ao criar o arquivo", Toast.LENGTH_SHORT).show();
        }
        if(photoFile != null){
            Uri photoUri = FileProvider.getUriForFile(this,
                    getApplicationContext().getPackageName() + ".provider",
                    photoFile);
            takePictureLauncher.launch(photoUri);
        }
    }

    private void recognizeText(Bitmap bitmap) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        recognizer.process(image)
                .addOnSuccessListener(ocrText -> {

                    List<String> result = extrairPalavras(ocrText.getText(), keywords);

                    if (result != null && !result.isEmpty()) {
                        try {

                            if (dbHelper == null) {
                                dbHelper = new CompostoDatabaseHelper(this);
                            }
                            createButton(result, columnCount, textView_data);
                        } catch (Exception e) {
                            Log.e("DatabaseError", "Erro ao buscar dados do composto: " + e.getMessage());
                            textView_data.setText("Erro ao acessar o banco de dados.");
                        }
                    } else {

                        textView_data.setText("Nenhuma palavra-chave reconhecida.");
                    }

                    textView_data.setMovementMethod(new ScrollingMovementMethod());
                    copyBtn.setVisibility(View.VISIBLE);

                    copyBtn.setOnClickListener(v -> {
                        ClipboardManager clipboard = ContextCompat.getSystemService(
                                this,
                                ClipboardManager.class
                        );
                        String textoParaCopiar = textView_data.getText().toString();

                        ClipData clip = ClipData.newPlainText("Recognized text", textoParaCopiar);
                        if (clipboard != null) {
                            clipboard.setPrimaryClip(clip);
                            Toast.makeText(this, "Texto copiado", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Falha ao reconhecer texto.", Toast.LENGTH_SHORT).show()
                );
    }

    public List<String> extrairPalavras(String texto, List<String> palavrasChaves){
        List<String> palavrasEncontradas = new ArrayList<>();

        for(String palavra : palavrasChaves){
            if(texto.toLowerCase().contains(palavra.toLowerCase())){
                palavrasEncontradas.add(palavra);
            }
        }
        return palavrasEncontradas;
    }
    private String buscarDadosDoComposto(String nomeComposto) {
        Composto compostoLocal = dbHelper.getCompostoByNome(nomeComposto);
        if (compostoLocal != null) {
            return String.format(
                    "Nome: %s\n\n" +
                            "Descrição: %s\n\n" +
                            "Função: %s",
                    compostoLocal.getNome(),
                    compostoLocal.getDescricao(),
                    compostoLocal.getFuncao()
            );
        } else {
            return "Composto nao encontrado no banco de dados";
        }
    }
    private void createButton(List<String> items, int columnCount, TextView textView_data){
        for (int i = 0; i < items.size(); i++) {
            String item = items.get(i);

            Button button = new Button(this);
            button.setText(item);
            button.setBackgroundColor(Color.parseColor("#56CCD9"));
            button.setTextColor(Color.parseColor("#FFFFFF"));

            button.setGravity(Gravity.CENTER);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = 200;
            params.columnSpec = GridLayout.spec(i % columnCount, 1, 1f);
            params.rowSpec = GridLayout.spec(i / columnCount);
            params.setMargins(2,0,2,2);


            button.setLayoutParams(params);

            button.setOnClickListener(v -> {
                        String dadosComposto = buscarDadosDoComposto(item);
                        textView_data.setText(dadosComposto);
                    }
            );

            gridLayout.addView(button);
        }
    }
}