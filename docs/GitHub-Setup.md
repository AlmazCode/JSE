# Публикация JSE на GitHub

Локальный репозиторий уже инициализирован на ветке main. Перед публикацией убедитесь, что команда использует правильный аккаунт и нужный GitHub-репозиторий. Этот файл описывает команды публикации; они не выполняются автоматически.

## Через сайт GitHub

1. Создайте пустой репозиторий JSE в своём аккаунте или организации команды. Выберите нужную видимость. Не добавляйте README, .gitignore или LICENSE на сайте: локальный проект уже имеет начальный коммит.
2. Скопируйте HTTPS или SSH URL репозитория.
3. В каталоге проекта выполните команды, заменив `REPOSITORY_URL` на скопированный URL:

```bash
git remote add origin REPOSITORY_URL
git push -u origin main
```

Если origin уже добавлен, сначала посмотрите `git remote -v` и используйте существующий адрес, когда он верный. Не заменяйте чужой remote автоматически.

## Через GitHub CLI

Если вы используете gh и вошли в нужный аккаунт, можно создать приватный репозиторий и отправить готовый main одной командой:

```bash
gh repo create JSE --private --source=. --remote=origin --push
```

Это альтернативный путь: не выполняйте его после создания того же репозитория через сайт. Для публичной видимости вместо --private используется --public.

## После публикации

- Откройте вкладку Actions и проверьте Java 17 build. Локальная проверка не подтверждает результат удалённого workflow.
- Добавьте двух коллег в collaborators через настройки репозитория. GitHub-имена пользователей не указаны в ТЗ, поэтому CODEOWNERS пока не назначен.
- Каждый клонирует проект и выполняет команды README. Windows использует mvnw.cmd.
- Создайте Issues для ближайших E2/A1/Y1 через Implementation task. Назначайте исполнителей по их настоящим GitHub-профилям.
- При необходимости включите правило защиты main с PR и успешной проверкой сборки.

Документация: [создание существующего проекта на GitHub](https://docs.github.com/en/migrations/importing-source-code/using-the-command-line-to-import-source-code/adding-locally-hosted-code-to-github), [GitHub CLI repo create](https://cli.github.com/manual/gh_repo_create), [Java и Maven в GitHub Actions](https://docs.github.com/en/actions/tutorials/build-and-test-code/java-with-maven).
