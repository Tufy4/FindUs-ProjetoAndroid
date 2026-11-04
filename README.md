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
- Cadastro de centros de distribuição (origem das entregas).
- Criação de entregas e atribuição a um motorista (e ao veículo vinculado a ele).
- Cadastro de perímetros de geofencing e de grupos de veículos.

### Motorista
- Captura de foto do veículo.
- Cadastro de dados do veículo.
- Lista das entregas atribuídas, com as ações de iniciar e concluir.

### Motorista e Controlador
- Geração de rota da localização atual até um ponto no mapa (veículo da frota ou destino de entrega).

## Fluxo de entrega

1. O controlador cadastra um **centro de distribuição** e um **motorista** com veículo vinculado.
2. Em **Entregas**, o controlador cria a entrega: descrição, centro de origem, motorista e ponto de entrega escolhido no mapa. O veículo da entrega é o veículo do motorista.
3. O motorista vê a entrega em **Minhas entregas**, toca em **Iniciar** e pode abrir a **rota até o destino**.
4. No **Mapa e telemetria da frota**, ao iniciar a simulação de um veículo com entrega, o trajeto vai do centro de distribuição até o ponto de entrega. Se a entrega ainda estava pendente, ela passa para "em rota".
5. O geofencing avisa quando o veículo sai do centro de distribuição e quando chega ao ponto de entrega.
6. O motorista toca em **Concluir** e o veículo volta a ficar disponível.

Status da entrega: `PENDENTE` → `EM_ROTA` → `ENTREGUE` (ou `CANCELADA` pelo controlador).

## Geofencing

São monitorados automaticamente:
- cada **centro de distribuição**, com o raio definido no cadastro, para todos os veículos;
- cada **ponto de entrega** pendente ou em rota, com raio de 200 m, só para o veículo daquela entrega;
- cada **perímetro** cadastrado e ativo, para todos os veículos, para um veículo específico ou para um grupo.

Os perímetros aparecem como círculos no mapa do controlador (azul: centros, laranja: pontos de entrega, roxo: perímetros cadastrados). O aviso aparece no rodapé do mapa e como notificação do sistema, com a placa do veículo e o nome do perímetro.

## Regras de negócio

As regras ficam em `app/.../domain/RegrasNegocio.kt`:
- Um veículo só pode estar vinculado a um motorista por vez.
- O motorista vê apenas o veículo vinculado a ele. Ao cadastrar um veículo, ele passa a ser o veículo desse motorista.
- Só é possível atribuir entrega a motorista com veículo, e o veículo não pode estar em manutenção ou inativo.
- Só entregas pendentes podem ser editadas. Entregas pendentes ou em rota podem ser canceladas.
- O motorista não pode iniciar uma entrega enquanto tiver outra em rota.
- Um centro de distribuição com entregas pendentes ou em rota não pode ser removido.
- Um grupo usado por um perímetro não pode ser removido.

## Tecnologias

| Camada | Tecnologia |
|---|---|
| Plataforma | Android (Kotlin) |
| Autenticação | Firebase Auth |
| Banco em nuvem | Firebase Firestore |
| Armazenamento de fotos (nuvem) | Firestore (imagem em Base64) |
| Persistência local | SQLite / Room |
| Armazenamento local de fotos | Filesystem do dispositivo |
| Mapas | osmdroid + tiles Geoapify (OpenStreetMap) |
| Geocoding | Android Geocoder (reverse) · Geoapify (rotas) |
| Geofencing | Cálculo de distância + notificação do sistema |

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
