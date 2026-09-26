# PoC E7: reverse geocoding (coordenada para endereço)

Prova de conceito da **Entrega 7** do projeto FindUs. Converte uma coordenada (um toque no mapa ou a posição do GPS) em um endereço legível, usando a Reverse Geocoding API da Geoapify, com fallback para o Geocoder nativo do Android.

## O que a PoC prova

1. Dá para obter um endereço estruturado (rua, número, bairro, cidade, UF e CEP) a partir de latitude e longitude, usando a mesma chave da Geoapify que o app já usa para os tiles e as rotas.
2. Quando a API não responde (sem chave, sem rede, erro HTTP ou ponto sem resultado), o app cai para o Geocoder do Android sem quebrar a tela.
3. Consultas repetidas no mesmo ponto não gastam cota da API, porque ficam em cache.
4. A interface trata os estados de carregando, encontrado e indisponível, sem ficar presa em "Buscando endereço…" quando a consulta falha.

## Como a consulta flui

```
   toque no mapa / GPS
            │
            ▼
     ReverseGeocoder ──► cache (lat/lon com 4 casas, ~11 m) ──► achou: devolve na hora
            │
            ▼ não achou
  GeoapifyReverseGeocoder   HTTP, timeout de 5 s
            │
            ▼ null (sem chave, sem rede, erro ou sem resultado)
   AndroidReverseGeocoder   Geocoder nativo
            │
            ▼
        Endereco? ──► EstadoEndereco (Carregando / Encontrado / Indisponivel) ──► tela
```

## Decisões de projeto

**Geoapify como provedor principal.** Ela já fornece os tiles do mapa e as rotas do projeto, então não é preciso criar conta nem chave nova. A chamada usa `lang=pt` e `format=json`, que devolve um JSON achatado, mais simples de ler que o GeoJSON padrão.

**Geocoder do Android como reserva, e não como principal.** Ele não precisa de chave, mas depende do backend do Google Play Services. Em emuladores sem Google Play ele pode nem existir (`Geocoder.isPresent()` devolve false), e a qualidade do resultado varia por aparelho. Além disso, a versão síncrona de `getFromLocation` está depreciada desde o API 33. A PoC usa o `GeocodeListener` assíncrono no API 33+ e a chamada antiga abaixo disso, já que o minSdk é 29.

**Endereço estruturado em vez de uma string.** A classe `Endereco` separa logradouro, número, bairro, cidade, UF e CEP e registra qual provedor respondeu. Isso permite mostrar "Rua, número - Bairro" em listas, o endereço completo em detalhes e preencher campos de formulário.

**Cache por coordenada arredondada.** Com 4 casas decimais, pontos a menos de ~11 m um do outro reaproveitam o mesmo resultado. É um LRU em memória com 200 entradas. Falhas não entram no cache, para que uma nova tentativa funcione quando a rede voltar.

**Interface `ProvedorEndereco`.** Os dois provedores implementam o mesmo contrato. A ordem de fallback fica configurável, e a lógica pode ser testada com provedores falsos, sem rede.

**Parser separado da chamada HTTP.** `parseRespostaGeoapify` é uma função pura, coberta por teste unitário com um JSON de exemplo.

## Como rodar

1. Abrir a pasta `pocs/e7-reverse_geocoding` no Android Studio como projeto próprio.
2. No `local.properties` (o Studio cria esse arquivo ao abrir o projeto), adicionar a mesma chave usada no app principal:

   ```
   GEOAPIFY_API_KEY=sua_chave_aqui
   ```

3. Rodar no emulador ou no aparelho. No emulador, defina uma localização em *Extended controls > Location* antes de testar o botão "Minha localização".

Não é preciso `google-services.json`, porque a PoC não usa Firebase.

O toolchain é o mesmo da PoC E3 (AGP 8.8.1, Gradle 8.10.2, Kotlin 2.1.10), então o projeto abre nas máquinas do laboratório sem atualizar o Android Studio.

## Roteiro de demonstração

1. Toque em um ponto do mapa. O endereço aparece com "Fonte: API Geoapify" e o tempo da consulta.
2. Toque em "Consultar de novo". O mesmo endereço volta marcado como "do cache", em poucos milissegundos.
3. Toque em "Minha localização". O app pede a permissão, centraliza o mapa na posição do GPS e mostra o endereço.
4. Ligue "Forçar Geocoder do Android" e toque em outro ponto. A fonte muda para "Geocoder do Android", simulando a API fora do ar sem mexer na chave. Em emulador sem Google Play aparece "Endereço não encontrado para este ponto", que é o comportamento esperado quando a reserva também não está disponível.
5. Toque no meio de uma represa ou de uma área sem endereço. A tela mostra que não encontrou, em vez de ficar carregando para sempre.
6. Compare as consultas no histórico, que mostra a fonte, o tempo e se a resposta veio do cache.

## Testes unitários

Na pasta da PoC:

```
./gradlew testDebugUnitTest
```

Ou, no Studio, clique com o botão direito em `app/src/test` e escolha *Run*. Os testes cobrem o parser da resposta da Geoapify, o fallback entre os provedores e o cache. O relatório fica em `app/build/reports/tests/testDebugUnitTest/index.html`.

O `org.json` do `android.jar` é só um stub nos testes que rodam na JVM, por isso o `build.gradle.kts` adiciona `org.json:json` como dependência de teste.

## Integração no app principal

Na branch `feature/reverse-geocoding`, o mesmo código da pasta `location/` foi levado para `FindUs/app/src/main/java/com/example/findus/location/` e ligado em três telas:

- **Painel do controlador:** ao tocar no marcador de um veículo, o diálogo mostra o endereço onde ele está.
- **Rota até a entrega (motorista):** o ponto tocado no mapa mostra o endereço no rodapé enquanto a rota é calculada.
- **Cadastro de negociante:** o botão "Usar minha localização atual" preenche o campo Endereço.

A única diferença entre os dois códigos é o método `emCache()`, que existe só na PoC para a tela mostrar se a resposta veio do cache.

## Limitações conhecidas

- O cache é só em memória e se perde ao fechar o app. Se a frota consultar muito os mesmos lugares, o próximo passo seria persistir os resultados no Room.
- O plano gratuito da Geoapify tem cota diária de requisições. Por isso as telas do app consultam o endereço sob demanda (quando o usuário toca), e não a cada ponto de telemetria recebido.
- O reverse geocoding devolve o endereço mais próximo do ponto. Em rodovias e áreas rurais pode vir só cidade e UF, sem rua ou número.
- A chave vai embutida no APK pelo `BuildConfig`. Para produção, o ideal é restringir a chave no painel da Geoapify ou fazer as chamadas por um backend.

## Arquivos relevantes

| Arquivo | Papel |
|---|---|
| `location/Endereco.kt` | Modelo do endereço e contrato `ProvedorEndereco` |
| `location/GeoapifyReverseGeocoder.kt` | Chamada HTTP e parser da resposta da Geoapify |
| `location/AndroidReverseGeocoder.kt` | Provedor de reserva com o Geocoder nativo |
| `location/ReverseGeocoder.kt` | Fallback entre os provedores e cache |
| `location/LocalizacaoAtual.kt` | Posição atual via FusedLocationProvider |
| `ui/EstadoEndereco.kt` | Estados da consulta e componente que exibe o endereço |
| `ui/MapaSelecao.kt` | Mapa osmdroid com tiles da Geoapify e seleção por toque |
| `ui/ReverseGeocodingViewModel.kt` | Consulta, medição de tempo, cache e histórico |
| `ui/ReverseGeocodingScreen.kt` | Tela de demonstração |

## Problemas comuns

- **Mapa cinza:** falta a `GEOAPIFY_API_KEY` no `local.properties`. Depois de adicionar, faça *Sync* e rode de novo, porque o `BuildConfig` só é gerado na build.
- **Fonte sempre "Geocoder do Android", mesmo com a chave:** a chave está errada (a API responde 401) ou o emulador está sem internet.
- **"Não foi possível obter sua localização":** a permissão foi negada ou o emulador está sem localização definida.
- **`SDK location not found` ao rodar `./gradlew` no terminal:** abra o projeto uma vez pelo Android Studio, que gera o `local.properties`, ou exporte `ANDROID_HOME`.
