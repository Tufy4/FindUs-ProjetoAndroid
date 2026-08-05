# Rastreamento de Veículos de Entrega

Projeto da disciplina de Projeto Integrador — Tecnologia em Sistemas para Internet (IFSP).

**Opção escolhida: #1 — Rastreamento de veículos de entrega**

## Descrição

Aplicativo Android para monitoramento de uma frota de veículos de entrega (caminhões, furgões etc.) e apoio ao trabalho de entregas, com dois perfis de usuário: **Motorista** e **Controlador**.

## Perfis e funcionalidades

### Controlador
- Localização dos veículos no mapa, com endereço obtido por reverse geocoding.
- Notificação de entrada/saída de perímetro (geofencing) para um veículo específico ou grupo.
- Visualização de dados de telemetria simulados: velocidade, estado das portas, estado do motor.

### Motorista
- Captura de foto do veículo.
- Cadastro de dados do veículo.

### Motorista e Controlador
- Geração de rota da localização atual até um ponto no mapa (veículo da frota ou destino de entrega).

## Tecnologias

| Camada | Tecnologia |
|---|---|
| Plataforma | Android (Kotlin) |
| Autenticação | Firebase Auth |
| Banco em nuvem | Firebase Firestore |
| Armazenamento de fotos (nuvem) | Firebase Storage |
| Persistência local | SQLite / Room |
| Armazenamento local de fotos | Filesystem do dispositivo |
| Mapas | Google Maps *(ou OpenStreetMap — definir)* |
| Geocoding | Google Geocoding API *(ou Nominatim — conforme escolha de mapa)* |
| Geofencing | Android Geofencing API |

## Arquitetura de dados

O aplicativo salva os dados inicialmente no dispositivo (SQLite via Room para dados estruturados; filesystem local para fotos) e sincroniza de forma assíncrona com o Firebase (Firestore / Storage) quando há conectividade.

## Simulação de monitoramento

A movimentação dos veículos e a geração de telemetria são simuladas a partir de uma sequência pré-determinada de posições geográficas, cada uma associada a um conjunto de dados de telemetria.

## Equipe

| Nome | Função/GitHub |
|---|---|
| Tufy Elias | Tufy4 |
| Miguel Henrique | Miguel-Henri |
| Pedro Aguiar | duxpe’s |
| Natan Araujo | natanaraujo3001 |

## Estrutura do repositório

```
/
├── app/                # código-fonte do aplicativo Android
├── docs/                # documentação (relatórios, PoCs, apresentações)
├── pocs/                 # provas de conceito desenvolvidas ao longo do projeto
└── README.md
```

## Cronograma de entregas

| Marco | Data | Entrega |
|---|---|---|
| Semana 3 | 12/08/2026 | Entrega 1 — Login (Firebase Auth) |
| Semana 5 | 26/08/2026 | Entrega 2 — Cadastro de veículos/motoristas + Room |
| Semana 7 | 09/09/2026 | Entrega 3 — Sincronização Room ↔ Firestore |
| Semana 8 | 16/09/2026 | Apresentação parcial |
| Semana 9 | 23/09/2026 | Entrega 4 — Fotos (local + Firebase Storage) |
| Semana 11 | 29/09/2026 | Entrega 5 — Mapa e telemetria simulada |
| Semana 13 | 21/10/2026 | Entrega 6 — Geração de rotas |
| Semana 15 | 04/11/2026 | Entrega 7 — Reverse geocoding |
| Semana 17 | 18/11/2026 | Entrega 8 — Geofencing (code freeze) |
| Semana 18 | 25/11/2026 | Correções solicitadas pelo UAT |
| Semana 19 | 02/12/2026 | Entrega 9 — Correções finais e apresentação final |

## Processo de trabalho

Cada funcionalidade segue o fluxo: **1) Pesquisa → 2) Prova de conceito (PoC) → 3) Integração ao projeto principal.**
As PoCs desenvolvidas em cada iteração ficam disponíveis na pasta `poc/`.
