## Perguntas reflexivas para a esteira DevSecOps

- Quais dados entram neste componente e qual regra explícita valida cada um deles?
- Uma entrada manipulada pode provocar injeção, execução indevida, vazamento ou indisponibilidade?
- Quem pode executar esta ação e como a autorização é validada no servidor?
- O usuário possui apenas as permissões estritamente necessárias?
- Quais dados são sensíveis e onde ficam armazenados, transmitidos e registrados?
- Há credenciais, chaves ou tokens expostos no código, repositório, configuração ou logs?
- O tratamento de erro revela informações úteis para um atacante?
- As dependências utilizadas possuem vulnerabilidades conhecidas ou estão desatualizadas?
- O código passou por revisão e testes para cenários inválidos, abusivos e não autorizados?
- O pipeline bloqueia vulnerabilidades críticas, segredos expostos e dependências inseguras?
- Como o sistema reage à indisponibilidade ou à resposta malformada de uma API externa?
- Há logs de auditoria, monitoramento, alerta, plano de rollback e procedimento de resposta a incidentes?

## Recomendações Técnicas

### Java — bibliotecas recomendadas

- Spring Boot / Spring Security: autenticação, autorização, proteção de rotas e gerenciamento de sessões.
- Jakarta Bean Validation (jakarta.validation): validação declarativa de campos.
- OWASP Java Encoder: codificação segura de conteúdo destinado ao navegador.
- JPA/Hibernate ou JdbcTemplate: consultas parametrizadas.
- OWASP Dependency-Check: identificação de vulnerabilidades em dependências.
- Logback/SLF4J: logs estruturados, evitando dados sensíveis.

```
// Exemplo: validação de dados de entrada com Spring Boot

public record OrdemRequest(

    @NotBlank

    @Pattern(regexp = "^[A-Z]{4}[0-9]{1,2}$")

    String ativo,



    @NotNull

    @Positive

    Integer quantidade

) {}

@PostMapping("/ordens")

public ResponseEntity<Void> criarOrdem(@Valid @RequestBody OrdemRequest ordem) {

    // Processar somente após validação automática.

    return ResponseEntity.ok().build();

}



// Exemplo: consulta parametrizada — não concatenar dados do usuário em SQL

String sql = "SELECT * FROM ativos WHERE codigo = ?";

List<Ativo> ativos = jdbcTemplate.query(

    sql,

    new BeanPropertyRowMapper<>(Ativo.class),

    codigoAtivo

);



// Exemplo: codificação de conteúdo antes de enviá-lo ao HTML

import org.owasp.encoder.Encode;

String ativoSeguroParaHtml = Encode.forHtml(userInput);
```

### Python — bibliotecas recomendadas

- FastAPI ou Django: frameworks com mecanismos modernos de validação e proteção.
- Pydantic: validação e tipagem de entradas em APIs.
- SQLAlchemy ou ORM do Django: consultas parametrizadas e abstração segura de banco.
- bleach: sanitização de HTML quando esse tipo de conteúdo for indispensável.
- bandit: análise estática de segurança em código Python.
- pip-audit: identificação de vulnerabilidades em dependências.
- logging: logs estruturados, sem registrar segredos.

```
# Exemplo: validação de entrada com FastAPI e Pydantic

from fastapi import FastAPI

from pydantic import BaseModel, Field, field_validator

import re

app = FastAPI()

class Ordem(BaseModel):

    ativo: str = Field(min_length=5, max_length=6)

    quantidade: int = Field(gt=0, le=10000)

    @field_validator("ativo")

    @classmethod

    def validar_ativo(cls, valor: str) -> str:

        valor = valor.upper()

        if not re.fullmatch(r"[A-Z]{4}[0-9]{1,2}", valor):

            raise ValueError("Código de ativo inválido")

        return valor



@app.post("/ordens")

def criar_ordem(ordem: Ordem):

    return {"status": "ordem recebida"}



# Exemplo: consulta parametrizada com SQLAlchemy

from sqlalchemy import text

consulta = text("SELECT * FROM ativos WHERE codigo = :codigo")

resultado = conexao.execute(consulta, {"codigo": codigo_ativo})



# Exemplo: sanitização de HTML, somente quando HTML for necessário

import bleach

conteudo_seguro = bleach.clean(

    comentario_usuario,

    tags=["p", "strong", "em"],

    attributes={},

    strip=True

)
```

## Priorização da sanitização de dados e prevenção de vulnerabilidades

- Validar no servidor antes de processar, persistir, registrar ou encaminhar qualquer dado.
- Definir formato, tamanho, tipo, intervalo e caracteres permitidos para campos como código de ativo, quantidade, e-mail, senha filtros e comentários.
- Aplicar allowlist: aceitar apenas valores previstos pelas regras de negócio.
- Utilizar consultas parametrizadas, nunca concatenar dados de entrada em comandos SQL.
- Codificar a saída conforme o contexto: HTML, URL, JavaScript, JSON, logs ou arquivo.
- Sanitizar HTML somente quando o sistema realmente aceitar conteúdo HTML; para os demais casos, preferir texto simples.
- Rejeitar caminhos de arquivo, URLs e comandos construídos diretamente a partir de entradas do usuário.
- Limitar tamanho de requisições, uploads, número de tentativas e frequência de chamadas.
- Tratar dados recebidos de APIs externas como não confiáveis e validá-los novamente.
- Não utilizar funções perigosas para avaliar conteúdo dinâmico, como eval() em Python ou mecanismos equivalentes em Java.

## Exemplo de Diagrama

![diagrama-umlsec](https://i.imgur.com/BRWTWO8.png)
