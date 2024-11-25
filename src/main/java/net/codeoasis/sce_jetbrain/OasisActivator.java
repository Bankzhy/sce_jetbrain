package net.codeoasis.sce_jetbrain;

import com.google.gson.Gson;
import com.intellij.AppTopics;
import com.intellij.ide.DataManager;
import com.intellij.notification.*;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.openapi.editor.*;
import net.codeoasis.sce_jetbrain.listeners.CustomDocumentListener;
import net.codeoasis.sce_jetbrain.models.Heartbeat;
import net.codeoasis.sce_jetbrain.models.LineStats;
import com.intellij.openapi.fileEditor.FileEditorManager;
import java.awt.*;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.intellij.openapi.fileTypes.FileType;
import net.codeoasis.sce_jetbrain.models.SendHeartbeat;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;
import java.util.Map;
import com.intellij.openapi.project.ProjectManager;

public class OasisActivator {
    public static final Logger log = Logger.getLogger("Activator");
    private static OasisActivator instance = null;
    public static OasisActivator getInstance() {
        if (instance == null) {
            instance = new OasisActivator();
        }
        return instance;
    }
    public static Map<String, LineStats> lineStatsCache = new HashMap<String, LineStats>();
//    private static ConcurrentLinkedQueue<Heartbeat> heartbeatsQueue = new ConcurrentLinkedQueue<Heartbeat>();
    private static ArrayList<Heartbeat> heartbeatsQueue = new ArrayList<Heartbeat>();
    public static Boolean READY = false;
    public static String lastFile = null;
    public static String lastProject = null;
    public static String lastLanguage = null;
    public static int lastLineCount = 0;
    public static BigDecimal lastTime = new BigDecimal(0);
    public static final BigDecimal FREQUENCY = new BigDecimal(2 * 60);
    public static final int SEND_FREQUENCY = 20;
    public static Boolean isBuilding = false;
    private ScheduledExecutorService scheduler;
    public static boolean enoughTimePassed(BigDecimal currentTime) {
        return OasisActivator.lastTime.add(FREQUENCY).compareTo(currentTime) < 0;
    }
    TimeCounter timeCounter = TimeCounter.getInstance();

    private OasisActivator() {
        init();
    }

    private void init() {
        if (!LoginManager.isLoggedIn()) {
            // Show login window
            LoginDialog loginDialog = new LoginDialog();
            if (loginDialog.showAndGet()) {  // If the user clicked "OK"
                String username = loginDialog.getUsername();
                String password = loginDialog.getPassword();
                doOKAction(username, password);
            } else {
                showNotification("Login canceled!");
                return;  // Exit without sending the snippet
            }
        }
        // Initialize the scheduler
         scheduler = Executors.newScheduledThreadPool(1);

        // Schedule the task to run every 15 minutes (900 seconds)
         scheduler.scheduleAtFixedRate(this::sendHeartbeat, 0, 1, TimeUnit.MINUTES);

        timeCounter.startCounter();

        setupEditorListeners();
    }

    public static void doOKAction(String username, String password) {

        // Call the login method from LoginManager
        try {
            // Create an HttpClient instance
            HttpClient client = HttpClient.newHttpClient();

            // Prepare the data (URL encoding the snippet to be safely transmitted)
            Map<String, String> postData = new HashMap<>();
            postData.put("username", username);
            postData.put("password", password);
            Gson gson = new Gson();
            String json = gson.toJson(postData);

            // Build the HTTP POST request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://www.codeoasis.net:8005/clogin/"))  // Replace with your server's URL
                    .header("Content-Type", "application/json")  // Set Content-Type header
                    .POST(HttpRequest.BodyPublishers.ofString(json))       // Attach the request body
                    .build();

            // Send the request asynchronously
            HttpResponse response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                Map<String, Object> result = gson.fromJson(response.body().toString(), Map.class);
//                System.out.println(String.valueOf((int)Math.round(result.get("user_id"))));
                double userId = (double)result.get("user_id");
                int userIDInt = (int)userId;
                String userIDStr = String.valueOf(userIDInt);

                LoginManager.saveUserDataLocally(
                        result.get("username").toString(),
                        result.get("access").toString(),
                        userIDStr,
                        result.get("refresh").toString()
                );
//                showNotification("Login successfully!");
                notifyError(getCurrentProject(), "Login successfully!");
            } else {
//                showNotification("login failed! The error code:" + response.statusCode());
                notifyError(getCurrentProject(), "Login failed! The error code:" + response.statusCode());
            }



        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupEditorListeners() {
        ApplicationManager.getApplication().invokeLater(new Runnable(){
            public void run() {
                Disposable disposable = Disposer.newDisposable("WakaTimeListener");
                MessageBusConnection connection = ApplicationManager.getApplication().getMessageBus().connect();

                // save file
//                connection.subscribe(AppTopics.FILE_DOCUMENT_SYNC, new CustomSaveListener());

                // edit document
                EditorFactory.getInstance().getEventMulticaster().addDocumentListener(new CustomDocumentListener(), disposable);

                // mouse press
//                EditorFactory.getInstance().getEventMulticaster().addEditorMouseListener(new CustomEditorMouseListener(), disposable);

                // scroll document
//                EditorFactory.getInstance().getEventMulticaster().addVisibleAreaListener(new CustomVisibleAreaListener(), disposable);

                // caret moved
//                EditorFactory.getInstance().getEventMulticaster().addCaretListener(new CustomCaretListener(), disposable);

                // compiling
                // connection.subscribe(BuildManagerListener.TOPIC, new CustomBuildManagerListener());
                // connection.subscribe(CompilerTopics.COMPILATION_STATUS, new CustomBuildManagerListener());
            }
        });
    }

    public static boolean isAppActive() {
        return KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow() != null;
    }

    private static void showNotification(String content) {
        Notification notification = new Notification(
                "Snippet Save Notification",  // Notification Group ID (can be used to categorize notifications)
                "Code Oasis",               // Title of the notification
                content,                      // Content of the notification
                NotificationType.INFORMATION  // Type of notification (e.g., INFORMATION, WARNING, ERROR)
        );
        Notifications.Bus.notify(notification);
    }

    public static void notifyError(Project project, String content) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup("Snippet Save Notification")
                .createNotification("Code Oasis", content, NotificationType.INFORMATION)
                .notify(project);
    }

    public static void showUnLoginNotification() {
        Notification notification = new Notification(
                "Snippet Save Notification",  // Notification Group ID (can be used to categorize notifications)
                "Code Oasis",               // Title of the notification
                "Your account is not login to the Code Oasis.",                      // Content of the notification
                NotificationType.INFORMATION  // Type of notification (e.g., INFORMATION, WARNING, ERROR)
        );

        notification.addAction(new NotificationAction("Login") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                notification.expire(); // Optionally close the notification
                LoginDialog loginDialog = new LoginDialog();
                if (loginDialog.showAndGet()) {  // If the user clicked "OK"
                    String username = loginDialog.getUsername();
                    String password = loginDialog.getPassword();
                    doOKAction(username, password);
                } else {
                    showNotification("Login canceled!");
                    return;  // Exit without sending the snippet
                }
            }
        });
        Notifications.Bus.notify(notification);
    }

    public static VirtualFile getFile(Document document) {
        if (document == null) return null;
        FileDocumentManager instance = FileDocumentManager.getInstance();
        if (instance == null) return null;
        VirtualFile file = instance.getFile(document);
        return file;
    }
    public static Project getProject(Document document) {
        Editor[] editors = EditorFactory.getInstance().getEditors(document);
        if (editors.length > 0) {
            return editors[0].getProject();
        }
        return null;
    }

    public static Project getCurrentProject() {
        Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
        if (openProjects.length > 0) {
            return openProjects[0]; // Returns the first open project
        }
        return null;
    }

    public static boolean isProjectInitialized(Project project) {
        if (project == null) return true;
        return project.isInitialized();
    }
    public static LineStats getLineStats(Document document, Editor editor) {
        if (editor == null && document != null) {
            Project project = OasisActivator.getProject(document);
            if (project != null && project.isInitialized()) {
                editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
            }
        }

        if (editor != null) {
            if (document == null) {
                document = editor.getDocument();
            }
            for (Caret caret : editor.getCaretModel().getAllCarets()) {
                LineStats lineStats = new LineStats();
                if (document != null) {
                    lineStats.lineCount = document.getLineCount();
                }
                LogicalPosition position = caret.getLogicalPosition();
                lineStats.lineNumber = position.line + 1;
                lineStats.cursorPosition = position.column + 1;
                if (lineStats.isOK()) {
                    saveLineStats(document, lineStats);
                    return lineStats;
                }
            }
        }

        return OasisActivator.getLineStats(document);
    }
    public static void saveLineStats(Document document, LineStats lineStats) {
        VirtualFile file = OasisActivator.getFile(document);
        saveLineStats(file, lineStats);
    }

    public static void saveLineStats(VirtualFile file, LineStats lineStats) {
        if (file == null) return;
        if (!lineStats.isOK()) return;
        OasisActivator.lineStatsCache.put(file.getPath(), lineStats);
    }

    public static LineStats getLineStats(Document document) {
        if (document != null) {
            LineStats lineStats = new LineStats();
            lineStats.lineCount = document.getLineCount();
            Caret caret = CommonDataKeys.CARET.getData(DataManager.getInstance().getDataContext());
            LogicalPosition position = caret.getLogicalPosition();
            lineStats.lineNumber = position.line + 1;
            lineStats.cursorPosition = position.column + 1;
            if (lineStats.isOK()) {
                saveLineStats(document, lineStats);
                return lineStats;
            }
        }

        return OasisActivator.getLineStats(OasisActivator.getFile(document));
    }
    public static LineStats getLineStats(VirtualFile file) {
        Caret caret = CommonDataKeys.CARET.getData(DataManager.getInstance().getDataContext());
        LogicalPosition position = caret.getLogicalPosition();
        LineStats lineStats = new LineStats();
        Editor editor = caret.getEditor();
        if (editor != null) {
            Document document = editor.getDocument();
            if (document != null) {
                lineStats.lineCount = document.getLineCount();
            }
        }
        lineStats.lineNumber = position.line + 1;
        lineStats.cursorPosition = position.column + 1;
        if (lineStats.isOK()) {
            saveLineStats(file, lineStats);
            return lineStats;
        }

        if (file == null) return new LineStats();

        return OasisActivator.lineStatsCache.get(file.getPath());
    }

    public static BigDecimal getCurrentTimestamp() {
        return new BigDecimal(String.valueOf(System.currentTimeMillis() / 1000.0)).setScale(4, BigDecimal.ROUND_HALF_UP);
    }

    private static String getLanguage(final VirtualFile file) {
        FileType type = file.getFileType();
        if (type != null)
            return type.getName();
        return null;
    }

    public static void appendHeartbeat(final VirtualFile file, final Project project, final boolean isWrite, final LineStats lineStats) {
//        checkDebug();

        if (lineStats == null || !lineStats.isOK()) return;

//        if (OasisActivator.READY) {
//            updateStatusBarText();
//            if (project != null) {
//                StatusBar statusbar = WindowManager.getInstance().getStatusBar(project);
//                if (statusbar != null) statusbar.updateWidget("WakaTime");
//            }
//        }
//
//        if (!shouldLogFile(file)) return;



        String filePath = file.getPath();

        final BigDecimal time = OasisActivator.getCurrentTimestamp();
//        if (!isWrite && filePath.equals(OasisActivator.lastFile) && !enoughTimePassed(time)) {
//            return;
//        }

        OasisActivator.lastFile = filePath;
        OasisActivator.lastTime = time;

        final String projectName = project != null ? project.getName() : null;

        if (projectName == null) {
            return;
        }
        final String language = OasisActivator.getLanguage(file);

        ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
            public void run() {
                Heartbeat h = new Heartbeat();
                h.entity = filePath;
                h.timestamp = time;
                h.isWrite = isWrite;
                h.isUnsavedFile = !file.exists();
                h.project = projectName;
                h.language = language;
                h.isBuilding = OasisActivator.isBuilding;
                h.lineCount = lineStats.lineCount;
                h.lineNumber = lineStats.lineNumber;
                h.cursorPosition = lineStats.cursorPosition;
                h.logTime = getCurrentFormatTime();
                System.out.println(h.toString());
                heartbeatsQueue.add(h);
                System.out.println(heartbeatsQueue);
//                if (OasisActivator.isBuilding) setBuildTimeout();
            }
        });
    }

    public static String getCurrentFormatTime() {
        // Get the current date and time
        LocalDateTime now = LocalDateTime.now();
        // Define the format
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        // Convert to String
        String formattedDateTime = now.format(formatter);
        return formattedDateTime;
    }

    public void sendHeartbeat() {

        if (timeCounter.getSecondsElapsed() < SEND_FREQUENCY) {
            System.out.println("Sending heartbeat refused.");
            return;
        }

        ArrayList<Heartbeat> sendList = new ArrayList<Heartbeat>();
        sendList.addAll(heartbeatsQueue);
        if (heartbeatsQueue.size() > 0) {
            OasisActivator.lastLineCount = heartbeatsQueue.get(heartbeatsQueue.size()-1).lineCount;
            OasisActivator.lastLanguage = heartbeatsQueue.get(heartbeatsQueue.size()-1).language;
            OasisActivator.lastProject = heartbeatsQueue.get(heartbeatsQueue.size()-1).project;
        }
        heartbeatsQueue.clear();
        timeCounter.resetCounter();
        timeCounter.startCounter();
        ArrayList<SendHeartbeat> sendHeartbeats = new ArrayList<SendHeartbeat>();

        if (sendList.isEmpty()) {
            System.out.println("sendList");
            SendHeartbeat sendHeartbeat = new SendHeartbeat();
            sendHeartbeat.project = OasisActivator.lastProject;
            sendHeartbeat.entity = OasisActivator.lastFile;
            sendHeartbeat.language = OasisActivator.lastLanguage;
            sendHeartbeat.lineCount = OasisActivator.lastLineCount;
            sendHeartbeat.lineChange = 0;
            sendHeartbeat.codeTime = SEND_FREQUENCY;
            sendHeartbeat.activeCodeTime = 0;
            sendHeartbeats.add(sendHeartbeat);
        }

        for (Heartbeat heartbeat : sendList) {
            int existIndex = OasisActivator.fetchExistEntityIndex(sendHeartbeats, heartbeat.entity);
            if (existIndex == -1) {
                SendHeartbeat sendHeartbeat = new SendHeartbeat();
                sendHeartbeat.project = heartbeat.project;
                sendHeartbeat.entity = heartbeat.entity;
                sendHeartbeat.language = heartbeat.language;
                sendHeartbeat.lineCount = heartbeat.lineCount;
                sendHeartbeat.lineChange = 0;
                sendHeartbeat.codeTime = SEND_FREQUENCY;
                sendHeartbeat.activeCodeTime = 0;
                sendHeartbeat.timestamp = heartbeat.timestamp;
                sendHeartbeats.add(sendHeartbeat);
            } else {
                System.out.println(heartbeat.lineCount);
                System.out.println(sendHeartbeats.get(existIndex).lineCount);
                sendHeartbeats.get(existIndex).lineChange += (heartbeat.lineCount - sendHeartbeats.get(existIndex).lineCount);
                sendHeartbeats.get(existIndex).lineCount = heartbeat.lineCount;
                BigDecimal second_duration = heartbeat.timestamp.subtract(sendHeartbeats.get(existIndex).timestamp);
                if (second_duration.compareTo(FREQUENCY)==-1) {
                    sendHeartbeats.get(existIndex).activeCodeTime += second_duration.intValue();
                }
                sendHeartbeats.get(existIndex).timestamp = heartbeat.timestamp;
            }
        }

        for (SendHeartbeat sendHeartbeat : sendHeartbeats) {
            Map<String, Object> postData = new HashMap<>();
            postData.put("user_id", -1);
            postData.put("project", sendHeartbeat.project);
            postData.put("entity", sendHeartbeat.entity);
            postData.put("language", sendHeartbeat.language);
            postData.put("line_count", sendHeartbeat.lineCount);
            postData.put("line_change", sendHeartbeat.lineChange);
            postData.put("code_time", sendHeartbeat.codeTime);
            postData.put("active_code_time", sendHeartbeat.activeCodeTime);
            postData.put("add_time", getCurrentFormatTime());

            try {
                HttpClient client = HttpClient.newHttpClient();
                Gson gson = new Gson();
                String json = gson.toJson(postData);

                // Build the HTTP POST request
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://www.codeoasis.net:8005/api/jetbrain_log/"))  // Replace with your server's URL
                        .header("Content-Type", "application/json")  // Set Content-Type header
                        .header("Authorization", "Bearer " + LoginManager.getAccess())
                        .POST(HttpRequest.BodyPublishers.ofString(json))       // Attach the request body
                        .build();

                HttpResponse response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 201) {
                    Map<String, Object> result = gson.fromJson(response.body().toString(), Map.class);
                    System.out.println(result);
//                    showNotification("Code Snippet Save Successfully!");
                } else if (response.statusCode() == 401) {
                    showUnLoginNotification();
                } else {
//                    showNotification("Save failed! The error code:" + response.statusCode());
                }
            } catch (Exception xe) {
                xe.printStackTrace();
                showNotification("An error occurred: " + xe.getMessage());
            }
        }
    }

    public static int fetchExistEntityIndex(ArrayList<SendHeartbeat> sendList, String entityName) {
        for (int i = 0; i < sendList.size(); i++) {
            if (sendList.get(i).entity.equals(entityName)) {
                return i;
            }
        }
        return -1;
    }

    public static void debugException(Exception e) {
//        if (!log.isDebugEnabled()) return;
//        StringWriter sw = new StringWriter();
//        e.printStackTrace(new PrintWriter(sw));
//        String str = e.getMessage() + "\n" + sw.toString();
//        log.debug(str);
        System.out.println(e.toString());
    }

}
