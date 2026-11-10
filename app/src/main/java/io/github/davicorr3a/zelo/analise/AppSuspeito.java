package io.github.davicorr3a.zelo.analise;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** Um app que merece atenção, com os motivos que levaram a isso. */
public final class AppSuspeito {

    public enum Acesso {
        /** Pode ler e controlar tudo o que aparece na tela. É o acesso usado no golpe da mão fantasma. */
        ACESSIBILIDADE,
        /** Pode bloquear o aparelho e impedir a própria desinstalação. */
        ADMINISTRADOR,
        /** Pode ler todas as notificações, inclusive códigos de confirmação. */
        LE_NOTIFICACOES
    }

    private final String pacote;
    private final String nome;
    private final EnumSet<Acesso> acessos = EnumSet.noneOf(Acesso.class);
    private boolean foraDaLoja;
    private boolean acessoRemoto;

    AppSuspeito(String pacote, String nome) {
        this.pacote = pacote;
        this.nome = nome;
    }

    void adicionar(Acesso acesso) {
        acessos.add(acesso);
    }

    void setForaDaLoja(boolean foraDaLoja) {
        this.foraDaLoja = foraDaLoja;
    }

    void setAcessoRemoto(boolean acessoRemoto) {
        this.acessoRemoto = acessoRemoto;
    }

    public String getPacote() {
        return pacote;
    }

    public String getNome() {
        return nome;
    }

    public Set<Acesso> getAcessos() {
        return Collections.unmodifiableSet(acessos);
    }

    public boolean tem(Acesso acesso) {
        return acessos.contains(acesso);
    }

    public boolean isForaDaLoja() {
        return foraDaLoja;
    }

    public boolean isAcessoRemoto() {
        return acessoRemoto;
    }

    public boolean mereceAtencao() {
        return !acessos.isEmpty() || foraDaLoja || acessoRemoto;
    }

    /** Pontuação usada só para ordenar a lista: o mais perigoso aparece primeiro. */
    int risco() {
        int risco = 0;
        if (tem(Acesso.ACESSIBILIDADE)) risco += 4;
        if (tem(Acesso.ADMINISTRADOR)) risco += 3;
        if (tem(Acesso.LE_NOTIFICACOES)) risco += 2;
        if (acessoRemoto) risco += 3;
        if (foraDaLoja) risco += 2;
        return risco;
    }
}
