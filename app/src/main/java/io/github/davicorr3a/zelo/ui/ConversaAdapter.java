package io.github.davicorr3a.zelo.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import io.github.davicorr3a.zelo.R;
import io.github.davicorr3a.zelo.conversa.Acao;
import io.github.davicorr3a.zelo.conversa.Mensagem;

/** Desenha a conversa: falas do Zelo à esquerda, com botões, e respostas da pessoa à direita. */
public class ConversaAdapter extends ListAdapter<ItemConversa, RecyclerView.ViewHolder> {

    public interface AoResponder {
        void responder(ItemConversa item, Acao acao);
    }

    private static final int TIPO_ZELO = 0;
    private static final int TIPO_PESSOA = 1;

    private static final DiffUtil.ItemCallback<ItemConversa> COMPARADOR = new DiffUtil.ItemCallback<ItemConversa>() {
        @Override
        public boolean areItemsTheSame(@NonNull ItemConversa antigo, @NonNull ItemConversa novo) {
            return antigo.getId() == novo.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull ItemConversa antigo, @NonNull ItemConversa novo) {
            // O texto de um item nunca muda. O que muda é ele ter sido respondido.
            return antigo.isRespondida() == novo.isRespondida();
        }
    };

    private final AoResponder aoResponder;

    public ConversaAdapter(AoResponder aoResponder) {
        super(COMPARADOR);
        this.aoResponder = aoResponder;
    }

    @Override
    public int getItemViewType(int posicao) {
        return getItem(posicao).getTipo() == ItemConversa.Tipo.ZELO ? TIPO_ZELO : TIPO_PESSOA;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup pai, int tipo) {
        LayoutInflater inflater = LayoutInflater.from(pai.getContext());
        if (tipo == TIPO_ZELO) {
            return new FalaDoZelo(inflater.inflate(R.layout.item_zelo, pai, false));
        }
        return new FalaDaPessoa(inflater.inflate(R.layout.item_pessoa, pai, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int posicao) {
        ItemConversa item = getItem(posicao);
        if (holder instanceof FalaDoZelo) {
            ((FalaDoZelo) holder).vincular(item);
        } else {
            ((FalaDaPessoa) holder).vincular(item);
        }
    }

    class FalaDoZelo extends RecyclerView.ViewHolder {

        private final MaterialCardView cartao;
        private final TextView texto;
        private final LinearLayout acoes;

        FalaDoZelo(View view) {
            super(view);
            cartao = view.findViewById(R.id.cartao);
            texto = view.findViewById(R.id.texto);
            acoes = view.findViewById(R.id.acoes);
        }

        void vincular(ItemConversa item) {
            Mensagem mensagem = item.getMensagem();
            Context contexto = itemView.getContext();
            texto.setText(mensagem.getTexto());

            if (mensagem.isAlerta()) {
                cartao.setCardBackgroundColor(ContextCompat.getColor(contexto, R.color.alerta_container));
                cartao.setStrokeColor(ContextCompat.getColor(contexto, R.color.alerta));
                cartao.setStrokeWidth(contexto.getResources().getDimensionPixelSize(R.dimen.borda_alerta));
            } else {
                cartao.setCardBackgroundColor(ContextCompat.getColor(contexto, R.color.superficie));
                cartao.setStrokeColor(ContextCompat.getColor(contexto, R.color.contorno));
                cartao.setStrokeWidth(contexto.getResources().getDimensionPixelSize(R.dimen.borda_cartao));
            }

            acoes.removeAllViews();
            if (!item.mostraBotoes()) {
                acoes.setVisibility(View.GONE);
                return;
            }

            LayoutInflater inflater = LayoutInflater.from(contexto);
            for (Acao acao : mensagem.getAcoes()) {
                int layout = acao.isPrincipal() ? R.layout.item_botao_principal : R.layout.item_botao;
                MaterialButton botao = (MaterialButton) inflater.inflate(layout, acoes, false);
                botao.setText(acao.getRotulo());
                botao.setOnClickListener(v -> aoResponder.responder(item, acao));
                acoes.addView(botao);
            }
            acoes.setVisibility(View.VISIBLE);
        }
    }

    static class FalaDaPessoa extends RecyclerView.ViewHolder {

        private final TextView texto;

        FalaDaPessoa(View view) {
            super(view);
            texto = view.findViewById(R.id.texto);
        }

        void vincular(ItemConversa item) {
            texto.setText(item.getTexto());
        }
    }
}
