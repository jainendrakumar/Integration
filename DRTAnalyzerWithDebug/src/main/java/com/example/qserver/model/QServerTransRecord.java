package com.example.qserver.model;

import java.time.LocalDateTime;

/**
 * QServerTransRecord represents a single QServer transaction log entry.
 * It encapsulates fields such as transaction ID, thread information, timestamps,
 * processing metrics, and other related details.
 */
public class QServerTransRecord {
    // Identifiers
    private String transactionId;
    private String transactionKind;
    private String threadName;
    private String threadId;

    // Timestamps
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    // Durations in seconds
    private double length;
    private double waitingTime;
    private double procTime;
    private double funcTime;
    private double dbTime;
    private double deltaSetFinalize;
    private double ioDef;
    private double notifications;
    private double mrSend;
    private double streamTime;

    // Counters
    private double nrDatasets;
    private double size;
    private double constructions;
    private double destructions;
    private double changes;

    // Additional Information
    private String clientId;
    private String ipClient;
    private String username;
    private String actionElementType;
    private String actionElementName;
    private String actionElementKey;
    private String description;
    private String messageId;
    private String lockProfile;

    // Memory usage (in KB)
    private double procMem;
    private double funcMem;
    private double dbMem;

    // Getters and setters for all fields

    public String getTransactionId() {
        return transactionId;
    }
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }
    public String getTransactionKind() {
        return transactionKind;
    }
    public void setTransactionKind(String transactionKind) {
        this.transactionKind = transactionKind;
    }
    public String getThreadName() {
        return threadName;
    }
    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }
    public String getThreadId() {
        return threadId;
    }
    public void setThreadId(String threadId) {
        this.threadId = threadId;
    }
    public LocalDateTime getStartTime() {
        return startTime;
    }
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
    public LocalDateTime getEndTime() {
        return endTime;
    }
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
    public double getLength() {
        return length;
    }
    public void setLength(double length) {
        this.length = length;
    }
    public double getWaitingTime() {
        return waitingTime;
    }
    public void setWaitingTime(double waitingTime) {
        this.waitingTime = waitingTime;
    }
    public double getProcTime() {
        return procTime;
    }
    public void setProcTime(double procTime) {
        this.procTime = procTime;
    }
    public double getFuncTime() {
        return funcTime;
    }
    public void setFuncTime(double funcTime) {
        this.funcTime = funcTime;
    }
    public double getDbTime() {
        return dbTime;
    }
    public void setDbTime(double dbTime) {
        this.dbTime = dbTime;
    }
    public double getDeltaSetFinalize() {
        return deltaSetFinalize;
    }
    public void setDeltaSetFinalize(double deltaSetFinalize) {
        this.deltaSetFinalize = deltaSetFinalize;
    }
    public double getIoDef() {
        return ioDef;
    }
    public void setIoDef(double ioDef) {
        this.ioDef = ioDef;
    }
    public double getNotifications() {
        return notifications;
    }
    public void setNotifications(double notifications) {
        this.notifications = notifications;
    }
    public double getMrSend() {
        return mrSend;
    }
    public void setMrSend(double mrSend) {
        this.mrSend = mrSend;
    }
    public double getStreamTime() {
        return streamTime;
    }
    public void setStreamTime(double streamTime) {
        this.streamTime = streamTime;
    }
    public double getNrDatasets() {
        return nrDatasets;
    }
    public void setNrDatasets(double nrDatasets) {
        this.nrDatasets = nrDatasets;
    }
    public double getSize() {
        return size;
    }
    public void setSize(double size) {
        this.size = size;
    }
    public double getConstructions() {
        return constructions;
    }
    public void setConstructions(double constructions) {
        this.constructions = constructions;
    }
    public double getDestructions() {
        return destructions;
    }
    public void setDestructions(double destructions) {
        this.destructions = destructions;
    }
    public double getChanges() {
        return changes;
    }
    public void setChanges(double changes) {
        this.changes = changes;
    }
    public String getClientId() {
        return clientId;
    }
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
    public String getIpClient() {
        return ipClient;
    }
    public void setIpClient(String ipClient) {
        this.ipClient = ipClient;
    }
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public String getActionElementType() {
        return actionElementType;
    }
    public void setActionElementType(String actionElementType) {
        this.actionElementType = actionElementType;
    }
    public String getActionElementName() {
        return actionElementName;
    }
    public void setActionElementName(String actionElementName) {
        this.actionElementName = actionElementName;
    }
    public String getActionElementKey() {
        return actionElementKey;
    }
    public void setActionElementKey(String actionElementKey) {
        this.actionElementKey = actionElementKey;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public String getMessageId() {
        return messageId;
    }
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
    public String getLockProfile() {
        return lockProfile;
    }
    public void setLockProfile(String lockProfile) {
        this.lockProfile = lockProfile;
    }
    public double getProcMem() {
        return procMem;
    }
    public void setProcMem(double procMem) {
        this.procMem = procMem;
    }
    public double getFuncMem() {
        return funcMem;
    }
    public void setFuncMem(double funcMem) {
        this.funcMem = funcMem;
    }
    public double getDbMem() {
        return dbMem;
    }
    public void setDbMem(double dbMem) {
        this.dbMem = dbMem;
    }

    @Override
    public String toString() {
        return "QServerTransRecord{" +
                "transactionId='" + transactionId + '\'' +
                ", threadId='" + threadId + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", procTime=" + procTime +
                ", waitingTime=" + waitingTime +
                '}';
    }
}
