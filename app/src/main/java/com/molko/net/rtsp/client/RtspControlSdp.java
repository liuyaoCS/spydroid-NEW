package com.molko.net.rtsp.client;

import java.net.InetAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import com. molko.net.rtsp.RtspConstants;
import com. molko.net.rtsp.RtspConstants.VideoEncoder;
import com. molko.net.rtsp.client.api.Request;
import com. molko.net.rtsp.client.api.RequestListener;
import com. molko.net.rtsp.client.api.Response;
import com. molko.net.rtsp.client.header.RtspHeader;
import com. molko.net.rtsp.client.header.SessionHeader;
import com. molko.net.rtsp.client.message.RtspDescriptor;
import com. molko.net.rtsp.client.message.RtspMedia;
import com. molko.net.rtsp.client.transport.TCPTransport;

import android.text.InputFilter.LengthFilter;
import android.util.Log;

public class RtspControlSdp implements RequestListener {

	// reference to the RTSP client
	private RtspClient client;

	// flag to indicate whether there is a connection
	// established to a remote RTSP server
	private boolean connected = false;

	// reference to the RTSP server URI
	private URI uri;
	public String realm="";
	public String nonce = "";
	private int port;
	public String username ="";
	public String password ="";
	private String resource;
	public VideoEncoder rtspVideoEncoder;
	// reference to the SDP file returned as a response
	// to a DESCRIBE request
	public String sdp;
	private RtspDescriptor rtspDescriptor;
	public String fullResponse="";
	String fullReq="";
	private int state;
	public ArrayList<String> serverport = new ArrayList<String>();
	public String session;

	private int track=1;

	/*private RtpSocket rtpSocket1;

	private RtpSocket rtpSocket2;
*/
	/**
	 * This constructor is invoked with an uri that
	 * describes the server uri and also a certain
	 * resource
	 * @param rtspVideoEncoder 
	 * @param password 
	 * @param username 
	 */

	public RtspControlSdp(String uri, String sdp, String username, String password) {	
		this.username = username;
		this.password = password;
		this.sdp = sdp;
		int pos = uri.lastIndexOf("/");

		try {

			this.uri      = new URI(uri.substring(0, pos));
			this.resource = uri.substring(pos+1);

			// initialize the RTSP communication
			this.client = new RtspClient();
			this.client.setTransport(new TCPTransport());
			
			this.client.setRequestListener(this);			
			this.state = RtspConstants.UNDEFINED;
			this.client.username = username;
			this.client.password = password;
			// the OPTIONS request is used to invoke and
			// test the connection to the RTSP server,
			// specified with the URI provided
			this.client.announce(this.uri, this.resource,sdp);
			//this.client.announce(this.uri, this.resource,rtspVideoEncoder,this.username,this.password,"","Streaming Server");

			//this.client.options("*", this.uri);

		} catch (Exception e) {
			
			if (this.client != null) {
				onError(this.client, e);
				
			} else {
				e.printStackTrace();
				
			}
			
		}
		
	}

	public void play() {

		if ((this.client == null) || (this.connected == false)) return;

		if (this.state == RtspConstants.READY) {
			this.client.play();		
		}
	
	}
	
	public void pause() {

		if ((this.client == null) || (this.connected == false)) return;

		if (this.state == RtspConstants.PLAYING) {
			this.client.pause();		
		}

	}
	
	public void stop() {
		
		if ((this.client == null) || (this.connected == false)) return;
		
		// send TEARDOWN request
		this.client.teardown();
		
	}

	public boolean isConnected() {
		return this.connected;
	}
	
	public int getState() {
		return this.state;
	}
	
	public int getClientPort() {
		return this.port;
	}
	
	public RtspDescriptor getDescriptor() {
		return this.rtspDescriptor;
	}
	
	@Override
	public void onError(RtspClient client, Throwable error) {

		if ((this.client != null) && (this.connected == true)) {
			this.client.teardown();
		}
 		
		this.state = RtspConstants.UNDEFINED;
		this.connected = false;
		
		this.client = null;
		
	}

	// register SDP file
	public void onDescriptor(RtspClient client, String descriptor) {
		this.rtspDescriptor = new RtspDescriptor(descriptor);		
	}

	public void onFailure(RtspClient client, Request request, Throwable cause) {

		if ((this.client != null) && (this.connected == true)) {
			this.client.teardown();
		}
 		
		this.state = RtspConstants.UNDEFINED;
		this.connected = false;
		
		this.client = null;
		
	}
	public void authenticate(Response response) throws Exception
	{
		String authen = response.getHeader("WWW-Authenticate").getRawValue().substring(response.getHeader("WWW-Authenticate").getRawValue().indexOf("Digest")+"Digest ".length());
		String list[] = authen.split(",");
        Dictionary <String, String> d = new Hashtable<String, String>();
		for(int i=0;i<list.length;++i)
		{
			d.put(list[i].split("=")[0].trim(), list[i].split("\"")[1].trim());
		}
		this.realm =  (String)d.get("realm");
		this.nonce = (String)d.get("nonce");
		this.client.realm = this.realm;
		this.client.nonce = this.nonce;
	}
	@SuppressWarnings("unused")
	public void onSuccess(RtspClient client, Request request, Response response) {
		fullResponse+=request+"\r\n"+response+"\r\n";
		fullReq+=request+"\r\n";
		Log.d("RTSP", request.toString());
		Log.d("RTSP", response.toString());

		try {
			try{
			
			this.client.setSession((SessionHeader) response.getHeader("Session"));
			}catch(Exception e){
				
			}
			if ((this.client != null) && (response.getStatusCode() == 200)) {
				
				Request.Method method = request.getMethod();
				if (method == Request.Method.OPTIONS) {
					// the response to an OPTIONS request
					this.connected = true;
					
					// send DESCRIBE request
					//this.client.describe(this.uri, this.resource);
					this.port = 6970;
					this.client.setup(this.uri, this.port, this.resource,String.valueOf(track));

				} else if (method == Request.Method.DESCRIBE) {
					
					// set state to INIT
					this.state = RtspConstants.INIT;
					
					/* 
					 * onSuccess is called AFTER onDescriptor method;
					 * this implies, that a media resource is present
					 * with a certain client port specified by the RTSP
					 * server
					 */
					
					RtspMedia video = this.rtspDescriptor.getFirstVideo();
					if (video != null) {
					
						this.port = Integer.valueOf(video.getTransportPort());
						
						// send SETUP request
						this.client.setup(this.uri, this.port, this.resource,"0");
						
					}

				} else if (method == Request.Method.SETUP) {
					// set state to READY
					String t =  response.getHeader("Transport").getRawValue();
					String ports = t.substring(t.indexOf("server_port=")+"server_port=".length());
					serverport.add(ports.substring(0,ports.indexOf("-")));
					serverport.add(ports.substring(ports.indexOf("-")+1,ports.indexOf("-")+5));
					++track;
					if(track==2)
					{
						this.port +=2;
						this.client.setup(this.uri, this.port, this.resource,String.valueOf(track));
					}
					else
					{
						this.client.record();
					}


				} else if (method == Request.Method.PLAY) {
					
					// set state to PLAYING
					this.state = RtspConstants.PLAYING;

				} else if (method == Request.Method.PAUSE) {
					
					// set state to READY
					this.state = RtspConstants.READY;

				} else if (method == Request.Method.TEARDOWN) {

					this.connected = false;
					
					// set state to UNDEFINED
					this.state = RtspConstants.UNDEFINED;

				}
				else if (method == Request.Method.ANNOUNCE) {

	
					this.client.options("*", this.uri);

				}
				else if (method == Request.Method.RECORD)
				{
					/*this.rtpSocket1 = new RtpSocket(InetAddress.getByName(this.uri.getHost()), Integer.valueOf(serverport.get(0)));
					this.rtpSocket2 = new RtpSocket(InetAddress.getByName(this.uri.getHost()), Integer.valueOf(serverport.get(2)));

    				// this RTP socket is registered as RTP receiver to also
    				// receive the streaming video of this device
    				RtpSender.getInstance().addReceiver(this.rtpSocket2);*/
					this.state = RtspConstants.READY;

				}
			
			} else if (response.getStatusCode() == 401)
			{
				Request.Method method = request.getMethod();
				authenticate(response);

				this.connected = true;
				//String authen = response.getHeader("WWW-Authenticate").getRawValue().substring(response.getHeader("WWW-Authenticate").getRawValue().indexOf("Digest")+"Digest ".length());
				//this.realm = authen.substring(authen.indexOf("realm=\"")+"realm=\"".length(),authen.indexOf("\","));
				//this.nonce = authen.substring(authen.indexOf("nonce=\"")+"nonce=\"".length(),authen.length()-1);
				
				//this.client.announce(this.uri, this.resource,rtspVideoEncoder);
				 if (method == Request.Method.ANNOUNCE) {

						this.client.announce(this.uri, this.resource,sdp);

				}else if (method == Request.Method.SETUP) {

					//this.client.setup(this.uri, this.port, this.resource);
				}
			}
			else
			{
				
			}
			
		} catch (Exception e) {
			onError(this.client, e);
			
		}
		
	}
}
