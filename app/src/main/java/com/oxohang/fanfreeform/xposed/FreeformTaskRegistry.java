package com.oxohang.fanfreeform.xposed;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

final class FreeformTaskRegistry {
    enum State { LAUNCHING, NORMAL, SUSPENDED }

    static final class Record {
        final int taskId;
        String packageName;
        State state;
        long suspendedOrder;
        long graceUntil;

        Record(int taskId, String packageName, State state) {
            this.taskId = taskId;
            this.packageName = packageName;
            this.state = state;
        }
    }

    private final LinkedHashMap<Integer, Record> records = new LinkedHashMap<>();
    private long nextSuspendedOrder;

    Record get(int taskId) {
        return records.get(taskId);
    }

    Record put(int taskId, String packageName, State state) {
        Record record = records.get(taskId);
        if (record == null) {
            record = new Record(taskId, packageName, state);
            records.put(taskId, record);
        } else {
            if (packageName != null && !packageName.isEmpty()) record.packageName = packageName;
            record.state = state;
        }
        if (state == State.SUSPENDED) {
            markSuspended(record);
        } else if (state == State.NORMAL) {
            record.suspendedOrder = 0;
            record.graceUntil = 0;
        }
        return record;
    }

    void markNormal(int taskId) {
        Record record = records.get(taskId);
        if (record == null) return;
        record.state = State.NORMAL;
        record.suspendedOrder = 0;
        record.graceUntil = 0;
    }

    void markSuspended(int taskId) {
        Record record = records.get(taskId);
        if (record != null) markSuspended(record);
    }

    private void markSuspended(Record record) {
        if (record.suspendedOrder == 0) record.suspendedOrder = ++nextSuspendedOrder;
        record.state = State.SUSPENDED;
        record.graceUntil = 0;
    }

    Record remove(int taskId) {
        return records.remove(taskId);
    }

    boolean contains(int taskId) {
        return records.containsKey(taskId);
    }

    int size() {
        return records.size();
    }

    List<Integer> taskIds() {
        return new ArrayList<>(records.keySet());
    }
}
