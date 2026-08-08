# Backend: Invoice Intelligence

This is the Spring Boot backend for the invoice intelligence POC.

## Requirements

- Java 17+
- Maven
- Tesseract OCR installed on the host machine

## Run locally

```bash
cd backend
mvn spring-boot:run
```

## Environment

Set the Gemini API key before running:

```bash
setx GEMINI_API_KEY "YOUR_GEMINI_API_KEY"
```

Make sure Tesseract OCR is installed and the `tessdata` path is configured. On Windows, point `TESSDATA_PREFIX` to the folder that contains `eng.traineddata`.

```bash
setx TESSDATA_PREFIX "C:\Program Files\Tesseract-OCR\tessdata"
```

On macOS/Linux:

```bash
export TESSDATA_PREFIX="/usr/share/tessdata"
```

If you prefer, you can also set the application property directly in `application.yml`:

```yaml
app:
  tesseract:
    data-path: /path/to/tessdata
```

## Endpoints

- `POST /api/invoices/upload`
- `GET /api/invoices/{id}`
- `POST /api/invoices/{id}/query`
