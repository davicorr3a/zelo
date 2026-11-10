package io.github.davicorr3a.zelo.ui;

import android.app.Application;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.github.davicorr3a.zelo.conversa.Acao;
import io.github.davicorr3a.zelo.conversa.Conversa;
import io.github.davicorr3a.zelo.conversa.Mensagem;
import io.github.davicorr3a.zelo.conversa.Roteiro;

/**
 * Segura a conversa e o roteiro fora da activity, para que nada se perca ao girar a tela.
 * A activity só observa a lista de itens e os pedidos para abrir telas do sistema.
 */
public class ConversaViewModel extends AndroidViewModel implements Conversa {

    private final MutableLiveData<List<ItemConversa>> itens = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<Evento<Intent>> telaParaAbrir = new MutableLiveData<>();
    private final Roteiro roteiro;

    private long proximoId;
    private boolean iniciada;

    public ConversaViewModel(@NonNull Application application) {
        super(application);
        roteiro = new Roteiro(application, this);
    }

    public LiveData<List<ItemConversa>> getItens() {
        return itens;
    }

    public LiveData<Evento<Intent>> getTelaParaAbrir() {
        return telaParaAbrir;
    }

    /** Começa a conversa só se ela ainda não existe. Ao girar a tela, nada acontece. */
    public void iniciarSeNecessario(String pacote, String motivo) {
        if (!iniciada) recomecar(pacote, motivo);
    }

    /**
     * Apaga a conversa e começa de novo.
     * Com um pacote, começa pelo aviso do guardião; sem pacote, pela revisão normal.
     */
    public void recomecar(String pacote, String motivo) {
        iniciada = true;
        itens.setValue(Collections.emptyList());
        if (pacote != null) {
            roteiro.alertaDoGuardiao(pacote, motivo);
        } else {
            roteiro.comecar();
        }
    }

    public void retomar() {
        roteiro.retomar();
    }

    public void responder(ItemConversa item, Acao acao) {
        List<ItemConversa> atual = new ArrayList<>(listaAtual());
        int posicao = posicaoDe(atual, item.getId());
        // Ignora toque duplo ou toque em mensagem antiga.
        if (posicao < 0 || atual.get(posicao).isRespondida()) return;

        atual.set(posicao, atual.get(posicao).comoRespondida());
        atual.add(ItemConversa.daPessoa(proximoId++, acao.getRotulo()));
        itens.setValue(atual);

        acao.getAoTocar().run();
    }

    @Override
    public void dizer(Mensagem mensagem) {
        List<ItemConversa> atual = new ArrayList<>(listaAtual());
        atual.add(ItemConversa.doZelo(proximoId++, mensagem));
        itens.setValue(atual);
    }

    @Override
    public void abrir(Intent intent) {
        telaParaAbrir.setValue(new Evento<>(intent));
    }

    @Override
    protected void onCleared() {
        roteiro.liberar();
    }

    private List<ItemConversa> listaAtual() {
        List<ItemConversa> lista = itens.getValue();
        return lista != null ? lista : Collections.emptyList();
    }

    private static int posicaoDe(List<ItemConversa> lista, long id) {
        for (int i = 0; i < lista.size(); i++) {
            if (lista.get(i).getId() == id) return i;
        }
        return -1;
    }
}
