/** 
 * Copyright (c) 2006, James Seigel, Calgary, AB., Canada
 * Copyright (c) 2003,2004, Stefan Haustein, Oberhausen, Rhld., Germany
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The  above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE. 
 */

package org.ksoap2.mypack.transport;

import java.io.*;

import org.ksoap2.*;
import org.ksoap2.serialization.SoapSerializationEnvelope;
//import org.kxml2.io.*;
import org.xmlpull.v1.*;

import android.util.Log;

/**
 * Abstract class which holds common methods and members that are used by the
 * transport layers. This class encapsulates the serialization and
 * deserialization of the soap messages, leaving the basic communication
 * routines to the subclasses.
 */
abstract public class Transport {
	private static final String TAG = "Transport";

    protected String url;
    /** Set to true if debugging */
    public boolean debug;
    /** String dump of request for debugging. */
    public String requestDump;
    /** String dump of response for debugging */
    public String responseDump;
    private String xmlVersionTag = "";

    public Transport() {
    }

    public Transport(String url) {
        this.url = url;
    }

    /**
     * Sets up the parsing to hand over to the envelope to deserialize.
     */
    protected void parseSoapResponse(SoapSerializationEnvelope envelope, InputStream is) throws XmlPullParserException, IOException {
    	//XmlPullParserFactory xppf = new XmlPullParserFactory();
    	XmlPullParser xp =  XmlPullParserFactory.newInstance().newPullParser();// new KXmlSerializer();
        xp.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
        xp.setInput(is, null);
        envelope.parseSoap(envelope, xp);
    }
    

    /**
     * Sets up the parsing to hand over to the envelope to deserialize.
     */
    protected void parseXmlResponse(SoapSerializationEnvelope envelope, InputStream is, String ename) throws XmlPullParserException, IOException {
    	XmlPullParser xp =  XmlPullParserFactory.newInstance().newPullParser();// new KXmlSerializer();
        xp.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
        xp.setInput(is, null);
        envelope.parseXml(envelope, xp, ename);
    }

    /**
     * Serializes the request.
     */
    protected byte[] createRequestData(SoapEnvelope envelope, String namespace) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bos.write(xmlVersionTag.getBytes());
        XmlSerializer xw;
        
        try {
			xw = XmlPullParserFactory.newInstance().newSerializer();

			xw.setOutput(bos, null);

//        envelope.write(xw);
			((SoapSerializationEnvelope)envelope).write(xw, namespace);
              
			xw.flush();
		} catch (XmlPullParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}// new KXmlSerializer();
        bos.write('\r');
        bos.write('\n');
        bos.flush();
       // Log.v(TAG, "createRequestData : " + bos);

        return bos.toByteArray();
    }

    /**
     * Set the target url.
     * 
     * @param url
     *            the target url.
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Sets the version tag for the outgoing soap call. Example <?xml
     * version=\"1.0\" encoding=\"UTF-8\"?>
     * 
     * @param tag
     *            the xml string to set at the top of the soap message.
     */
    public void setXmlVersionTag(String tag) {
        xmlVersionTag = tag;
    }

    /**
     * Attempts to reset the connection.
     */
    public void reset() {
    }

    /**
     * Perform a soap call with a given namespace and the given envelope.
     * 
     * @param targetNamespace
     *            the namespace with which to perform the call in.
     * @param envelope
     *            the envelope the contains the information for the call.
     */
    abstract public void call(String targetNamespace, SoapSerializationEnvelope envelope) throws IOException, XmlPullParserException;

}
