#!/bin/bash

# Останавливаем контейнер (если запущен)
docker stop kotitonttu || true
docker rm kotitonttu || true

# Загружаем свежую версию репозитория
cd ~
rm -rf kotitonttu
git clone https://github.com/aalleexxaannddrr-prog/kotitonttu.git

# Переходим в папку проекта и строим Docker образ
cd kotitonttu
docker build -t kotitonttu .

# Запускаем новый контейнер
docker run -d --name kotitonttu -p 8080:8080 kotitonttu

# Проверка логов
docker logs -f kotitonttu
