<p align="center">
  <img src="docs/marca/zelo-simbolo.svg" alt="Símbolo do Zelo" width="112">
</p>

# Zelo

App Android que ajuda a cuidar do celular de quem tem pouca intimidade com tecnologia, principalmente pais e avós.

O Zelo conversa com a pessoa usando frases curtas e botões grandes, mostra o que tem de perigoso no aparelho e ajuda a resolver o problema de forma prática. Ele funciona totalmente offline: o app não tem permissão de internet, então nada do que ele vê sai do celular.

> **Status:** em desenvolvimento (0.1). Ainda não tem release.

## Por que eu comecei

Um dia minha mãe me mostrou o celular dela. Tinha mais de mil notificações da Shopee, site mandando aviso de "vírus detectado" pelo navegador, propaganda abrindo em tela cheia, app com acesso que não devia ter e configuração de segurança do jeito que veio de fábrica. Ela não fazia ideia de nada disso, e não é culpa dela: o Android esconde essas coisas em menus que nem quem trabalha com tecnologia acha com facilidade.

Ao mesmo tempo, a normalização de toda essa bagunça propricia um cenário favorável ao descontrole, golpes, e execução de processos indevidos e, em alguns casos, maliciosos.

Já existem ferramentas ótimas para auditar permissões, mas elas foram feitas para quem tem muita familiaridade com tecnologia. E Eu queria algo que minha mãe conseguisse usar sozinha e que eu conseguisse configurar de forma rápida nas vezes em que ela me pede para ver algo no celular dela.

## Como funciona

São dois momentos.

**Na visita.** Alguém da família abre o Zelo junto com a pessoa, e ele faz uma revisão guiada:

- mostra apps com acesso à acessibilidade, à administração do dispositivo ou à leitura de notificações;
- aponta apps instalados fora de uma loja oficial e apps de acesso remoto;
- pergunta, app por app, se a pessoa reconhece. Se não reconhece, guia até desligar o acesso e desinstalar;
- conta as notificações acumuladas, limpa tudo com um toque e ajuda a silenciar o app que mais incomoda.

**No dia a dia.** Depois que o acesso às notificações é liberado, o Zelo fica de guarda:

- quando um app novo é instalado, ele avisa e pergunta se alguém pediu para instalar;
- quando algum app ganha acesso à acessibilidade, ele avisa na hora.

Tudo acontece em forma de conversa. Não tem painel, gráfico nem termo técnico.

## Pensado para quem enxerga pouco

- **Fonte Atkinson Hyperlegible**, criada pelo Braille Institute para pessoas com baixa visão. Ela diferencia bem letras que costumam confundir, como I, l e 1.
- **Contraste alto.** Todo texto passa no nível AAA da WCAG (7:1 ou mais), no modo claro e no escuro.
- **Paleta fixa.** O app não usa as cores dinâmicas do Material You, que mudam conforme o papel de parede e podem deixar o texto ilegível.
- **Botões grandes**, com no mínimo 64dp de altura e sempre com texto, nunca só ícone.
- **Respeita o tamanho de fonte do celular.** Se a pessoa aumentou a letra nas configurações, o Zelo acompanha.

## O que ele não faz

- **Não vigia.** Não monitora o que a pessoa assiste, com quem conversa ou por onde anda. É um app de cuidado e organização.
- **Não se esconde.** A pessoa vê tudo o que o Zelo vê.
- **Não guarda o que chega nas notificações.** Para saber quem mais incomoda, ele anota só quantas notificações cada app mandou nos últimos sete dias. Título e texto nunca são salvos.
- **Não manda nada para lugar nenhum.** Não existe servidor.
- **Não decide sozinho.** O Android não deixa um app comum revogar permissões de outros apps, e isso é bom. O Zelo leva a pessoa até a tela certa e ela decide.

## Permissões

| Permissão | Para quê |
|---|---|
| `QUERY_ALL_PACKAGES` | Listar os apps instalados e conferir de onde cada um veio |
| Acesso às notificações | Contar e limpar notificações e manter o guardião ativo |
| `POST_NOTIFICATIONS` | Mostrar os avisos do guardião |
| `REQUEST_DELETE_PACKAGES` | Abrir a confirmação de desinstalação |

A permissão de internet é removida explicitamente no manifesto (`tools:node="remove"`). Se alguma biblioteca tentar adicioná-la no futuro, ela não entra.

Se você inspecionar o APK, vai ver também a `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`. Ela é criada pela biblioteca `androidx.core` para proteger o receptor que o guardião registra, e só o próprio Zelo pode usar. Não dá acesso a nada fora do app.

Duas observações práticas:

- Por causa do `QUERY_ALL_PACKAGES`, o Zelo dificilmente passaria na revisão da Play Store. A distribuição vai ser pelo F-Droid e pelas releases aqui do GitHub.
- No Android 13 ou mais novo, um APK instalado pelo navegador ou pelo gerenciador de arquivos fica com o acesso às notificações bloqueado até a pessoa liberar as "configurações restritas" nas informações do app. O próprio Zelo explica esse passo quando acontece.

## Compilando

Você vai precisar do Android Studio (ou só do Android SDK) e do JDK 17 ou mais novo.

```bash
git clone https://github.com/davicorr3a/zelo.git
cd zelo
./gradlew assembleDebug
```

O APK fica em `app/build/outputs/apk/debug/`.

## Tecnologias

Java 17, Views em XML e Material 3. Como dependências, só bibliotecas Jetpack e Material, todas do próprio Google e de código aberto:

- `core`, `appcompat` e `activity`, para compatibilidade entre versões do Android e o layout de ponta a ponta;
- `recyclerview`, para a lista da conversa;
- `lifecycle` (ViewModel e LiveData), para a conversa não se perder quando a tela gira;
- `material`, para os componentes e o tema.

A regra é simples: nenhuma biblioteca que acesse a rede, nada de analytics e nada de SDK de terceiros. A cada dependência nova, o manifesto final é conferido para garantir que nenhuma permissão entrou junto. As versões ficam em `gradle/libs.versions.toml`.

## Estrutura

```
app/src/main/java/io/github/davicorr3a/zelo/
├── analise/     varredura dos apps instalados (acessos, origem, acesso remoto)
├── conversa/    o roteiro da conversa e os modelos de mensagem
├── guardiao/    serviço que fica de olho em instalações e na acessibilidade
├── ui/          tela, ViewModel e adapter da conversa
└── util/        atalhos para as telas de configuração e preferências locais
```

O ponto de partida para entender o app é o `Roteiro.java`. Cada método é um momento da conversa, e dá para seguir o fluxo inteiro lendo de cima para baixo. O `ConversaViewModel` guarda a conversa e a activity só desenha o que ele manda.

## Próximos passos

- [ ] Contar as notificações ao longo da semana, e não só as que estão na bandeja
- [ ] Achar sites que mandam notificação pelo navegador e ajudar a bloquear
- [ ] Silenciar só as promoções de um app e manter os avisos úteis, como o de entrega
- [ ] Apps esquecidos, sem uso há meses, com sugestão de desinstalar
- [ ] Apps que abrem por cima dos outros (propaganda em tela cheia, tela falsa de banco)
- [ ] Aviso por SMS para um contato de confiança quando algo grave acontecer
- [ ] Checklist guiado de contas: verificação em duas etapas do WhatsApp, Verificador de Senhas do Google, PIN do chip
- [ ] "Isso é golpe?": compartilhar uma mensagem com o Zelo e ele fazer as perguntas certas
- [ ] Leitura das mensagens em voz alta
- [ ] Testes em aparelhos Samsung, Motorola e Xiaomi, que mudam bastante as telas de configuração

## Contribuindo

Issues e pull requests são bem-vindos.

Se você tem pais ou avós e já passou por alguma situação com o celular deles, abrir uma issue contando o caso também ajuda muito. Boa parte do que vai entrar no Zelo vem dessas histórias.

## Licença

O código é MIT. Veja o arquivo [LICENSE](LICENSE).

A fonte Atkinson Hyperlegible é do Braille Institute of America e é distribuída sob a SIL Open Font License 1.1. A licença completa está em [licencas/AtkinsonHyperlegible-OFL.txt](licencas/AtkinsonHyperlegible-OFL.txt).
