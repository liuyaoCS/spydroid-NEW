package com.spydroid.net.rtsp.server.response;

public class RtspPauseResponse extends RtspResponse {

    public RtspPauseResponse(int cseq) {
        super(cseq);
    }

    protected void generateBody() {
    }

}
