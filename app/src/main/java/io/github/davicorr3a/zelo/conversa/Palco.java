package io.github.davicorr3a.zelo.conversa;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

import io.github.davicorr3a.zelo.R;
import io.github.davicorr3a.zelo.guardiao.GuardiaoService;

/**
 * O que todas as cenas da conversa compartilham: a tela onde as falas aparecem, os textos,
 * a espera pela volta de uma tela do sistema e o trabalho em segundo plano.
 *
 * O {@link Roteiro} decide a ordem, cada parte da conversa é uma cena, e todas acontecem no mesmo palco.
 */
final class Palco {

    private static final long ESPERA_AO_VOLTAR_MS = 800;

    private final Context contexto;
    private final Conversa conversa;
    private final ExecutorService segundoPlano = Executors.newSingleThreadExecutor();
    private final Handler principal = new Handler(Looper.getMainLooper());

    private Runnable aoRetomar;
    // Muda a cada recomeço. Resultados de uma análise antiga são descartados.
    private int sessao;

    Palco(Context contexto, Conversa conversa) {
        this.contexto = contexto.getApplicationContext();
        this.conversa = conversa;
    }

    Context contexto() {
        return contexto;
    }

    void dizer(Mensagem mensagem) {
        conversa.dizer(mensagem);
    }

    /** Abre uma tela do sistema e guarda o próximo momento para quando a pessoa voltar. */
    void abrirEDepois(Intent tela, Runnable depois) {
        aoRetomar = depois;
        conversa.abrir(tela);
    }

    void retomar() {
        Runnable proximo = aoRetomar;
        aoRetomar = null;
        if (proximo != null) principal.postDelayed(proximo, ESPERA_AO_VOLTAR_MS);
    }

    /** Roda a tarefa fora da thread principal e entrega o resultado nela, se a conversa não recomeçou nesse meio-tempo. */
    <T> void emSegundoPlano(Supplier<T> tarefa, Consumer<T> depois) {
        int minhaSessao = sessao;
        segundoPlano.execute(() -> {
            T resultado = tarefa.get();
            principal.post(() -> {
                if (minhaSessao == sessao) depois.accept(resultado);
            });
        });
    }

    void encerrar() {
        boolean deGuarda = GuardiaoService.ativo() != null;
        dizer(Mensagem.de(t(deGuarda ? R.string.fim_com_guardiao : R.string.fim)));
    }

    void reiniciar() {
        sessao++;
        aoRetomar = null;
        principal.removeCallbacksAndMessages(null);
    }

    void liberar() {
        reiniciar();
        segundoPlano.shutdownNow();
    }

    String t(int id, Object... argumentos) {
        return argumentos.length == 0 ? contexto.getString(id) : contexto.getString(id, argumentos);
    }

    String p(int id, int quantidade, Object... argumentos) {
        return contexto.getResources().getQuantityString(id, quantidade, argumentos);
    }
}
