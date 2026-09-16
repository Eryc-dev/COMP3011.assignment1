# COMP3011 Assignment 1 - Speech-to-Text Web Site

Spring Boot web site that records audio in the browser, transcribes it with the OpenAI
`gpt-4o-mini-transcribe` model, and exposes the administration/statistics API defined in
`assignment1api.yaml`.

## Endpoints

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/` | Recording web page |
| POST | `/api/v1/transcribe` | Multipart `file` upload -> `{"text": "..."}` |
| GET | `/api/v1/admin/uptime` | Server start, now, uptime seconds |
| POST | `/api/v1/admin/shutdown` | 202 accepted / 409 already shutting down |
| GET | `/api/v1/global/stats` | Input/output tokens since server start |

## Design

- `controller` - thin REST controllers; `service` - OpenAI client, token counters, lifecycle;
  `dto` - records mirroring the YAML schemas; `exception` - `ErrorResponse` mapping; `config` - HTTP client.
- **Concurrency:** requests run on Java virtual threads (`spring.threads.virtual.enabled=true`), so
  blocking calls to OpenAI park cheap virtual threads and >200 concurrent requests do not block each
  other. Token counters use `AtomicLong`; one pooled, thread-safe HTTP client with timeouts is shared.
- **Security:** the API key is read only from the `OPENAI_API_KEY` environment variable and used only
  in the outbound `Authorization` header. It is never in source, logs, or responses.
- **Graceful shutdown:** `server.shutdown=graceful` lets in-flight requests complete.

## Configuration and profiles

- `application.properties` - defaults used on TITAN (`java -jar`, no profile).
- `application-local.properties` - development settings (debug logging). Activate with
  `--spring.profiles.active=local` or `SPRING_PROFILES_ACTIVE=local`.

## Build and run

```bash
./mvnw clean package            # runs unit/integration tests (no real OpenAI calls)
OPENAI_API_KEY=sk-... java -jar target/Assignment1-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

## Use of generative AI

Generative AI (Claude) was used to help diagnose the audio-format transcription failure, refactor the
code into layers, and draft unit tests. All code was reviewed, built and tested by me.
