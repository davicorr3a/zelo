package io.github.davicorr3a.zelo.analise;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;

import androidx.core.app.NotificationManagerCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.github.davicorr3a.zelo.util.Preferencias;

/**
 * Varre os apps instalados procurando acessos perigosos, apps instalados fora de uma loja
 * conhecida e apps de acesso remoto.
 *
 * Apps do sistema ficam de fora: vieram com o aparelho e listar todos eles só geraria
 * perguntas que a pessoa não sabe responder.
 */
public final class AnalisadorApps {

    private static final Set<String> LOJAS_CONHECIDAS = new HashSet<>(Arrays.asList(
            "com.android.vending",              // Google Play
            "com.sec.android.app.samsungapps",  // Galaxy Store
            "com.huawei.appmarket",
            "com.xiaomi.market",
            "com.xiaomi.mipicks",
            "com.heytap.market",
            "com.oppo.market",
            "com.amazon.venezia",
            "org.fdroid.fdroid"
    ));

    // Apps legítimos, mas que aparecem em quase todo golpe de "suporte técnico" por telefone.
    private static final Set<String> APPS_DE_ACESSO_REMOTO = new HashSet<>(Arrays.asList(
            "com.anydesk.anydeskandroid",
            "com.teamviewer.quicksupport.market",
            "com.teamviewer.host.market",
            "com.carriez.flutter_hbb",          // RustDesk
            "com.sand.airdroid"
    ));

    private final Context contexto;
    private final PackageManager pm;
    private final Preferencias preferencias;

    public AnalisadorApps(Context contexto, Preferencias preferencias) {
        this.contexto = contexto.getApplicationContext();
        this.pm = this.contexto.getPackageManager();
        this.preferencias = preferencias;
    }

    /** Lista os apps que merecem atenção, do mais perigoso para o menos. Pode demorar: rode fora da thread principal. */
    public List<AppSuspeito> analisar() {
        Set<String> acessibilidade = pacotesComAcessibilidade(contexto);
        Set<String> administradores = pacotesAdministradores();
        Set<String> leitores = NotificationManagerCompat.getEnabledListenerPackages(contexto);

        List<AppSuspeito> achados = new ArrayList<>();
        for (ApplicationInfo info : appsInstalados()) {
            if (ehDoSistema(info) || info.packageName.equals(contexto.getPackageName())) continue;
            if (preferencias.confia(info.packageName)) continue;

            AppSuspeito app = montar(info, acessibilidade, administradores, leitores);
            if (app.mereceAtencao()) achados.add(app);
        }

        achados.sort((a, b) -> Integer.compare(b.risco(), a.risco()));
        return achados;
    }

    /** Analisa um app específico, mesmo que ele esteja na lista de confiáveis. Retorna null se ele não estiver instalado. */
    public AppSuspeito analisarPacote(String pacote) {
        ApplicationInfo info = infoDe(pacote);
        if (info == null) return null;
        return montar(info,
                pacotesComAcessibilidade(contexto),
                pacotesAdministradores(),
                NotificationManagerCompat.getEnabledListenerPackages(contexto));
    }

    public boolean estaInstalado(String pacote) {
        return infoDe(pacote) != null;
    }

    private AppSuspeito montar(ApplicationInfo info, Set<String> acessibilidade,
                               Set<String> administradores, Set<String> leitores) {
        String pacote = info.packageName;
        AppSuspeito app = new AppSuspeito(pacote, pm.getApplicationLabel(info).toString());

        if (acessibilidade.contains(pacote)) app.adicionar(AppSuspeito.Acesso.ACESSIBILIDADE);
        if (administradores.contains(pacote)) app.adicionar(AppSuspeito.Acesso.ADMINISTRADOR);
        if (leitores.contains(pacote)) app.adicionar(AppSuspeito.Acesso.LE_NOTIFICACOES);

        app.setForaDaLoja(!ehDoSistema(info) && !LOJAS_CONHECIDAS.contains(instaladorDe(pacote)));
        app.setAcessoRemoto(APPS_DE_ACESSO_REMOTO.contains(pacote));
        return app;
    }

    private Set<String> pacotesAdministradores() {
        Set<String> pacotes = new HashSet<>();
        DevicePolicyManager dpm = contexto.getSystemService(DevicePolicyManager.class);
        if (dpm == null) return pacotes;

        List<ComponentName> admins = dpm.getActiveAdmins();
        if (admins != null) {
            for (ComponentName admin : admins) pacotes.add(admin.getPackageName());
        }
        return pacotes;
    }

    @SuppressWarnings("deprecation")
    private List<ApplicationInfo> appsInstalados() {
        return pm.getInstalledApplications(0);
    }

    @SuppressWarnings("deprecation")
    private ApplicationInfo infoDe(String pacote) {
        try {
            return pm.getApplicationInfo(pacote, 0);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    @SuppressWarnings("deprecation")
    private String instaladorDe(String pacote) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                return pm.getInstallSourceInfo(pacote).getInstallingPackageName();
            }
            return pm.getInstallerPackageName(pacote);
        } catch (PackageManager.NameNotFoundException | IllegalArgumentException e) {
            return null;
        }
    }

    // ---- Funções usadas também pelo guardião ----

    public static Set<String> pacotesComAcessibilidade(Context contexto) {
        return pacotesDaConfiguracao(contexto, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
    }

    public static boolean ehAcessoRemoto(String pacote) {
        return APPS_DE_ACESSO_REMOTO.contains(pacote);
    }

    @SuppressWarnings("deprecation")
    public static boolean ehDoSistema(Context contexto, String pacote) {
        try {
            return ehDoSistema(contexto.getPackageManager().getApplicationInfo(pacote, 0));
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    @SuppressWarnings("deprecation")
    public static String nomeDoApp(Context contexto, String pacote) {
        PackageManager pm = contexto.getPackageManager();
        try {
            return pm.getApplicationLabel(pm.getApplicationInfo(pacote, 0)).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return pacote;
        }
    }

    private static boolean ehDoSistema(ApplicationInfo info) {
        return (info.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
    }

    /** Algumas configurações guardam uma lista de componentes no formato "pacote/Classe:pacote/Classe". */
    private static Set<String> pacotesDaConfiguracao(Context contexto, String chave) {
        Set<String> pacotes = new HashSet<>();
        String valor;
        try {
            valor = Settings.Secure.getString(contexto.getContentResolver(), chave);
        } catch (SecurityException e) {
            return pacotes;
        }
        if (valor == null || valor.isEmpty()) return pacotes;

        for (String item : valor.split(":")) {
            ComponentName componente = ComponentName.unflattenFromString(item);
            if (componente != null) pacotes.add(componente.getPackageName());
        }
        return pacotes;
    }
}
