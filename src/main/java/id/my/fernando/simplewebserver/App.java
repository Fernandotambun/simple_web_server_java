package id.my.fernando.simplewebserver;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

public class App extends Application {

    // Variabel-variabel UI
    private TextField portField; // TextField untuk memasukkan port server
    private TextField webDirectoryField; // TextField untuk memasukkan direktori web server
    private TextField logDirectoryField; // TextField untuk memasukkan direktori log server
    private Button startButton; // Tombol untuk memulai server
    private Button stopButton; // Tombol untuk menghentikan server
    private Button browseWebDirectoryButton; // Tombol untuk menelusuri direktori web
    private Button browseLogDirectoryButton; // Tombol untuk menelusuri direktori log
    private TextArea logTextArea; // TextArea untuk menampilkan log server
    private ServerSocket serverSocket; // ServerSocket untuk menerima koneksi dari klien
    private boolean isServerRunning = false; // Status apakah server sedang berjalan atau tidak

    // Path file konfigurasi
    private static final String CONFIG_FILE_PATH = "config.txt";

    @Override
    public void start(Stage primaryStage) {// Metode utama yang dipanggil saat aplikasi JavaFX dimulai, menginisialisasi jendela utama.
        primaryStage.setTitle("Web Server");

        // Konfigurasi UI
        GridPane configPane = new GridPane();
        configPane.setHgap(10);
        configPane.setVgap(5);
        configPane.setPadding(new Insets(10, 10, 10, 10));

        portField = new TextField();
        portField.setPromptText("Port"); // Placeholder untuk port
        webDirectoryField = new TextField();
        webDirectoryField.setPromptText("Web Directory"); // Placeholder untuk direktori web
        logDirectoryField = new TextField();
        logDirectoryField.setPromptText("Log Directory"); // Placeholder untuk direktori log

        browseWebDirectoryButton = new Button("Browse"); // Tombol untuk menelusuri direktori web
        browseWebDirectoryButton.setOnAction(e -> browseDirectory("Select Web Directory", webDirectoryField));

        browseLogDirectoryButton = new Button("Browse"); // Tombol untuk menelusuri direktori log
        browseLogDirectoryButton.setOnAction(e -> browseDirectory("Select Log Directory", logDirectoryField));

        configPane.addRow(0, new Label("Port:"), portField);
        configPane.addRow(1, new Label("Web Directory:"), webDirectoryField, browseWebDirectoryButton);
        configPane.addRow(2, new Label("Log Directory:"), logDirectoryField, browseLogDirectoryButton);

        TitledPane configTitledPane = new TitledPane("Configuration", configPane);
        configTitledPane.setCollapsible(false);

        // Tombol kontrol
        GridPane buttonPane = new GridPane();
        buttonPane.setHgap(10);
        buttonPane.setVgap(5);
        startButton = new Button("Start"); // Tombol untuk memulai server
        stopButton = new Button("Stop"); // Tombol untuk menghentikan server
        startButton.setOnAction(e -> startServer());
        stopButton.setOnAction(e -> stopServer());
        stopButton.setDisable(true); // Saat aplikasi pertama kali dijalankan, tombol Stop dinonaktifkan

        buttonPane.addRow(0, startButton, stopButton);

        Separator separator = new Separator(); // Membuat objek Separator untuk memisahkan bagian-bagian dalam antarmuka pengguna.
        separator.setPadding(new Insets(10, 0, 10, 0));

        // Tampilan log
        logTextArea = new TextArea(); // TextArea untuk menampilkan log server
        logTextArea.setEditable(false);
        logTextArea.setWrapText(true);
        logTextArea.setPrefHeight(200);

        VBox logBox = new VBox(10, new Label("Log:"), logTextArea);

        VBox root = new VBox(10, configTitledPane, separator, buttonPane, logBox);
        root.setPadding(new Insets(10, 10, 10, 10));

        primaryStage.setScene(new Scene(root, 400, 400)); // Mengatur ukuran dan tata letak utama aplikasi
        primaryStage.show();

        // Memuat konfigurasi yang tersimpan
        loadConfig();
    }

    // Method untuk memulai server
    private void startServer() {
        int port = Integer.parseInt(portField.getText()); // Mendapatkan port dari TextField
        String webDirectory = webDirectoryField.getText(); // Mendapatkan direktori web dari TextField
        String logDirectory = logDirectoryField.getText(); // Mendapatkan direktori log dari TextField

        new Thread(() -> { // Thread baru untuk menjalankan server
            try {
                serverSocket = new ServerSocket(port); // Membuat ServerSocket dengan port tertentu
                isServerRunning = true;
                log("Server started on port " + port); // Menampilkan log bahwa server telah dimulai

                while (isServerRunning) {
                    Socket clientSocket = serverSocket.accept(); // Menerima koneksi dari klien
                    handleRequest(clientSocket, webDirectory, logDirectory); // Menangani permintaan dari klien
                }
            } catch (IOException e) {
                log("Error starting server: " + e.getMessage()); // Menampilkan log jika terjadi kesalahan saat memulai server
            }
        }).start(); // Memulai thread baru untuk menjalankan server

        startButton.setDisable(true); // Menonaktifkan tombol Start setelah server dimulai
        stopButton.setDisable(false); // Mengaktifkan tombol Stop setelah server dimulai

        // Menyimpan konfigurasi
        saveConfig(port, webDirectory, logDirectory);
    }

    // Method untuk menghentikan server
    private void stopServer() {
        isServerRunning = false; // Mengubah status server menjadi tidak berjalan
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close(); // Menutup ServerSocket jika tidak null dan belum ditutup sebelumnya
            }
        } catch (IOException e) {
            log("Error stopping server: " + e.getMessage()); // Menampilkan log jika terjadi kesalahan saat menghentikan server
        }
        startButton.setDisable(false); // Mengaktifkan tombol Start setelah server dihentikan
        stopButton.setDisable(true); // Menonaktifkan tombol Stop setelah server dihentikan
    }

    // Method untuk menangani permintaan dari klien
    private void handleRequest(Socket clientSocket, String webDirectory, String logDirectory) {
        //membaca request yang dikirim oleh klien melalui socket.
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream())); // Menginisialisasi BufferedReader untuk membaca input dari klien
             BufferedOutputStream outputStream = new BufferedOutputStream(clientSocket.getOutputStream())) { // dan BufferedOutputStream untuk menulis output ke klien.

            String request = reader.readLine(); // Membaca request dari klien
            logAccess(request, logDirectory); // Merekam log akses dari klien

            String[] parts = request.split(" "); // Memecah request menjadi bagian-bagian parts[0] = "GET" parts[1] = "index.html" parts[2] = "HTTP/1.1"
            if (parts.length >= 2 && parts[0].equals("GET")) { // Memeriksa apakah permintaan merupakan permintaan HTTP GET.
                String requestedPath = parts[1]; // Mendapatkan path yang diminta dari request
                String filePath = webDirectory + requestedPath.replace("/", File.separator); // Mendapatkan path absolut file yang diminta
                File file = new File(filePath); // Membuat objek File dari path

                if (file.exists()) {
                    if (file.isFile()) {
                        // Jika file diminta, kirim kontennya
                        sendFile(outputStream, file);
                    } else if (file.isDirectory()) {
                        if (!requestedPath.endsWith("/")) {
                            // Jika direktori diminta tanpa "/" di akhir, redirect ke URL dengan "/"
                            outputStream.write(("HTTP/1.1 301 Moved Permanently\r\n").getBytes());
                            outputStream.write(("Location: " + requestedPath + "/\r\n").getBytes());
                            outputStream.write("\r\n".getBytes());
                        } else {
                            // Jika direktori diminta, tampilkan daftar file di dalamnya
                            listDirectory(outputStream, file);
                        }
                    }
                } else {
                    // Jika file tidak ditemukan, kirim respons 404 Not Found
                    String notFoundResponse = "HTTP/1.1 404 Not Found\r\n" +
                            "Content-Type: text/plain\r\n" +
                            "\r\n" +
                            "404 Not Found\r\n";
                    outputStream.write(notFoundResponse.getBytes());
                }
            }
        } catch (IOException e) {
            log("Error handling request: " + e.getMessage()); // Menampilkan log jika terjadi kesalahan saat menangani permintaan
        } finally {
            try {
                clientSocket.close(); // Menutup socket klien setelah selesai
            } catch (IOException e) {
                log("Error closing client socket: " + e.getMessage()); // Menampilkan log jika terjadi kesalahan saat menutup socket klien
            }
        }
    }

    // Method untuk mengirim file ke klien
    private void sendFile(OutputStream outputStream, File file) throws IOException {
        String contentType = getContentType(file.getName()); // Mendapatkan tipe konten berdasarkan ekstensi file

        // Membuat buffer untuk membaca konten file
        byte[] buffer = new byte[1024];
        int bytesRead;

        FileInputStream fileInputStream = new FileInputStream(file); // untuk membaca konten file

        // Menyusun header HTTP yang akan dikirimkan ke klien
        String header = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: " + contentType + "\r\n" +
                "\r\n";
        outputStream.write(header.getBytes());

        while ((bytesRead = fileInputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        fileInputStream.close();
        outputStream.flush();
    }

    // Method untuk menampilkan daftar file dalam sebuah direktori
    private void listDirectory(OutputStream outputStream, File directory) throws IOException {
        File[] files = directory.listFiles();
        StringBuilder htmlResponse = new StringBuilder();
        htmlResponse.append("<html><body>\r\n");
        htmlResponse.append("<h1>Directory listing</h1>\r\n");

        if (!directory.getPath().equals(webDirectoryField.getText())) {
            htmlResponse.append("<p><a href=\"..\">Back to Parent Directory</a></p>\r\n");
        }

        htmlResponse.append("<ul>\r\n");

        for (File file : files) {
            String fileName = file.getName();
            if (file.isDirectory()) {
                fileName += "/";
            }
            htmlResponse.append("<li><a href=\"" + fileName + "\">" + fileName + "</a></li>\r\n");
        }

        htmlResponse.append("</ul>\r\n");
        htmlResponse.append("</body></html>\r\n");

        String directoryListingResponse = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/html\r\n" +
                "\r\n" +
                htmlResponse.toString();
        outputStream.write(directoryListingResponse.getBytes());
        outputStream.flush();
    }

    // Method untuk merekam akses ke server dalam file log
    private void logAccess(String request, String logDirectory) {
        String logEntry = null;
        try {
            String[] parts = request.split(" ");
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            String url = parts[1];
            String ip = parts[2];

            logEntry = timestamp + " | URL: " + url + " | IP: " + ip + "\n";

            String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            String logFileName = logDirectory + File.separator + "access_log_" + date + ".txt";

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFileName, true))) {
                writer.write(logEntry);
            }
        } catch (IOException | ArrayIndexOutOfBoundsException e) {
            log("Error logging access: " + e.getMessage()); // Menampilkan log jika terjadi kesalahan saat merekam akses
        }
        log(logEntry.replaceAll("\n", "")); // Menampilkan log akses
    }

    // Method untuk mendapatkan tipe konten berdasarkan ekstensi file
    private String getContentType(String fileName) {
        if (fileName.endsWith(".html") || fileName.endsWith(".htm")) {
            return "text/html";
        } else if (fileName.endsWith(".css")) {
            return "text/css";
        } else if (fileName.endsWith(".js")) {
            return "text/javascript";
        } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (fileName.endsWith(".png")) {
            return "image/png";
        } else if (fileName.endsWith(".gif")) {
            return "image/gif";
        } else if (fileName.endsWith(".pdf")) {
            return "application/pdf";
        } else {
            return "application/octet-stream";
        }
    }

    // Method untuk menampilkan log ke console dan TextArea
    private void log(String message) {
        System.out.println(message);
        logTextArea.appendText(message + "\n");
    }

    // Method untuk menelusuri direktori dan memasukkan path ke TextField
    private void browseDirectory(String title, TextField directoryField) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle(title);
        File selectedDirectory = directoryChooser.showDialog(null);
        if (selectedDirectory != null) {
            directoryField.setText(selectedDirectory.getAbsolutePath()); // Mengatur teks pada directoryField dengan path dari direktori yang dipilih oleh pengguna.
        }
    }

    // Method untuk menyimpan konfigurasi ke file
    private void saveConfig(int port, String webDirectory, String logDirectory) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(CONFIG_FILE_PATH))) {
            writer.println("port=" + port);
            writer.println("webDirectory=" + webDirectory);
            writer.println("logDirectory=" + logDirectory);
        } catch (IOException e) {
            log("Error saving configuration: " + e.getMessage()); // Menampilkan log jika terjadi kesalahan saat menyimpan konfigurasi
        }
    }

    // Method untuk memuat konfigurasi dari file
    private void loadConfig() {
        try (BufferedReader reader = new BufferedReader(new FileReader(CONFIG_FILE_PATH))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("=", 2);// Memisahkan baris menjadi kunci (key) dan nilai (value) menggunakan "=" sebagai pemisah
                if (parts.length == 2) { // Memeriksa apakah baris memiliki dua bagian setelah dipisahkan
                    String key = parts[0];
                    String value = parts[1];
                    switch (key) {
                        case "port":
                            portField.setText(value);
                            break;
                        case "webDirectory":
                            webDirectoryField.setText(value);
                            break;
                        case "logDirectory":
                            logDirectoryField.setText(value);
                            break;
                    }
                }
            }
        } catch (IOException e) {
            log("Error loading configuration: " + e.getMessage()); // Menampilkan log jika terjadi kesalahan saat memuat konfigurasi
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
