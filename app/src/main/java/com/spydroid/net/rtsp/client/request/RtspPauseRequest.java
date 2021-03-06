package com.spydroid.net.rtsp.client.request;

import java.net.URISyntaxException;

import com.spydroid.net.rtsp.client.header.SessionHeader;


public class RtspPauseRequest extends RtspRequest {

	public RtspPauseRequest() {
	}
	
	public RtspPauseRequest(String messageLine) throws URISyntaxException {
		super(messageLine);
	}

	@Override
	public byte[] getBytes() throws Exception {
		getHeader(SessionHeader.NAME);
		return super.getBytes();
	}

}
