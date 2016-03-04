package net.majorkernelpanic.spydroid;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Inet4Address;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;

import net.majorkernelpanic.http.BasicHttpServer;
import net.majorkernelpanic.http.ModifiedHttpContext;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentProducer;
import org.apache.http.entity.EntityTemplate;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import com.molko.net.rtsp.RtspConstants;
import com.molko.net.rtsp.client.RtspControlSdp;


import android.content.res.AssetManager;
import android.os.Handler;

public class RemoteServer {

	private Session session;
	private Handler handler;
	private String address;
	public RemoteServer(String address,int port ,Handler handler)  {
		//super(port, assetManager);
		//addRequestHandler("/spydroid.sdp*", new DescriptorRequestHandler(handler));
		this.handler = handler;
		if(address.startsWith("rtsp://"))
		{
			address=address.substring(address.indexOf("rtsp://".length()));
		}
		this.address = address;

		int pos = address.lastIndexOf("/");
		try {
			connect(new Socket(address.substring(0, pos), port));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public  void connect(Socket socket) throws IOException {
		
		// Stop all streams if a Session already exists
		if (session != null) {
			session.stopAll();
			session.flush();
		}
		
		// Create new Session
		session = new Session(socket.getInetAddress(),handler);
		
		// Parse URI and configure the Session accordingly 
		final String uri = "/spydroid.sdp";
		UriParser.parse(uri, session);
		
		 String sessionDescriptor = 
				"v=0\r\n" +
				//"o=- 15143872582342435176 15143872582342435176 IN IP4 "+socket.getLocalAddress().getHostName()+"\r\n"+
				"o=- "+System.currentTimeMillis()+"	"+System.currentTimeMillis()+" IN IP4 "+socket.getLocalAddress().getHostName()+"\r\n"+
				"s=CameraStream\r\n"+
				//"i=N/A\r\n"+
				"c=IN IP4 "+socket.getInetAddress().getHostAddress()+"\r\n"+
				//"t=0 0\r\n"+
				//"a=range:npt=now-"+
				//"a=isma-compliance:2,2.0,2"+
				//"a=tool:spydroid(cu)\r\n"+
				//"a=recvonly\r\n"+
				//"a=type:broadcast\r\n"+
				//"a=charset:UTF-8\r\n"+
				session.getSessionDescriptor();
		
		RtspControlSdp control = new RtspControlSdp("rtsp://"+address, sessionDescriptor, Session.user,Session.pass);	
		while (control.getState() != RtspConstants.READY) {
        	//Log.v("tag", String.valueOf(rtspControl.getState()));
		}
		session.setTrackDestinationPort(0, Integer.valueOf(control.serverport.get(0)));
		session.setTrackDestinationPort(1, Integer.valueOf(control.serverport.get(2)));
		// Start all streams associated to the Session
		session.startAll();
		
	}	
}

