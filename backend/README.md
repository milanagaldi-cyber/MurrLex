# Backend

Django backend for Make Mistake.

Current state:

- minimal Django project is created;
- SQLite is used by default for local development;
- `DATABASE_URL` can later switch the project to PostgreSQL settings;
- no lesson models, Lab UI, or import API have been added yet.

## Local Setup

From this folder:

```powershell
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
```

The project reads environment variables from:

```text
C:\CodexProjects\MakeMistake\.env
C:\CodexProjects\MakeMistake\backend\.env
```

For local development, copy the root `.env.example` to `.env`.

## Database

Default local database:

```text
backend/db.sqlite3
```

If `DATABASE_URL` is empty, Django uses SQLite.

Later PostgreSQL example:

```text
DATABASE_URL=postgresql://make_mistake_user:password@localhost:5432/make_mistake
```

## Verification

```powershell
.\.venv\Scripts\python.exe manage.py check
.\.venv\Scripts\python.exe manage.py migrate
.\.venv\Scripts\python.exe manage.py runserver
```
