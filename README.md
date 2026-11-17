# Sistema de Gestão de Locadora de Veículos
### Trabalho Prático – 3° Trimestre | API RESTful com Spring Boot

---

## Integrantes do Grupo
* Davi Oliz
* Douglas Picoli
* Gabriel Lima

---

## 1. Descrição do Tema Escolhido

O projeto consiste em uma **API RESTful** desenvolvida para o gerenciamento operacional de uma **Locadora de Veículos**. O sistema gerencia a frota de automóveis disponíveis, a base de clientes cadastrados e a entidade central transacional de **Locações**, que controla todo o ciclo de vida do aluguel, desde a reserva e entrega até a devolução do veículo com eventual apuração de multas e penalidades.

### Estrutura de Entidades (Mínimo 3 Entidades)

```
+------------------------+             1:N             +-------------------------------+             N:1             +-------------------------+
|        VEICULO         | <-------------------------> |            LOCACAO            | <-------------------------> |         CLIENTE         |
|  (Entidade Base 1)     |                             |      (Entidade Central)       |                             |   (Entidade Base 2)     |
+------------------------+                             +-------------------------------+                             +-------------------------+
| - id: Long             |                             | - id: Long                    |                             | - id: Long              |
| - modelo: String       |                             | - veiculo: Veiculo (@ManyToOne)                             | - nome: String          |
| - placa: String (UNQ)  |                             | - cliente: Cliente (@ManyToOne)                             | - cpf: String (UNQ)     |
| - categoria: String    |                             | - dataInicio: LocalDate       |                             | - email: String (UNQ)   |
| - valorDiaria: BigDec. |                             | - dataPrevistaDevolucao: Date |                             | - dataCadastro: Date    |
+------------------------+                             | - dataRealDevolucao: Date     |                             | - bloqueadoAte: Date    |
                                                       | - valorTotal: BigDecimal      |                             +-------------------------+
                                                       | - status: StatusLocacao (Enum)|
                                                       +-------------------------------+
```

1. **`Veiculo` (Entidade Base 1):** Automóveis da frota da locadora. Possui validação de placa única (`unique = true`), modelo, categoria e valor da diária.
2. **`Cliente` (Entidade Base 2):** Usuários/condutores que alugam os veículos. Possui CPF e e-mail únicos, data de cadastro gerada automaticamente no momento do registro e campo `bloqueadoAte` para controle de suspensão de novas locações.
3. **`Locacao` (Entidade Central / Transacional):** Registra a transação associando um `Veiculo` e um `Cliente`, com datas de início e previsão de entrega, data real de devolução, cálculo de valor total e controle de status (`ATIVA`, `FINALIZADA`, `CANCELADA`).

---

## 2. Regras de Negócio (Camada @Service)

Todas as regras foram implementadas e validadas **exclusivamente na camada de serviço (`LocacaoService`, `VeiculoService`, `ClienteService`)**, garantindo o isolamento da lógica de domínio:

* **RN1 – Disponibilidade / Status do Veículo:**
  Um veículo só pode ser alugado se **não** possuir nenhuma locação com status `ATIVA`. Caso o veículo informado já esteja em uso em outro contrato aberto, o sistema impede a transação e lança uma exceção de negócio com mensagem clara e HTTP Status `400 Bad Request`.

* **RN2 – Limite de Locações Ativas e Bloqueio de Cliente:**
  - **Limite Quantitativo:** Um mesmo cliente pode ter no máximo **2 locações ativas simultaneamente**. Se tentar abrir uma 3ª locação sem finalizar as anteriores, a solicitação é barrada.
  - **Checagem de Bloqueio:** O cliente é impedido de realizar novas locações caso seu campo `bloqueadoAte` possua data futura (penalidade aplicada por atrasos em devoluções anteriores).

* **RN3 – Cálculos Automáticos no Backend:**
  O `valorTotal` da locação é gerado **100% dinamicamente no backend**, sem ser recebido pelo payload JSON do cliente. O cálculo multiplica o intervalo de diárias contratadas (`dataPrevistaDevolucao - dataInicio`) pelo `valorDiaria` cadastrado no veículo. Caso a locação ocorra no mesmo dia, é cobrado o valor mínimo de 1 diária. O status inicial `ATIVA` também é fixado automaticamente pelo servidor.

* **RN4 – Penalidades e Condicionais Complexas na Devolução:**
  No momento do encerramento da locação (`PATCH /locacoes/{id}/devolucao`):
  - Se a `dataRealDevolucao` for **posterior** à `dataPrevistaDevolucao` (atraso na entrega):
    1. O sistema calcula a quantidade de dias em atraso.
    2. Cobra as diárias excedentes utilizadas pelo condutor.
    3. Aplica **multa adicional de 20% sobre o valor da diária por cada dia de atraso**:
       $$\text{Multa} = \text{valorDiaria} \times 0.20 \times \text{diasAtraso}$$
    4. Atualiza o `valorTotal` final somando as diárias extras e a multa calculada.
    5. Aplica suspensão punitiva ao cliente, atualizando seu campo `bloqueadoAte` para **15 dias a partir da data atual** (`LocalDate.now().plusDays(15)`), impedindo novas locações nesse período.
  - Se a devolução ocorrer no prazo, a locação é marcada como `FINALIZADA` sem aplicação de multas ou suspensão ao cliente.

* **Proteção de Integridade Referencial em Exclusões:**
  - Veículos com locações associadas **não** podem ser excluídos (`400 Bad Request`).
  - Clientes com locações ativas ou histórico de locações **não** podem ser excluídos (`400 Bad Request`).

---

## 3. Tecnologias Utilizadas e Padrões de Projeto

| Módulo | Especificação Técnica Adotada |
| :--- | :--- |
| **Linguagem & Framework** | Java 17/21 LTS, Spring Boot 3.4.3 |
| **Persistência** | Spring Data JPA, Hibernate ORM, H2 Database (em memória) |
| **Validação** | Jakarta Bean Validation (`@Valid`, `@NotBlank`, `@Email`, `@NotNull`, `@Size`, etc.) |
| **Arquitetura** | Padrão em camadas estrito: `controller`, `service`, `repository`, `entity`, `dto`, `exception`, `config` |
| **Exposição de Dados** | Proibição de exposição direta de entidades; uso estrito de DTOs (`Request` e `Response`) |
| **Tratamento de Erros** | `@RestControllerAdvice` global com respostas padronizadas e mapeamento de status codes |
| **Padrão REST** | Verbos HTTP semânticos (GET, POST, PUT, PATCH, DELETE), URIs no plural e controllers enxutos |

---

## 4. Como Executar o Projeto

### Pré-requisitos
* **Java JDK 17 ou superior** instalado e configurado no ambiente.
* Terminal (PowerShell, Bash ou Prompt de Comando).

### Passo a Passo

1. **Clonar ou abrir a pasta do repositório:**
   ```bash
   cd Locacao-Veiculos
   ```

2. **Compilar e rodar os testes automatizados:**
   ```powershell
   # Windows (PowerShell / CMD)
   .\mvnw.cmd test

   # Linux / macOS
   ./mvnw test
   ```

3. **Iniciar a aplicação:**
   ```powershell
   # Windows (PowerShell / CMD)
   .\mvnw.cmd spring-boot:run

   # Linux / macOS
   ./mvnw spring-boot:run
   ```

4. A aplicação iniciará na porta padrão `8080`:
   * **URL Base da API:** `http://localhost:8080`

### Acesso ao Console do Banco H2
Durante a execução, o console web do banco de dados em memória está disponível para auditoria:
* **URL:** `http://localhost:8080/h2-console`
* **Driver Class:** `org.h2.Driver`
* **JDBC URL:** `jdbc:h2:mem:locadoradb`
* **User Name:** `sa`
* **Password:** *(deixar em branco)*

> [!NOTE]
> A aplicação inclui um componente `DataLoader` que popula automaticamente registros iniciais de veículos, clientes e locações para facilitar testes imediatos.

---

## 5. Tabela de Endpoints da API

### Veículos (`/veiculos`)

| Método | Rota | Descrição | Status Sucesso | Status Erro |
| :--- | :--- | :--- | :---: | :---: |
| `POST` | `/veiculos` | Cadastrar novo veículo com placa única | `201 Created` | `400`, `409` |
| `GET` | `/veiculos` | Listar todos os veículos | `200 OK` | - |
| `GET` | `/veiculos/{id}` | Buscar veículo por ID | `200 OK` | `404 Not Found` |
| `GET` | `/veiculos/busca?modelo={texto}` | Query method: buscar veículos por modelo | `200 OK` | - |
| `PUT` | `/veiculos/{id}` | Atualizar dados de um veículo | `200 OK` | `400`, `404`, `409` |
| `DELETE` | `/veiculos/{id}` | Excluir veículo (bloqueado se em uso) | `204 No Content`| `400`, `404` |
| `GET` | `/veiculos/{id}/locacoes` | Listar transações vinculadas a um veículo | `200 OK` | `404 Not Found` |

### Clientes (`/clientes`)

| Método | Rota | Descrição | Status Sucesso | Status Erro |
| :--- | :--- | :--- | :---: | :---: |
| `POST` | `/clientes` | Cadastrar cliente (CPF/e-mail únicos) | `201 Created` | `400`, `409` |
| `GET` | `/clientes` | Listar todos os clientes | `200 OK` | - |
| `GET` | `/clientes/{id}` | Buscar cliente por ID | `200 OK` | `404 Not Found` |
| `GET` | `/clientes/busca?nome={texto}` | Query method: buscar clientes por nome | `200 OK` | - |
| `PUT` | `/clientes/{id}` | Atualizar dados do cliente | `200 OK` | `400`, `404`, `409` |
| `DELETE` | `/clientes/{id}` | Excluir cliente (bloqueado se tiver histórico) | `204 No Content`| `400`, `404` |
| `GET` | `/clientes/{id}/locacoes` | Listar transações vinculadas a um cliente | `200 OK` | `404 Not Found` |

### Locações (`/locacoes`)

| Método | Rota | Descrição | Status Sucesso | Status Erro |
| :--- | :--- | :--- | :---: | :---: |
| `POST` | `/locacoes` | Criar nova locação (aplica RN1, RN2, RN3) | `201 Created` | `400`, `404` |
| `GET` | `/locacoes` | Listar todas as locações (filtros: `?veiculoId=`, `?clienteId=`) | `200 OK` | - |
| `GET` | `/locacoes/{id}` | Buscar locação por ID | `200 OK` | `404 Not Found` |
| `PATCH` | `/locacoes/{id}/devolucao` | Registrar devolução (aplica RN4: atraso/multa) | `200 OK` | `400`, `404` |
| `PATCH` | `/locacoes/{id}/cancelar` | Cancelar uma locação ativa | `200 OK` | `400`, `404` |

---

## 6. Exemplos de Requisições e Respostas

### 1. Cadastrar Veículo (`POST /veiculos`)
* **Request:**
  ```json
  {
    "modelo": "Honda Civic Touring",
    "placa": "CIV2026",
    "categoria": "SEDAN",
    "valorDiaria": 210.00
  }
  ```
* **Response (`201 Created`):**
  ```json
  {
    "id": 5,
    "modelo": "Honda Civic Touring",
    "placa": "CIV2026",
    "categoria": "SEDAN",
    "valorDiaria": 210.00
  }
  ```

### 2. Cadastrar Cliente (`POST /clientes`)
* **Request:**
  ```json
  {
    "nome": "Lucas Martins",
    "cpf": "55566677788",
    "email": "lucas.martins@email.com"
  }
  ```
* **Response (`201 Created`):**
  ```json
  {
    "id": 4,
    "nome": "Lucas Martins",
    "cpf": "55566677788",
    "email": "lucas.martins@email.com",
    "dataCadastro": "2026-11-17",
    "bloqueadoAte": null,
    "estaBloqueado": false
  }
  ```

### 3. Criar Locação (`POST /locacoes`) - Aplicação de RN3
* **Request:**
  ```json
  {
    "veiculoId": 1,
    "clienteId": 1,
    "dataInicio": "2026-11-20",
    "dataPrevistaDevolucao": "2026-11-25"
  }
  ```
* **Response (`201 Created`):**
  *(Cálculo: 5 diárias × R$ 180,00 = R$ 900,00)*
  ```json
  {
    "id": 3,
    "veiculo": {
      "id": 1,
      "modelo": "Toyota Corolla XEi 2.0",
      "placa": "BRA2E19",
      "categoria": "SEDAN",
      "valorDiaria": 180.00
    },
    "cliente": {
      "id": 1,
      "nome": "Carlos Eduardo da Silva",
      "cpf": "11122233344",
      "email": "carlos.silva@email.com",
      "dataCadastro": "2026-10-08",
      "bloqueadoAte": null,
      "estaBloqueado": false
    },
    "dataInicio": "2026-11-20",
    "dataPrevistaDevolucao": "2026-11-25",
    "dataRealDevolucao": null,
    "valorTotal": 900.00,
    "status": "ATIVA",
    "diasAtraso": null,
    "valorMulta": null
  }
  ```

### 4. Registrar Devolução com Atraso (`PATCH /locacoes/{id}/devolucao`) - Aplicação de RN4
* **Request:**
  ```json
  {
    "dataRealDevolucao": "2026-11-28"
  }
  ```
* **Response (`200 OK`):**
  *(3 dias de atraso: diárias extras R$ 540,00 + multa de 20% R$ 108,00 -> Total recalculado para R$ 1.548,00 e cliente bloqueado por 15 dias)*
  ```json
  {
    "id": 3,
    "veiculo": {
      "id": 1,
      "modelo": "Toyota Corolla XEi 2.0",
      "placa": "BRA2E19",
      "categoria": "SEDAN",
      "valorDiaria": 180.00
    },
    "cliente": {
      "id": 1,
      "nome": "Carlos Eduardo da Silva",
      "cpf": "11122233344",
      "email": "carlos.silva@email.com",
      "dataCadastro": "2026-10-08",
      "bloqueadoAte": "2026-12-02",
      "estaBloqueado": true
    },
    "dataInicio": "2026-11-20",
    "dataPrevistaDevolucao": "2026-11-25",
    "dataRealDevolucao": "2026-11-28",
    "valorTotal": 1548.00,
    "status": "FINALIZADA",
    "diasAtraso": 3,
    "valorMulta": 108.00
  }
  ```

### 5. Exemplo de Resposta de Erro Padronizada (`400 Bad Request`)
```json
{
  "timestamp": "2026-11-17T13:45:00",
  "status": 400,
  "error": "Violação de regra de negócio",
  "message": "O veículo 'Jeep Renegade Longitude' (Placa: JEP4X88) já possui uma locação ativa em andamento e não está disponível.",
  "path": "/locacoes"
}
```

---

## 7. Coleção do Postman / Insomnia

Para facilitar os testes de todos os endpoints e regras de negócio:
* O arquivo de coleção já formatado está incluído na raiz do projeto: [`postman_collection.json`](./postman_collection.json).
* **Como importar no Postman:**
  1. Abra o Postman.
  2. Clique no botão **Import** (canto superior esquerdo).
  3. Selecione o arquivo `postman_collection.json` localizado na pasta raiz deste projeto.
  4. Todas as rotas (Veículos, Clientes, Locações, cenários de sucesso e testes de erros das regras de negócio) estarão prontamente organizadas em pastas para disparo.
