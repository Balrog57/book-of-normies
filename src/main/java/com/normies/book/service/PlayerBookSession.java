package com.normies.book.service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player state for cycling choice arguments and pending confirmations.
 */
public final class PlayerBookSession {
    private static final Map<UUID, PlayerBookSession> SESSIONS = new ConcurrentHashMap<>();

    private final Map<String, Integer> choiceIndexes = new HashMap<>();
    private String pendingCommand;
    private int resumePage;

    private PlayerBookSession() {}

    public static PlayerBookSession of(UUID playerId) {
        return SESSIONS.computeIfAbsent(playerId, id -> new PlayerBookSession());
    }

    public static void clear(UUID playerId) {
        SESSIONS.remove(playerId);
    }

    public int cycleChoice(String commandName, int choiceCount) {
        if (choiceCount <= 0) {
            return 0;
        }
        int next = (choiceIndexes.getOrDefault(commandName, -1) + 1) % choiceCount;
        choiceIndexes.put(commandName, next);
        return next;
    }

    public int getChoiceIndex(String commandName) {
        return choiceIndexes.getOrDefault(commandName, 0);
    }

    public String getSelectedChoice(String commandName, java.util.List<String> choices) {
        if (choices == null || choices.isEmpty()) {
            return "";
        }
        int idx = Math.floorMod(getChoiceIndex(commandName), choices.size());
        return choices.get(idx);
    }

    public void setPendingCommand(String command) {
        this.pendingCommand = command;
    }

    public String getPendingCommand() {
        return pendingCommand;
    }

    public void clearPending() {
        this.pendingCommand = null;
    }

    public void setResumePage(int page) {
        this.resumePage = Math.max(0, page);
    }

    public int getResumePage() {
        return resumePage;
    }
}
