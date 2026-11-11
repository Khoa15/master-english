package com.masterenglish.app.data.remote.dto;

/** Server acknowledgement for a pushed payload. */
public final class SyncAck {

    public boolean accepted;
    public long serverTime;
}
