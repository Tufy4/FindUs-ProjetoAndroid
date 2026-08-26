# PoC E3: sincronização SQLite/Room ↔ Firebase/Firestore

Prova de conceito da **Entrega 3** do projeto FindUs. Demonstra a arquitetura *offline-first* descrita no README principal: o app grava primeiro no dispositivo e sincroniza com a nuvem de forma assíncrona.

## O que a PoC prova

1. O usuário consegue cadastrar, editar e excluir veículos **sem rede**, e a tela responde na hora.
2. As alterações feitas offline entram numa fila e sobem sozinhas quando a conexão volta.
3. Alterações feitas no Firestore (por outro celular ou pelo console) descem para o SQLite e aparecem na tela.
4. Quando o mesmo registro é alterado dos dois lados, existe uma regra determinística de desempate.

## Como o dado circula

```
                escrita                          leitura
   UI  ──────────────────►  Room (SQLite)  ◄────────────────  UI
                              │      ▲
             pendenteSync=1   │      │  merge por atualizadoEm
                              ▼      │
                          SyncRepository
                              │      ▲
                       set()  │      │  addSnapshotListener
                              ▼      │
                            Firestore
```

O Room é a única fonte de verdade da interface. A tela nunca lê do Firestore diretamente, então ela funciona igual com ou sem rede.

## Decisões de projeto

**Id gerado no cliente (UUID).** Não dá para usar autoincremento: o registro precisa nascer offline já com a identidade definitiva, porque esse mesmo id vira o id do documento no Firestore. Isso também torna o reenvio idempotente, já que reenviar o mesmo registro só sobrescreve o documento com o mesmo conteúdo.

**Flag `pendenteSync` como fila de envio.** Toda escrita local marca a linha como pendente. A flag só cai quando o servidor confirma a gravação. Se o app fechar no meio, a fila continua no banco.

**Exclusão lógica (`deletado`).** Um DELETE físico sumiria da tabela e não teria mais como ser propagado. A linha permanece, some da listagem e viaja até o Firestore como qualquer outra alteração.

**Last write wins por `atualizadoEm`.** Cada alteração carimba o horário. Quando um documento chega do servidor, ele só sobrescreve a linha local se for mais novo. Se a linha local está pendente e é igual ou mais nova, ela vence, porque a edição do usuário ainda não chegou ao servidor e sobrescrever apagaria algo que ele acabou de digitar.

**Timeout no envio.** A `Task` do `set()` do Firestore só completa quando o servidor confirma. Sem rede ela fica pendente em vez de falhar, o que travaria o laço de sincronização. Por isso o envio é envolvido em `withTimeout(5s)` e o estouro é tratado como "continua na fila".

## Como rodar

1. Abrir a pasta `pocs/e3-sync-room-firestore` no Android Studio como projeto próprio.
2. Copiar o `google-services.json` do app principal para `app/`. É o mesmo projeto do Firebase usado no login da Entrega 1, então não precisa criar outro.
3. No console do Firebase, criar o banco Firestore em modo de teste (ou aplicar as regras abaixo).
4. Rodar no emulador ou no aparelho.

Regras mínimas para a demonstração, com a coleção liberada só para usuário autenticado:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /veiculos/{id} {
      allow read, write: if request.auth != null;
    }
  }
}
```

Como esta PoC não faz login, use modo de teste (`if true`) enquanto estiver isolada. Ao integrar no app principal, volte para a regra acima.

## Roteiro de demonstração

1. Cadastre dois veículos com a chave "Simular offline" desligada. Eles aparecem sem etiqueta e já surgem no console do Firebase.
2. Ligue "Simular offline" e cadastre um terceiro. Ele aparece na lista com a etiqueta **pendente**, e o contador "Aguardando envio" sobe.
3. Feche e reabra o app ainda offline. O registro continua lá, provando que a fila está no SQLite e não em memória.
4. Desligue "Simular offline". O envio acontece, a etiqueta some e o documento surge no console.
5. Edite um campo direto no console do Firebase. A alteração desce e a lista se atualiza sem nenhuma ação do usuário.
6. Exclua um veículo pelo app e confirme no console que o documento ficou com `deletado: true`.

O botão "Simular offline" usa `disableNetwork()` / `enableNetwork()` do Firestore em vez do modo avião, o que deixa a demonstração reprodutível sem mexer nas configurações do aparelho.

## Limitações conhecidas

- A sincronização é disparada manualmente ou logo após cada gravação. Na integração ao app principal isso deve virar um `WorkManager` com constraint de rede, para reagir sozinho à volta da conexão e sobreviver ao app fechado.
- O `atualizadoEm` usa o relógio do dispositivo. Com vários motoristas, um celular com a hora errada pode ganhar um desempate que não deveria. A alternativa é `FieldValue.serverTimestamp()` no Firestore, ao custo de complicar o merge, já que o valor só é conhecido depois do commit.
- Exclusões feitas direto no console (delete físico do documento) não são detectadas, porque a PoC compara documento a documento e não trata desaparecimento. A exclusão suportada é a lógica.
- Sem paginação: o listener traz a coleção inteira a cada mudança. Suficiente para uma frota de demonstração, não para escala.
- Um único usuário. Ao integrar, a coleção deve ser particionada por empresa ou por controlador, com as regras do Firestore refletindo isso.

## Arquivos relevantes

| Arquivo | Papel |
|---|---|
| `data/local/VeiculoEntity.kt` | Entidade Room, incluindo os campos de controle de sync |
| `data/local/VeiculoDao.kt` | Consultas reativas e a fila de pendentes |
| `data/local/PocDatabase.kt` | Configuração do banco |
| `data/remote/VeiculoRemoteDataSource.kt` | Envio e listener do Firestore |
| `data/SyncRepository.kt` | Regras de push, pull e resolução de conflito |
| `ui/SyncViewModel.kt` | Estado da tela e disparo da sincronização |
| `ui/SyncScreen.kt` | Interface de demonstração |

## Problemas comuns

- **`Default FirebaseApp is not initialized`**: falta o `google-services.json` em `app/`.
- **`SDK location not found`**: acontece ao rodar `./gradlew` direto no terminal. Abra pelo Android Studio, que gera o `local.properties`, ou exporte `ANDROID_HOME`.

## Por que kapt e não KSP

O Room aceita os dois. A PoC usa kapt porque o projeto está na AGP 8.8.1, e o KSP 2.3+ chama APIs de variante que só existem em versões mais novas da AGP. A tentativa de usar KSP falha na sincronização com:

```
Unable to find method 'void com.android.build.api.variant.
AndroidComponentsExtension.addKspConfigurations(boolean)'
```

Descer a versão do KSP não resolve, porque as versões anteriores à 2.3.0 são amarradas à versão do Kotlin e nenhuma delas aceita Kotlin 2.4.10.

Quando o time subir a AGP, a volta para KSP são três linhas. Em `gradle/libs.versions.toml`, trocar o plugin `kotlin-kapt` por `ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }` com a versão correspondente ao Kotlin em uso; em `app/build.gradle.kts`, trocar o alias do plugin e a linha `kapt(libs.androidx.room.compiler)` por `ksp(...)`. Nesse cenário o Room também pode subir para a 2.8.x. O código Kotlin não muda em nada.
