# Underground

Protótipo navegável do MVP da Underground, uma plataforma de companhia social não sexual para adultos verificados no Brasil e em Portugal.

## Executar o protótipo

```bash
cd underground/frontend
python3 -m http.server 4173
```

Abra <http://localhost:4173>. O protótipo usa apenas dados fictícios e não executa verificação, pagamentos, reservas ou rastreamento reais.

## Estrutura

- `underground/frontend/`: protótipo web responsivo e acessível, sem dependências de execução.
- `underground/docs/`: requisitos, arquitetura e decisões do produto.
- `underground/backend/`: API Java 21: identidade, autenticação e perfis (consulte o README do backend).
- `underground/AI_MANIFEST.md`: índice de decisões e estado verificável da entrega.

## Validação rápida

```bash
python3 -m unittest discover -s underground/frontend/tests -v
```
