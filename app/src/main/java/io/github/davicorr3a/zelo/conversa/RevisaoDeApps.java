package io.github.davicorr3a.zelo.conversa;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import io.github.davicorr3a.zelo.R;
import io.github.davicorr3a.zelo.analise.AnalisadorApps;
import io.github.davicorr3a.zelo.analise.AppSuspeito;
import io.github.davicorr3a.zelo.analise.AppSuspeito.Acesso;
import io.github.davicorr3a.zelo.util.Preferencias;

/** Olha os apps instalados e pergunta, um por um, sobre os que merecem atenção. */
final class RevisaoDeApps {

    private final Palco palco;
    private final AnalisadorApps analisador;
    private final Preferencias preferencias;
    private final RemocaoGuiada remocao;
    private final Runnable aoTerminar;
    private final Deque<AppSuspeito> fila = new ArrayDeque<>();

    RevisaoDeApps(Palco palco, AnalisadorApps analisador, Preferencias preferencias,
                  RemocaoGuiada remocao, Runnable aoTerminar) {
        this.palco = palco;
        this.analisador = analisador;
        this.preferencias = preferencias;
        this.remocao = remocao;
        this.aoTerminar = aoTerminar;
    }

    void comecar() {
        palco.dizer(Mensagem.de(palco.t(R.string.um_instante)));
        palco.emSegundoPlano(analisador::analisar, this::apresentarAchados);
    }

    private void apresentarAchados(List<AppSuspeito> achados) {
        fila.clear();
        fila.addAll(achados);

        if (achados.isEmpty()) {
            palco.dizer(Mensagem.de(palco.t(R.string.nenhum_suspeito)));
            aoTerminar.run();
            return;
        }

        String texto = palco.p(R.plurals.achados, achados.size(), achados.size());
        palco.dizer(Mensagem.de(texto).com(Acao.principal(palco.t(R.string.vamos), this::proximoApp)));
    }

    private void proximoApp() {
        AppSuspeito app = fila.poll();
        if (app == null) {
            palco.dizer(Mensagem.de(palco.t(R.string.fim_apps)));
            aoTerminar.run();
            return;
        }

        palco.dizer(Mensagem.alerta(descrever(app) + "\n\n" + palco.t(R.string.reconhece))
                .com(Acao.secundaria(palco.t(R.string.reconheco), () -> {
                    preferencias.confiar(app.getPacote());
                    proximoApp();
                }))
                .com(Acao.principal(palco.t(R.string.nao_conheco), () -> remocao.tirar(app, this::proximoApp))));
    }

    private String descrever(AppSuspeito app) {
        StringBuilder texto = new StringBuilder();

        if (!app.getAcessos().isEmpty()) {
            List<String> partes = new ArrayList<>();
            for (Acesso acesso : app.getAcessos()) partes.add(palco.t(textoDo(acesso)));
            texto.append(palco.t(R.string.app_pode, app.getNome(), juntar(partes, palco.t(R.string.conector_e))));
            if (app.isForaDaLoja()) texto.append(' ').append(palco.t(R.string.fora_da_loja));
        } else if (app.isForaDaLoja()) {
            texto.append(palco.t(R.string.so_fora_da_loja, app.getNome()));
        }

        if (app.isAcessoRemoto()) {
            if (texto.length() == 0) {
                texto.append(palco.t(R.string.acesso_remoto_com_nome, app.getNome()));
            } else {
                texto.append(' ').append(palco.t(R.string.acesso_remoto));
            }
        }
        return texto.toString();
    }

    private static int textoDo(Acesso acesso) {
        switch (acesso) {
            case ACESSIBILIDADE:
                return R.string.acesso_acessibilidade;
            case ADMINISTRADOR:
                return R.string.acesso_administrador;
            default:
                return R.string.acesso_notificacoes;
        }
    }

    /** "a", "a e b", "a, b e c". O conector vem de fora porque é texto da interface. */
    static String juntar(List<String> partes, String conector) {
        if (partes.size() == 1) return partes.get(0);
        String inicio = String.join(", ", partes.subList(0, partes.size() - 1));
        return inicio + conector + partes.get(partes.size() - 1);
    }
}
