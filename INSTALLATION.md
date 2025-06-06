# Инструкция по установке ChessBGPU

## Требования
- Android 5.0 (API уровень 21) или выше
- Минимум 100 МБ свободного места на устройстве

## Установка отладочной версии

Отладочная версия приложения находится по пути:
```
app/build/outputs/apk/debug/app-debug.apk
```

Для установки отладочной версии:

1. Включите режим разработчика на вашем устройстве Android:
   - Перейдите в "Настройки" > "О телефоне"
   - Нажмите 7 раз на "Номер сборки" или "Версия MIUI" (зависит от устройства)
   - Вы увидите сообщение "Вы стали разработчиком"

2. Включите отладку по USB:
   - Перейдите в "Настройки" > "Дополнительно" > "Для разработчиков"
   - Включите "Отладка по USB"

3. Подключите устройство к компьютеру по USB

4. Установите приложение с помощью ADB:
   ```
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

   Или просто скопируйте APK файл на устройство и установите его, разрешив установку из неизвестных источников.

## Установка релизной версии

Релизная версия приложения находится по пути:
```
app/build/outputs/apk/release/app-release-unsigned.apk
```

Обратите внимание, что релизная версия не подписана и требует подписи перед установкой.

### Подписание APK

1. Создайте ключ подписи (если у вас его еще нет):
   ```
   keytool -genkey -v -keystore chessbgpu.keystore -alias chessbgpu -keyalg RSA -keysize 2048 -validity 10000
   ```

2. Подпишите APK с помощью созданного ключа:
   ```
   jarsigner -verbose -sigalg SHA1withRSA -digestalg SHA1 -keystore chessbgpu.keystore app/build/outputs/apk/release/app-release-unsigned.apk chessbgpu
   ```

3. Оптимизируйте APK с помощью zipalign:
   ```
   zipalign -v 4 app/build/outputs/apk/release/app-release-unsigned.apk app-release.apk
   ```

4. Установите подписанный APK:
   ```
   adb install -r app-release.apk
   ```

## Установка через Android Studio

Самый простой способ установки - использовать Android Studio:

1. Откройте проект в Android Studio
2. Подключите устройство Android к компьютеру
3. Нажмите кнопку "Run" (зеленый треугольник) на панели инструментов
4. Выберите ваше устройство в списке и нажмите "OK"
5. Android Studio автоматически соберет, подпишет и установит приложение на ваше устройство

## Возможные проблемы

### Ошибка "adb не распознается как внутренняя или внешняя команда"

Если вы получаете такую ошибку, убедитесь, что:
1. Android SDK Platform Tools установлен
2. Путь к adb добавлен в переменную PATH вашей системы

Для Windows:
```
set PATH=%PATH%;C:\Users\YOUR_USERNAME\AppData\Local\Android\Sdk\platform-tools
```

Для Linux/Mac:
```
export PATH=$PATH:~/Android/Sdk/platform-tools
```

### Ошибка "Установка заблокирована"

Если вы получаете ошибку о блокировке установки:
1. Перейдите в "Настройки" > "Безопасность"
2. Включите "Неизвестные источники" или "Установка из неизвестных источников"
3. Попробуйте установить APK снова 