package com.molko.net.rtsp.server.response;

public class RtspAnnounceResponse extends RtspResponse {

    public RtspAnnounceResponse(int cseq) {           
    	super(cseq);
    }

    protected void generateBody() {
    }

}
