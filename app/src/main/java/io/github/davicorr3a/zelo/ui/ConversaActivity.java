package io.github.davicorr3a.zelo.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import io.github.davicorr3a.zelo.R;
import io.github.davicorr3a.zelo.guardiao.AlertasGuardiao;
import io.github.davicorr3a.zelo.util.Atalhos;

/**
 * A única tela do app: uma conversa com o Zelo.
 * Toda a lógica fica no ConversaViewModel e no Roteiro. A activity só desenha e repassa toques.
 */
public class ConversaActivity extends AppCompatActivity {

    private ConversaViewModel viewModel;
    private RecyclerView lista;

    // Se a pessoa negar, os avisos do guardião não aparecem, mas a conversa segue igual.
    private final ActivityResultLauncher<String> pedidoDeNotificacao =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), concedida -> { });

    @Override
    protected void onCreate(Bundle estadoSalvo) {
        super.onCreate(estadoSalvo);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_conversa);
        respeitarBarrasDoSistema(findViewById(R.id.raiz));

        viewModel = new ViewModelProvider(this).get(ConversaViewModel.class);

        ConversaAdapter adapter = new ConversaAdapter((item, acao) -> viewModel.responder(item, acao));
        lista = findViewById(R.id.lista);
        lista.setLayoutManager(new LinearLayoutManager(this));
        lista.setAdapter(adapter);

        viewModel.getItens().observe(this, itens -> adapter.submitList(itens, () -> {
            if (!itens.isEmpty()) lista.smoothScrollToPosition(itens.size() - 1);
        }));

        viewModel.getTelaParaAbrir().observe(this, evento -> {
            Intent tela = evento.consumir();
            if (tela != null) Atalhos.abrir(this, tela);
        });

        if (estadoSalvo == null) pedirPermissaoDeNotificacao();

        Intent intent = getIntent();
        viewModel.iniciarSeNecessario(
                intent.getStringExtra(AlertasGuardiao.EXTRA_PACOTE),
                intent.getStringExtra(AlertasGuardiao.EXTRA_MOTIVO));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        // Chegou um aviso do guardião com o app já aberto: a conversa recomeça por ele.
        viewModel.recomecar(
                intent.getStringExtra(AlertasGuardiao.EXTRA_PACOTE),
                intent.getStringExtra(AlertasGuardiao.EXTRA_MOTIVO));
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.retomar();
    }

    private void pedirPermissaoDeNotificacao() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            pedidoDeNotificacao.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    /** O app desenha atrás das barras do sistema, então o conteúdo ganha margem do tamanho delas. */
    private void respeitarBarrasDoSistema(View raiz) {
        int esquerda = raiz.getPaddingLeft();
        int topo = raiz.getPaddingTop();
        int direita = raiz.getPaddingRight();
        int baixo = raiz.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(raiz, (v, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(esquerda + barras.left, topo + barras.top, direita + barras.right, baixo + barras.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }
}
