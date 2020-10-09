package br.natanael.android.instagram.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;

import br.natanael.android.instagram.R;

public class FiltroActivity extends AppCompatActivity {

    private ImageView imageFotoEscolhida;
    private Bitmap imagem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filtro);

        //Inicializar componentes
        imageFotoEscolhida = findViewById(R.id.imageFotoEscolhida);

        //Recupera a imagem escolhida pelo Usuario
        Bundle bundle = getIntent().getExtras();

        if(bundle != null)
        {
            byte[] dadosImagem = bundle.getByteArray("fotoEscolhida");

            if(dadosImagem != null)
            {
                imagem = BitmapFactory.decodeByteArray(dadosImagem,0,dadosImagem.length);
                imageFotoEscolhida.setImageBitmap(imagem);
            }
        }




    }
}
