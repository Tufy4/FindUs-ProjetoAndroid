# Setup

O app não compila sem o `google-services.json`.

1. Firebase Console → Configurações do projeto → app Android `com.example.findus` → baixar `google-services.json`.
2. Colocar em `FindUs/app/google-services.json`.

O arquivo é gitignored. Cada dev usa o seu.

## Chave da Geoapify

A Geoapify (free tier, sem cartão) serve os tiles do mapa e a geração de rotas.

1. Criar uma conta em https://myprojects.geoapify.com e copiar a API key.
2. Adicionar em `FindUs/local.properties`:

```
GEOAPIFY_API_KEY=sua_chave_aqui
```

Sem a chave o mapa vem cinza e a rota cai para uma linha reta entre origem e destino.

Os tiles vêm de `maps.geoapify.com` (estilo `osm-bright`), renderizados pelo osmdroid.
Não usar `basemaps.cartocdn.com` nem `tile.openstreetmap.org`: o primeiro carimba
"API KEY REQUIRED" na imagem e o segundo bloqueia apps osmdroid com um tile de 403.
Os dois respondem HTTP 200, então o erro só aparece na tela.
