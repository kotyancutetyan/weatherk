# weatherk

Погодное приложение (статическая страница + OpenWeatherMap).

## Настройка

1. Скопируй `config.example.js` в `config.js`.
2. В `config.js` укажи свой ключ API: [OpenWeatherMap](https://openweathermap.org/api).

## Локальный запуск

Открой `index.html` в браузере (или подними любой статический сервер в папке проекта).

## Разработка для телефона (Android)

### Полноэкранный режим
Приложение настроено на immersive fullscreen (скрытие верхней/нижней системных панелей) в `MainActivity`.

### Быстрый цикл изменений

#### Вариант 1 (простой, без live reload)
После изменений в `index.html`:
```bash
npm run copy
```
Потом в Android Studio нажми `Run` (или `Apply Changes`).

#### Вариант 2 (почти сразу на телефоне)
Live reload через Capacitor:
```bash
npm run run:android:live
```
Условия:
- телефон и ПК в одной сети;
- USB/ADB подключение настроено;
- если попросит host/port, укажи локальный IP и порт dev-сервера.

В live-режиме изменения веб-части появляются на телефоне без полной пересборки APK.

### Запуск приложения с GitHub Pages

Можно запускать Android-приложение напрямую с опубликованного сайта:
- в `capacitor.config.json` задан `server.url = https://kotyancutetyan.github.io/weatherk/`
- после изменения config выполни:
  ```bash
  npm run sync
  ```

Тогда приложение берёт страницу из GitHub Pages (нужен интернет), и сеть ПК/телефона может быть разной.

## Версионирование

Используем схему `0.MINOR.PATCH` (пока проект в pre-1.0):
- `MINOR` — заметная новая фича/изменение интерфейса.
- `PATCH` — исправления/малые доработки без новой крупной фичи.

Текущая версия выводится на странице в левом нижнем углу (еле заметно). При каждом обновлении версии меняй `APP_VERSION` в `index.html`.

## Публикация на GitHub

```bash
cd путь/к/проекту
git init
git add .
git commit -m "Initial commit"
git branch -M main
git remote add origin https://github.com/kotyancutetyan/weatherk.git
git push -u origin main
```

Если репозиторий уже не пустой, может понадобиться `git pull origin main --allow-unrelated-histories`, затем `git push`.

**Важно:** ключ в `config.js` в репозиторий не попадает (файл в `.gitignore`). На GitHub Pages после клона создай `config.js` локально или используй другой способ хранения секрета.
