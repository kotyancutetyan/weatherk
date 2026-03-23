# weatherk

Погодное приложение (статическая страница + OpenWeatherMap).

## Настройка

1. Скопируй `config.example.js` в `config.js`.
2. В `config.js` укажи свой ключ API: [OpenWeatherMap](https://openweathermap.org/api).

## Локальный запуск

Открой `index.html` в браузере (или подними любой статический сервер в папке проекта).

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
