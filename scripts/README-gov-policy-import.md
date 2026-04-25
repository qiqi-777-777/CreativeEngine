# Gov policy import

This project imports public policy pages from `www.gov.cn` into the existing
`policy_data` table.

## Files

- `scripts/import-gov-graduate-policies.js`: fetches public policy pages by keyword and writes SQL.
- `scripts/generated/graduate_policies.sql`: generated SQL for graduate-related policies.
- `scripts/alter_policy_data.sql`: optional migration if you later want separate source metadata columns.

## Run

Generate SQL for graduate, startup, and college student policy matching:

```bash
node scripts/import-gov-graduate-policies.js "--keyword=毕业生,创业,大学生" --pages=1 --output=scripts/generated/graduate_policies.sql
```

Import into MySQL:

```bash
mysql -uroot -p creative_engine < scripts/generated/graduate_policies.sql
```

The importer keeps request frequency low and preserves the original `gov.cn`
source URL inside each policy's content, so it works with the existing
`policy_data` table without adding new columns. Generated SQL inserts new
policies and refreshes existing rows with the same policy title.

For automatic import, the local machine needs:

- MySQL service running.
- The `mysql` command available in PATH.
- The `creative_engine` database already created.
- The database password, provided interactively after `-p` or through the
  backend `DB_PASSWORD` environment variable.
