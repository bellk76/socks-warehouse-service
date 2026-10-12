# Socks Warehouse Service

Web-приложение для автоматизации учёта носков на складе магазина.

## Стек

- Java 17, Spring Boot 3.3
- PostgreSQL, Flyway (миграции схемы)
- Maven (Maven Wrapper — сборка без глобальной установки Maven)
- Docker, Testcontainers (для интеграционных тестов)

## Запуск

### Вариант 1: Docker Compose (рекомендуется)

```bash
docker compose up --build
```

Приложение будет доступно на http://localhost:8080, PostgreSQL — на `localhost:5432`.

### Вариант 2: Локально (нужен запущенный PostgreSQL)

```bash
./mvnw spring-boot:run
```

Переменные окружения для подключения к БД (по умолчанию — localhost):

- `DB_URL` (по умолчанию `jdbc:postgresql://localhost:5432/socks_db`)
- `DB_USER` (по умолчанию `socks`)
- `DB_PASSWORD` (по умолчанию `socks`)

## API

### Приход

```
POST /api/socks/income
Content-Type: application/json

{"color": "red", "cottonPart": 80, "quantity": 10}
```

### Расход

```
POST /api/socks/outcome
Content-Type: application/json

{"color": "red", "cottonPart": 80, "quantity": 5}
```

### Остатки

```
GET /api/socks?color=red&operation=moreThan&cottonPart=90
GET /api/socks?color=black&operation=lessThan&cottonPart=10
GET /api/socks?color=yellow&operation=equal&cottonPart=50
```

Ответ — целое число (общее количество пар).

## Коды ответов

- `200` — успех
- `400` — некорректные/отсутствующие параметры, нехватка носков на расход
- `500` — внутренняя ошибка (например, БД недоступна)

## Тесты

```bash
./mvnw test
```

- **Юнит-тесты** (`SocksServiceTest`): JUnit 5 + Mockito — бизнес-логика сервиса.
- **Интеграционные** (`SocksControllerTest`): MockMvc + Testcontainers (реальный PostgreSQL в контейнере). Пропускаются, если движок контейнеров недоступен.

### Запуск интеграционных тестов

Интеграционным тестам нужен работающий движок контейнеров (Docker **или** Podman). Образ `postgres:16-alpine` Testcontainers скачивает и поднимает сам — вручную ничего запускать не нужно.

**Вариант A — Docker Desktop** (если доступен): просто запустите Docker Desktop и выполните `./mvnw test`.

**Вариант B — Podman** (без регистрации, актуально при ограничениях на Docker Desktop):

1. Установите Podman CLI + Podman Desktop, создайте и запустите машину:
   ```bash
   podman machine init
   podman machine start
   ```
2. Создайте файл `~/.testcontainers.properties` со строкой (требуется для rootless-режима Podman):
   ```
   ryuk.container.privileged=true
   ```
3. Проверьте, что движок виден:
   ```bash
   podman ps
   ```
4. Запустите тесты: `./mvnw test` (из IDE — обычным Run, дополнительных настроек не требуется).

Сокет Podman пробрасывается на `npipe:////./pipe/docker_engine`, Testcontainers находит его автоматически.

## Деплой на Render

Проект содержит `render.yaml` (Blueprint) — разворачивает web-сервис (Docker) и PostgreSQL одной кнопкой.

1. Создайте аккаунт на [render.com](https://render.com) (нужна только почта, карта не требуется).
2. Подключите GitHub-аккаунт: `Dashboard` → `New` → `Blueprint` → выберите репозиторий.
3. Render найдёт `render.yaml` и создаст два сервиса: `socks-warehouse-service` (web) и `socks-db` (PostgreSQL). Переменные БД (`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`) подставятся автоматически.
4. После деплоя приложение будет доступно по адресу вида `https://socks-warehouse-service.onrender.com`.

Проверка: `GET https://<адрес>/actuator/health` → `{"status":"UP"}`.

Порт приложение берёт из переменной `PORT`, которую Render задаёт автоматически (`server.port: ${PORT:8080}`).

## Схема БД

Миграции Flyway: `src/main/resources/db/migration/`.

Таблица `socks`: уникальность по паре `color + cotton_part`, ограничения `cotton_part` 0..100 и `quantity` >= 0.
