package com.groupon.seleniumgridextras.loggers;

import com.groupon.seleniumgridextras.utilities.FileIOUtility;
import com.groupon.seleniumgridextras.utilities.TimeStampUtility;
import com.groupon.seleniumgridextras.utilities.json.JsonParserWrapper;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SessionHistoryLog {

    public static final String NO_HISTORY_FOR_NODE = "[]";
    private static Logger logger = Logger.getLogger(SessionHistoryLog.class);
    private static File outputDir;
    private static Map<String, NodeSessionHistory> history;

    public static void setOutputDir(File dir) {
        outputDir = dir;
    }

    public static File getOutputDir() {
        return outputDir;
    }

    public static void newSession(String node, Map session) {
        initialize();

        String logFile = getLogFileForNode(node);
        File outputFile = new File(outputDir, logFile);

        logger.info("Registering new session to file " + outputFile.getAbsolutePath());
        logger.debug(session);

        if (!history.containsKey(logFile) || history.get(logFile).timeToRotateLog()) {
            history.put(logFile, new NodeSessionHistory(outputFile));
        }

        history.get(logFile).addNewSession(session);
        history.get(logFile).backupToFile();
    }

    protected static void resetMemory() {
        if (history == null) {
            history = new HashMap<String, NodeSessionHistory>();
        } else {
            //delete all reference to objects to make objects ready for garbage collection
            //Doing it this way, just in case the history map is refered to anywhere, and does not get garbage collected
            //This is done in hopes of reducing memory bloat, if a JVM expert is reading this, please fix it!
            //-Love, Dima
            history.clear();

        }
    }

    public static String getAllHistory(){
        initialize();
        Map<String, List<Map>> allHistory = new HashMap<String, List<Map>>();

        for (String sessionId : history.keySet()){
            allHistory.put(sessionId, history.get(sessionId).getSessions());
        }

        return JsonParserWrapper.prettyPrintString(allHistory);
    }

    public static String getHistory(String node, String day, String month, String year) {
        initialize();
        return getHistoryFromMemoryOrFile(getLogFileForNode(node, day, month, year));
    }

    public static String getHistory(String node) {
        initialize();
        return getHistoryFromMemoryOrFile(getLogFileForNode(node));
    }

    protected static String getHistoryFromMemoryOrFile(String logFile){
        if (history.containsKey(logFile)) {
            return history.get(logFile).toJson();
        } else {
            return getHistoryFromFile(new File(outputDir, logFile));
        }
    }

    protected static String getHistoryFromFile(File inputFile) {
        try {
            String fileString = FileIOUtility.getAsString(inputFile);
            return JsonParserWrapper.prettyPrintString(JsonParserWrapper.toList(fileString));
        } catch (FileNotFoundException e) {
            return NO_HISTORY_FOR_NODE;
        }
    }

    protected static String getLogFileForNode(String node, String day, String month, String year) {
        return node + "_" + day + "_" + month + "_" + year + ".log";
    }

    protected static String getLogFileForNode(String node) {
        return node + "_" + TimeStampUtility.osFriendlyTimestamp() + ".log";
    }


    protected static void createOutputDir() {
        outputDir.mkdir();
    }

    protected static void initialize() {
        if (!outputDir.exists()) {
            createOutputDir();
        }

        if (history == null) {
            resetMemory();
        }
    }

}
