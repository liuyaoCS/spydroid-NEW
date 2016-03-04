package com.molko.net.rtsp.client.request;

import java.net.URISyntaxException;

import com. molko.net.rtsp.client.header.SessionHeader;


public class RtspAnnounceRequest extends RtspRequest {

	public RtspAnnounceRequest() {
	}
	
	public RtspAnnounceRequest(String messageLine) throws URISyntaxException {
		super(messageLine);
	}

	@Override
	public byte[] getBytes() throws Exception {
		getHeader(SessionHeader.NAME);
		return super.getBytes();
	}

}
