# camunda-demo

Camunda 7.24 embarqué + "service externe" simulé pour tester `Process_CreationProfilTrader`.

## Lancer
    mvn spring-boot:run

- Cockpit / Tasklist : http://localhost:8080/camunda/app/  (demo / demo)
- API REST Camunda   : http://localhost:8080/engine-rest
- Console H2         : http://localhost:8080/h2-console  (jdbc:h2:file:./data/camunda-h2, sa, mot de passe vide)

Le BPMN est déployé automatiquement depuis `src/main/resources/processes/`.
Scénarios prêts à jouer : `test-scenarios.http` (IntelliJ / VS Code REST Client).

## Endpoints
| Méthode | URL | Rôle |
|---|---|---|
| POST | /mock-api/requests | Service externe : reçoit la requête et lance le workflow |
| POST | /mock-api/requests/{id}/status | Appelé par le workflow (WAITING_TRADER, COMPLETED, EXPIRED) |
| GET | /mock-api/requests[/{id}] | Statuts + historique côté service externe |
| POST | /mock-api/fail?enabled=true ou false | Simule une panne du service externe |
| GET | /api/requests/{id} | Etat du workflow (tâches, reminderCount, outcome...) |
| GET | /api/requests/{id}/emails | Emails simulés |
| POST | /api/requests/{id}/team/claim | "Assign to me" |
| POST | /api/requests/{id}/team/submit | Case de confirmation + Submit |
| POST | /api/requests/{id}/trader/confirm | Confirmation du trader |
