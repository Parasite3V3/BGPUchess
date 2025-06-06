# Инструкция по загрузке проекта на GitHub

## Подготовка

1. Убедитесь, что у вас установлен Git. Вы уже скачали Git Bash и Git GUI, что отлично.

2. Создайте новый репозиторий на GitHub:
   - Перейдите на сайт [GitHub](https://github.com/)
   - Войдите в свой аккаунт
   - Нажмите на "+" в правом верхнем углу и выберите "New repository"
   - Введите имя репозитория (например, "ChessBGPU")
   - Добавьте описание (опционально)
   - Выберите видимость (публичный или приватный)
   - НЕ инициализируйте репозиторий с README, .gitignore или лицензией
   - Нажмите "Create repository"

## Загрузка проекта через Git Bash

1. Откройте Git Bash (щелкните правой кнопкой мыши в директории проекта и выберите "Git Bash Here" или запустите Git Bash и перейдите в директорию проекта)

2. Инициализируйте локальный репозиторий:
   ```bash
   git init
   ```

3. Добавьте все файлы проекта в индекс:
   ```bash
   git add .
   ```

4. Создайте файл .gitignore, чтобы исключить ненужные файлы:
   ```bash
   echo "*.iml
   .gradle
   /local.properties
   /.idea
   .DS_Store
   /build
   /captures
   .externalNativeBuild
   .cxx
   *.apk
   *.aab
   *.aar
   *.ap_
   *.dex" > .gitignore
   ```

5. Добавьте .gitignore в индекс:
   ```bash
   git add .gitignore
   ```

6. Создайте первый коммит:
   ```bash
   git commit -m "Initial commit"
   ```

7. Добавьте удаленный репозиторий (замените URL на URL вашего репозитория):
   ```bash
   git remote add origin https://github.com/YOUR_USERNAME/ChessBGPU.git
   ```

8. Отправьте изменения в удаленный репозиторий:
   ```bash
   git push -u origin master
   ```
   
   Если ваша основная ветка называется "main", используйте:
   ```bash
   git push -u origin main
   ```

9. При первой отправке Git попросит вас ввести учетные данные GitHub. Введите свой логин и пароль или токен.

## Загрузка проекта через Git GUI

1. Откройте Git GUI

2. Выберите "Create New Repository" и укажите директорию проекта

3. Нажмите "Create"

4. В главном окне Git GUI:
   - Все несохраненные файлы будут отображены в разделе "Unstaged Changes"
   - Нажмите "Stage Changed" для добавления всех файлов

5. Создайте файл .gitignore с тем же содержимым, что указано выше

6. Добавьте .gitignore в индекс, нажав "Stage Changed" снова

7. Введите сообщение коммита (например, "Initial commit") в поле "Commit Message"

8. Нажмите "Commit"

9. В меню выберите "Remote" > "Add..."
   - Введите "origin" в поле "Name"
   - Введите URL вашего репозитория в поле "Location"
   - Нажмите "Add"

10. В меню выберите "Push" > "origin"
    - Выберите локальную ветку (обычно "master" или "main")
    - Нажмите "Push"

11. Введите учетные данные GitHub при запросе

## Проверка загрузки

После загрузки проекта:
1. Перейдите на страницу вашего репозитория на GitHub
2. Убедитесь, что все файлы проекта отображаются
3. Проверьте, что файлы, указанные в .gitignore, не были загружены

## Дополнительные действия

### Добавление README.md

Вы можете добавить README.md в корень проекта, чтобы описать ваш проект:

```bash
git add README.md
git commit -m "Add README"
git push
```

### Добавление лицензии

Если вы хотите добавить лицензию к вашему проекту:
1. На GitHub перейдите в ваш репозиторий
2. Нажмите "Add file" > "Create new file"
3. Назовите файл "LICENSE"
4. GitHub предложит шаблоны лицензий
5. Выберите подходящую лицензию
6. Нажмите "Commit new file"

### Настройка защиты ветки

Для защиты основной ветки:
1. На GitHub перейдите в "Settings" вашего репозитория
2. Выберите "Branches"
3. Нажмите "Add rule"
4. Введите имя ветки (master или main)
5. Настройте правила защиты
6. Нажмите "Create" 