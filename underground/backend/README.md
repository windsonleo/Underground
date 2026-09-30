# Backend — primeira entrega

Java 21, Maven, Spring Boot 3.5.16, Spring Security, JDBC, Flyway e MySQL. H2 existe somente no classpath de testes. Frontend ainda não integrado à API.

## Executar

Crie um banco MySQL vazio e um usuário com permissões para executar as migrações. Configure no ambiente (não versionar valores):

- `DB_URL`: URL JDBC do MySQL, incluindo banco e opções TLS apropriadas.
- `DB_USERNAME` e `DB_PASSWORD`: credenciais do banco.
- `PORT`: opcional, padrão 8080.
- `SESSION_COOKIE_SECURE`: padrão `true`; use `false` somente para HTTP local.
- `OPENAPI_ENABLED`: padrão `false`; use `true` para documentação local.

No PowerShell, selecione seu JDK 21 com `$env:JAVA_HOME = 'caminho-do-jdk-21'`. Execute neste diretório:

```sh
mvn spring-boot:run
```

Flyway cria as tabelas automaticamente. Não há credenciais padrão, seed de ADMIN ou aprovação automática de identidade. Sessões são locais ao processo e expiram após 30 minutos de inatividade; reiniciar encerra as sessões.

## API e autenticação

1. `GET /api/auth/csrf`: guarde o cookie de sessão e os campos `headerName` e `token`.
2. `POST /api/auth/register`: JSON com `email`, `password`, `role` (`CLIENT` ou `HOST`) e `adultConfirmed: true`, enviando o cabeçalho CSRF. Retorna 201 e conta pendente; não inicia sessão autenticada.
3. `POST /api/auth/login`: formulário `application/x-www-form-urlencoded`, campos `email` e `password`, cookie e CSRF. Retorna 204 ou 401. Preserve o cookie retornado.
4. Consulte novamente `/api/auth/csrf` após login e logout: a autenticação renova o token.
5. `POST /api/auth/logout`: cookie e CSRF; invalida a sessão (204).

Senhas: 12–64 caracteres ASCII imprimíveis, armazenadas com BCrypt custo 12. O limite em bytes evita truncamento do BCrypt. Emails são normalizados para minúsculas. Confirmação de maioridade é uma declaração, não uma verificação documental. Estados previstos: `PENDING`, `VERIFIED`, `REJECTED`; não há transição exposta nesta etapa.

| Endpoint | Acesso |
| --- | --- |
| `GET /api/accounts/me` | Dados privados da própria conta |
| `GET /api/profiles/me` | Próprio perfil; 404 enquanto não criado |
| `PUT /api/profiles/me` | Cria/atualiza próprio perfil, JSON `displayName` e `bio` |
| `GET /api/profiles/{id}` | Solicitante e alvo verificados; retorna apenas `id`, `displayName`, `bio` |

Todos os endpoints de perfis exigem autenticação. Mutações exigem CSRF. Campos extras são rejeitados; proprietário deriva da sessão. ADMIN não ganha acesso a dados de terceiros. Contas pendentes podem editar seu perfil, mas não descobrir outros perfis. Alvos inelegíveis retornam 404. Não existe matching implementado; a interface `IdentityDirectory` já fornece a regra de elegibilidade para módulos futuros.

Com OpenAPI habilitado: `/v3/api-docs` e `/swagger-ui/index.html`. Login/logout são filtros do Spring Security e estão documentados também no OpenAPI, incluindo formulário, cookie e CSRF. Para clientes de navegador, use a mesma origem por proxy: CORS não está habilitado.

## Módulos e testes

`identity` detém contas, credenciais e elegibilidade. `profiles` detém sua tabela e consulta exclusivamente `identity.api.IdentityDirectory`. `shared` contém configuração de segurança e erros. Sem joins ou chaves estrangeiras entre tabelas privadas dos módulos. Testes ArchUnit verificam dependências Java entre módulos.

```sh
mvn verify
```

Os testes ativam H2 em modo MySQL e executam a mesma migração Flyway. Cobrem cadastro, hash, autenticação real, CSRF, logout, titularidade, projeção pública, inelegibilidade e ausência de privilégios administrativos implícitos. A execução real em MySQL precisa ser validada separadamente; H2 não comprova compatibilidade completa de produção.

Fora desta entrega: pagamentos, créditos, matching, encontros, tracking, fornecedor de identidade, recuperação de senha e operações administrativas. Antes de exposição pública, definir e implementar limites de tentativas e a operação de verificação; não há conta verificável pela API atual.

Versões conferidas nas fontes oficiais: [Spring Boot](https://docs.spring.io/spring-boot/3.5/system-requirements.html) e [springdoc](https://springdoc.org/).
