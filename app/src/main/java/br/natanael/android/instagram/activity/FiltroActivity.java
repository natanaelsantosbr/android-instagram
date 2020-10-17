package br.natanael.android.instagram.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PostProcessor;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;


import com.google.android.gms.auth.api.signin.internal.Storage;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.zomato.photofilters.FilterPack;
import com.zomato.photofilters.imageprocessors.Filter;
import com.zomato.photofilters.utils.ThumbnailItem;
import com.zomato.photofilters.utils.ThumbnailsManager;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import br.natanael.android.instagram.R;
import br.natanael.android.instagram.adapter.AdapterMiniaturas;
import br.natanael.android.instagram.helper.ConfiguracaoFirebase;
import br.natanael.android.instagram.helper.RecyclerItemClickListener;
import br.natanael.android.instagram.helper.UsuarioFirebase;
import br.natanael.android.instagram.model.Postagem;
import br.natanael.android.instagram.model.Usuario;

public class FiltroActivity extends AppCompatActivity {
    static
    {
        System.loadLibrary("NativeImageProcessor");
    }

    private ImageView imageFotoEscolhida;
    private Bitmap imagem;
    private Bitmap imagemFiltro;

    private List<ThumbnailItem> listaFiltros;

    private RecyclerView recyclerFiltros;
    private AdapterMiniaturas adapterMiniaturas;

    private TextInputEditText textDescricaoFiltro;

    private String idUsuarioLogado;

    private ProgressBar progressPostagem;

    private DatabaseReference usuariosRef;
    private DatabaseReference usuarioLogadoRef;
    private Usuario usuarioLogado;
    private  boolean estaCarregando;

    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filtro);

        //configuracoes inciais
        listaFiltros = new ArrayList<>();
        idUsuarioLogado = UsuarioFirebase.getIdentificadorUsuario();
        textDescricaoFiltro = findViewById(R.id.textDescricaoFiltro);
        usuariosRef = ConfiguracaoFirebase.getFirebase().child("usuarios");



        //Inicializar componentes
        imageFotoEscolhida = findViewById(R.id.imageFotoEscolhida);
        recyclerFiltros = findViewById(R.id.recyclerFiltros);
        progressPostagem = findViewById(R.id.progressPostagem);
        progressPostagem.setVisibility(View.GONE);
        configurarToolbar();

        recuperarDadosDoUsuarioLogado();

        //Recupera a imagem escolhida pelo Usuario
        Bundle bundle = getIntent().getExtras();

        if(bundle != null)
        {
            byte[] dadosImagem = bundle.getByteArray("fotoEscolhida");

            if(dadosImagem != null)
            {
                imagem = BitmapFactory.decodeByteArray(dadosImagem,0,dadosImagem.length);
                imageFotoEscolhida.setImageBitmap(imagem);
                imagemFiltro = imagem.copy(imagem.getConfig(),true);

                //configurar recylcerView
                adapterMiniaturas = new AdapterMiniaturas(listaFiltros, getApplicationContext());
                RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
                recyclerFiltros.setLayoutManager(layoutManager);
                recyclerFiltros.setAdapter(adapterMiniaturas);


                //Adicionar evento de clieque no recyclerview
                recyclerFiltros.addOnItemTouchListener(new RecyclerItemClickListener(
                        getApplicationContext(), recyclerFiltros, new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        ThumbnailItem item = listaFiltros.get(position);

                        imagemFiltro = imagem.copy(imagem.getConfig(),true);
                        Filter filter = item.filter;
                        imageFotoEscolhida.setImageBitmap(filter.processFilter((imagemFiltro)));
                    }

                    @Override
                    public void onLongItemClick(View view, int position) {

                    }

                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                    }
                }
                ));


                //Recuperar filtros
                recuperarFiltros();
            }
        }
    }

    private void carregando(boolean estado ){
        if(estado)
        {
            estaCarregando = true;
            progressPostagem.setVisibility(View.VISIBLE);
        }
        else
        {
            estaCarregando = false;
            progressPostagem.setVisibility(View.GONE);
        }
    }

    private void recuperarDadosDoUsuarioLogado() {
        carregando(true);
        usuarioLogadoRef = usuariosRef.child(idUsuarioLogado);
        usuarioLogadoRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                //Recuperar usuario logado
                usuarioLogado = dataSnapshot.getValue(Usuario.class);
                carregando(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void recuperarFiltros() {

        //Limpar itens
        ThumbnailsManager.clearThumbs();
        listaFiltros.clear();

        //Configurar filtro normal
        ThumbnailItem item = new ThumbnailItem();
        item.image = imagem;
        item.filterName = "Normal";
        ThumbnailsManager.addThumb(item);

        //Lista todos os filtros
        List<Filter> filtros = FilterPack.getFilterPack(getApplicationContext());

        for (Filter filtro: filtros)
        {
            ThumbnailItem itemFiltro = new ThumbnailItem();
            itemFiltro.image = imagem;
            itemFiltro.filter  = filtro;
            itemFiltro.filterName = filtro.getName();

            ThumbnailsManager.addThumb(itemFiltro);
        }

        listaFiltros.addAll(ThumbnailsManager.processThumbs(getApplicationContext()));
        adapterMiniaturas.notifyDataSetChanged();


    }

    private void configurarToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbarPrincipal);
        toolbar.setTitle("Filtros");
        setSupportActionBar(toolbar);

        //Habilitar botao voltar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //Alterar o icone do botao voltar
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close_black_24dp);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_filtro, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId())
        {
            case R.id.ic_salvar_postagem:
                publicarPostagem();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void publicarPostagem() {
        if(estaCarregando){
            Toast.makeText(getApplicationContext(), "Carregando dados, aguarde!", Toast.LENGTH_SHORT).show();
        }
        else
        {
            progressPostagem.setVisibility(View.VISIBLE);
            final Postagem postagem = new Postagem();

            postagem.setIdUsuario(idUsuarioLogado);
            postagem.setDescricao(textDescricaoFiltro.getText().toString());


            //Recuperar dados da imagem para o firebase
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            imagemFiltro.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            byte[] dadosImagem = baos.toByteArray();


            //Salvar imagem no firebase storage
            StorageReference storageRef = ConfiguracaoFirebase.getFirebaseStorage();

            final StorageReference imagemRef = storageRef
                    .child("imagens")
                    .child("postagens")
                    .child(postagem.getId() + ".jpeg");

            final UploadTask uploadTask = imagemRef.putBytes(dadosImagem);
            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {

                    Toast.makeText(FiltroActivity.this, "Erro ao salvar uma imagem, tente novamente", Toast.LENGTH_SHORT).show();
                    progressPostagem.setVisibility(View.GONE);


                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {




                    imagemRef.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                        @Override
                        public void onComplete(@NonNull Task<Uri> task) {
                            Uri url = task.getResult();
                            postagem.setCaminhoFoto(url.toString());

                            if(postagem.salvar())
                            {
                                int quantidade = usuarioLogado.getPostagens() + 1;
                                usuarioLogado.setPostagens(quantidade);
                                usuarioLogado.atualizarQuantidadeDePostagem();


                                progressPostagem.setVisibility(View.GONE);
                                Toast.makeText(FiltroActivity.this, "Sucesso ao fazer o upload da imagem", Toast.LENGTH_SHORT).show();
                                finish();
                            }

                            //atualizarFotoUsuario(url);

                        }
                    });
                }
            });
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return false;
    }
}
