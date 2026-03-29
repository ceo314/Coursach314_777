package Steg007;
/*
 * Курсовой проект: Разработка приложения с графическим интерфейсом для криптографического шифрования
 *                  и стеганографического встраивания сообщений в цифровые изображения
 * Язык: Java
 * Автор: [Твоё имя]
 * Дата: 2025
 *
 * Программа позволяет:
 * - Зашифровать секретное сообщение с помощью AES-128 и спрятать его в изображение (метод LSB)
 * - Извлечь из изображения зашифрованные данные и расшифровать их при правильном пароле
 * Используются только стандартные библиотеки Java.
 */

import javax.crypto.Cipher;                     // для шифрования/дешифрования
import javax.crypto.spec.SecretKeySpec;        // для представления ключа
import javax.imageio.ImageIO;                  // для чтения/записи изображений
import javax.swing.*;                          // для графического интерфейса
import java.awt.*;                             // для работы с цветом и графикой
import java.awt.event.ActionEvent;             // для обработки событий кнопок
import java.awt.image.BufferedImage;           // для хранения и обработки изображений
import java.io.File;                           // для работы с файлами
import java.nio.charset.StandardCharsets;      // для корректной работы с UTF-8
import java.security.MessageDigest;             // для хэширования пароля
import java.security.NoSuchAlgorithmException; // для обработки ошибок алгоритма
import java.util.Arrays;                       // для копирования массива

public class Main extends JFrame {

    // Поля для компонентов интерфейса
    private JTextField imagePathField;          // поле для пути к файлу изображения
    private JTextArea messageArea;              // область для ввода/вывода текста
    private JPasswordField passwordField;       // поле для пароля (скрывает ввод)
    private JLabel imagePreviewLabel;           // метка для предпросмотра картинки (опционально)

    // Конструктор: создаёт окно и размещает элементы управления
    public Main() {
        setTitle("Криптостеганография — скрытая передача данных с шифрованием");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // закрытие окна завершает программу
        setSize(700, 600);                              // размер окна
        setLocationRelativeTo(null);                    // центр экрана
        initUI();                                       // вызываем метод создания интерфейса
    }

    // Метод, который строит интерфейс
    private void initUI() {
        // Основная панель с отступами
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // ---- Панель выбора файла (верх) ----
        JPanel filePanel = new JPanel(new BorderLayout(5, 5));
        filePanel.add(new JLabel("Изображение:"), BorderLayout.WEST);
        imagePathField = new JTextField();
        filePanel.add(imagePathField, BorderLayout.CENTER);
        JButton browseButton = new JButton("Обзор");
        browseButton.addActionListener(this::browseImage); // обработчик нажатия
        filePanel.add(browseButton, BorderLayout.EAST);
        mainPanel.add(filePanel, BorderLayout.NORTH);

        // ---- Центральная панель: сообщение и пароль ----
        JPanel centerPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Поле для сообщения
        gbc.gridx = 0;
        gbc.gridy = 0;
        centerPanel.add(new JLabel("Секретное сообщение:"), gbc);
        gbc.gridy = 1;
        messageArea = new JTextArea(6, 40);
        messageArea.setLineWrap(true);               // перенос строк
        messageArea.setWrapStyleWord(true);          // перенос по словам
        JScrollPane scrollPane = new JScrollPane(messageArea);
        centerPanel.add(scrollPane, gbc);

        // Поле для пароля
        gbc.gridy = 2;
        centerPanel.add(new JLabel("Пароль:"), gbc);
        gbc.gridy = 3;
        passwordField = new JPasswordField(20);
        centerPanel.add(passwordField, gbc);

        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // ---- Панель кнопок действий (низ) ----
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton hideButton = new JButton("🔒 Зашифровать и спрятать");
        hideButton.addActionListener(this::hideMessage);
        JButton extractButton = new JButton("🔓 Извлечь и расшифровать");
        extractButton.addActionListener(this::extractMessage);
        buttonPanel.add(hideButton);
        buttonPanel.add(extractButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Добавляем основную панель в окно
        add(mainPanel);
    }

    // Обработчик кнопки "Обзор": открывает диалог выбора файла
    private void browseImage(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Изображения PNG, JPG", "png", "jpg", "jpeg"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            imagePathField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    /**
     * Генерация ключа AES-128 из пароля.
     * Берём SHA-256 от пароля и берём первые 16 байт (128 бит).
     */
    private SecretKeySpec deriveKey(String password) throws NoSuchAlgorithmException {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = sha.digest(password.getBytes(StandardCharsets.UTF_8));
        keyBytes = Arrays.copyOf(keyBytes, 16); // AES-128
        return new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * Шифрование текста с паролем. Возвращает зашифрованные байты.
     */
    private byte[] encrypt(String message, String password) throws Exception {
        // Создаём шифровальщик в режиме AES/ECB/PKCS5Padding
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        SecretKeySpec key = deriveKey(password);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        // Преобразуем строку в байты UTF-8 и шифруем
        return cipher.doFinal(message.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Дешифрование байтов в строку.
     */
    private String decrypt(byte[] encryptedData, String password) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        SecretKeySpec key = deriveKey(password);
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decrypted = cipher.doFinal(encryptedData);
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    /**
     * Встраивание зашифрованных данных в изображение (LSB).
     * Алгоритм:
     * - Сначала сохраняем длину зашифрованных данных в первых 4 байтах.
     * - Затем побайтово записываем сами данные.
     * - Каждый байт разбивается на 8 бит, каждый бит записывается в младший бит
     *   одного из цветовых каналов (R, G, B) последовательно.
     */
    private void hideMessage(ActionEvent e) {
        String imagePath = imagePathField.getText();
        String message = messageArea.getText();
        String password = new String(passwordField.getPassword());

        // Проверка заполнения полей
        if (imagePath.isEmpty() || message.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Заполните все поля!");
            return;
        }

        try {
            // 1. Загружаем изображение
            BufferedImage img = ImageIO.read(new File(imagePath));
            if (img == null) {
                JOptionPane.showMessageDialog(this, "Не удалось загрузить изображение. Формат PNG или JPG?");
                return;
            }

            // 2. Шифруем сообщение
            byte[] encrypted = encrypt(message, password);

            // 3. Формируем массив для встраивания: [4 байта длины] + [encrypted]
            byte[] dataToHide = new byte[encrypted.length + 4];
            // Записываем длину зашифрованных данных в big-endian порядке
            dataToHide[0] = (byte) (encrypted.length >> 24);
            dataToHide[1] = (byte) (encrypted.length >> 16);
            dataToHide[2] = (byte) (encrypted.length >> 8);
            dataToHide[3] = (byte) (encrypted.length);
            // Копируем сами данные
            System.arraycopy(encrypted, 0, dataToHide, 4, encrypted.length);

            // 4. Проверяем, влезет ли всё в изображение
            int totalBits = dataToHide.length * 8;          // всего битов
            int pixelsNeeded = (int) Math.ceil(totalBits / 3.0); // в 1 пикселе 3 канала -> 3 бита
            if (img.getWidth() * img.getHeight() < pixelsNeeded) {
                JOptionPane.showMessageDialog(this,
                        "Изображение слишком маленькое для этого сообщения. Нужно хотя бы " +
                                pixelsNeeded + " пикселей, а у вас " + (img.getWidth() * img.getHeight()));
                return;
            }

            // 5. Встраиваем биты
            int bitIndex = 0; // глобальный счётчик битов
            int i = 0;
            while (i < dataToHide.length) {
                // Берём текущий байт
                byte currentByte = dataToHide[i];
                // Проходим по каждому биту от старшего (7) к младшему (0)
                for (int bitPos = 7; bitPos >= 0; bitPos--) {
                    int bit = (currentByte >> bitPos) & 1; // извлекаем бит

                    // Определяем координаты пикселя и номер цветового канала (0=R,1=G,2=B)
                    int pixelNumber = bitIndex / 3;        // номер пикселя (начиная с 0)
                    int channel = bitIndex % 3;            // канал
                    int x = pixelNumber % img.getWidth();   // колонка
                    int y = pixelNumber / img.getWidth();   // строка

                    // Получаем текущий цвет пикселя
                    int rgb = img.getRGB(x, y);
                    Color color = new Color(rgb, true); // true = учитывать альфа, но нам не нужен

                    int red = color.getRed();
                    int green = color.getGreen();
                    int blue = color.getBlue();

                    // Меняем нужный канал: заменяем младший бит на наш бит
                    switch (channel) {
                        case 0:
                            red = (red & 0xFE) | bit;   // 0xFE = 11111110, обнуляем последний бит
                            break;
                        case 1:
                            green = (green & 0xFE) | bit;
                            break;
                        case 2:
                            blue = (blue & 0xFE) | bit;
                            break;
                    }

                    // Собираем новый цвет и устанавливаем в изображение
                    Color newColor = new Color(red, green, blue);
                    img.setRGB(x, y, newColor.getRGB());

                    bitIndex++; // переходим к следующему биту
                }
                i++;
            }

            // 6. Сохраняем изменённое изображение в формате PNG
            String outputPath = imagePath.replaceFirst("(\\.[^.]+)$", "_secret$1");
            ImageIO.write(img, "png", new File(outputPath));
            JOptionPane.showMessageDialog(this,
                    "Сообщение успешно спрятано!\nФайл сохранён: " + outputPath);

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Ошибка при сокрытии: " + ex.getMessage());
        }
    }

    /**
     * Извлечение зашифрованных данных из изображения и их расшифровка.
     * Алгоритм:
     * - Сначала извлекаем первые 4 байта, чтобы узнать длину зашифрованного сообщения.
     * - Затем читаем ровно столько байтов, сколько указано в длине.
     * - Расшифровываем и выводим результат.
     */
    private void extractMessage(ActionEvent e) {
        String imagePath = imagePathField.getText();
        String password = new String(passwordField.getPassword());

        if (imagePath.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Укажите изображение и пароль!");
            return;
        }

        try {
            BufferedImage img = ImageIO.read(new File(imagePath));
            if (img == null) {
                JOptionPane.showMessageDialog(this, "Не удалось загрузить изображение.");
                return;
            }

            // 1. Извлекаем первые 4 байта (длину)
            byte[] lengthBytes = new byte[4];
            int bitIndex = 0;
            for (int i = 0; i < 32; i++) { // 4 байта * 8 бит = 32 бита
                int pixelNumber = bitIndex / 3;
                int channel = bitIndex % 3;
                int x = pixelNumber % img.getWidth();
                int y = pixelNumber / img.getWidth();

                int rgb = img.getRGB(x, y);
                Color color = new Color(rgb);
                int bit;

                // Извлекаем младший бит из нужного канала
                switch (channel) {
                    case 0:
                        bit = color.getRed() & 1;
                        break;
                    case 1:
                        bit = color.getGreen() & 1;
                        break;
                    default:
                        bit = color.getBlue() & 1;
                        break;
                }

                // Собираем байт: i - номер бита, byteIndex = i/8, bitInByte = 7 - (i%8)
                int byteIndex = i / 8;              // 0..3
                int bitInByte = 7 - (i % 8);        // от старшего бита к младшему
                if (bit == 1) {
                    lengthBytes[byteIndex] |= (1 << bitInByte);
                }
                bitIndex++;
            }

            // Преобразуем 4 байта в int (длина зашифрованных данных)
            int encryptedLength = ((lengthBytes[0] & 0xFF) << 24) |
                    ((lengthBytes[1] & 0xFF) << 16) |
                    ((lengthBytes[2] & 0xFF) << 8)  |
                    (lengthBytes[3] & 0xFF);

            // Проверяем, не превышает ли длина возможностей изображения
            if (encryptedLength <= 0 || encryptedLength > img.getWidth() * img.getHeight() * 3 / 8 - 4) {
                JOptionPane.showMessageDialog(this,
                        "Не удалось извлечь данные. Возможно, неверный пароль или изображение не содержит секрета.");
                return;
            }

            // 2. Извлекаем зашифрованные байты
            byte[] encrypted = new byte[encryptedLength];
            for (int i = 0; i < encryptedLength * 8; i++) {
                int globalBitIndex = i + 32; // пропускаем первые 32 бита (длина)
                int pixelNumber = globalBitIndex / 3;
                int channel = globalBitIndex % 3;
                int x = pixelNumber % img.getWidth();
                int y = pixelNumber / img.getWidth();

                int rgb = img.getRGB(x, y);
                Color color = new Color(rgb);
                int bit;

                switch (channel) {
                    case 0:
                        bit = color.getRed() & 1;
                        break;
                    case 1:
                        bit = color.getGreen() & 1;
                        break;
                    default:
                        bit = color.getBlue() & 1;
                        break;
                }

                int byteIndex = i / 8;
                int bitInByte = 7 - (i % 8);
                if (bit == 1) {
                    encrypted[byteIndex] |= (1 << bitInByte);
                }
            }

            // 3. Расшифровываем
            String decrypted = decrypt(encrypted, password);
            messageArea.setText(decrypted);
            JOptionPane.showMessageDialog(this, "Сообщение успешно извлечено!");

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Ошибка при извлечении: " + ex.getMessage());
        }
    }

    // Точка входа в программу
    public static void main(String[] args) {
        // Запускаем интерфейс в потоке обработки событий (правило Swing)
        SwingUtilities.invokeLater(() -> {
            new Main().setVisible(true);
        });
    }
}
