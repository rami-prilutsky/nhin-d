/* 
Copyright (c) 2010, NHIN Direct Project
All rights reserved.

Authors:
   Greg Meyer      gm2552@cerner.com
 
Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer 
in the documentation and/or other materials provided with the distribution.  Neither the name of the The NHIN Direct Project (nhindirect.org). 
nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS 
BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE 
GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, 
STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF 
THE POSSIBILITY OF SUCH DAMAGE.
*/

package org.nhindirect.gateway.smtp.dsn.impl;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.mail.Address;
import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;

import org.apache.commons.io.IOUtils;
import org.apache.mailet.Mailet;
import org.nhindirect.common.mail.dsn.DSNFailureTextBodyPartGenerator;
import org.nhindirect.common.mail.dsn.DSNGenerator;
import org.nhindirect.common.mail.dsn.DSNMessageHeaders;
import org.nhindirect.common.mail.dsn.DSNRecipientHeaders;
import org.nhindirect.common.mail.dsn.DSNStandard.DSNAction;
import org.nhindirect.common.mail.dsn.DSNStandard.DSNStatus;
import org.nhindirect.common.mail.dsn.DSNStandard.MtaNameType;
import org.nhindirect.common.tx.model.Tx;
import org.nhindirect.common.tx.model.TxDetail;
import org.nhindirect.common.tx.model.TxDetailType;
import org.nhindirect.gateway.smtp.dsn.DSNCreator;
import org.nhindirect.stagent.NHINDAddress;
import org.nhindirect.stagent.NHINDAddressCollection;

public abstract class AbstractDSNCreator implements DSNCreator 
{
	protected Mailet mailet;
	protected DSNGenerator generator;
	protected String postmasterMailbox;
	protected String reportingMta;
	protected DSNFailureTextBodyPartGenerator textGenerator;
	protected String dsnStatus;
	
	@Override
	public MimeMessage createDSNFailure(Tx tx, NHINDAddressCollection failedRecipeints) throws MessagingException
	{
	    InternetAddress originalSender = null;
	    String originalSubject = "";
	    InternetAddress postmaster = null;
	    String originalMessageId = "";
	    Enumeration<Header> fullMessageHeaders = null;
	    
	    final List<DSNRecipientHeaders> recipientDSNHeaders = new ArrayList<DSNRecipientHeaders>();
	    final List<Address> failedRecipAddresses = new ArrayList<Address>();
	    
	    final TxDetail sender = tx.getDetail(TxDetailType.FROM);
	    if (sender != null)
	    {
	    	originalSender = new InternetAddress(sender.getDetailValue());
	    	postmaster = new InternetAddress(postmasterMailbox + "@" + getAddressDomain(originalSender));
	    }
	    
	    final TxDetail subject = tx.getDetail(TxDetailType.SUBJECT);
	    if (subject != null)
	    	originalSubject = subject.getDetailValue();
	    
	    
	    for (NHINDAddress incompleteRecip : failedRecipeints)
	    {
	    	
	    	DSNRecipientHeaders dsnRecipHeaders = 
	    			new DSNRecipientHeaders(DSNAction.FAILED, 
	    			DSNStatus.getStatus(DSNStatus.PERMANENT, dsnStatus), incompleteRecip);
	    	
	    	recipientDSNHeaders.add(dsnRecipHeaders);
	    	failedRecipAddresses.add(incompleteRecip);
	    }
	    
	    ///CLOVER:OFF
	    final TxDetail origMessId = tx.getDetail(TxDetailType.MSG_ID);
	    if (origMessId != null)
	    	originalMessageId = origMessId.getDetailValue();
	    ///CLOVER:ON
	    
	    final DSNMessageHeaders messageDSNHeaders = new DSNMessageHeaders(reportingMta, originalMessageId, MtaNameType.DNS);
	    
	    final TxDetail fullHeaders = tx.getDetail(TxDetailType.MSG_FULL_HEADERS);
	    if (fullHeaders != null)
	    	fullMessageHeaders = this.convertStringToHeaders(fullHeaders.getDetailValue());
	    
	    final MimeBodyPart textBodyPart = textGenerator.generate(originalSender, failedRecipAddresses, fullMessageHeaders);
	    
	    return generator.createDSNMessage(originalSender, originalSubject, postmaster, recipientDSNHeaders, messageDSNHeaders, textBodyPart);

	}
	
    /**
     * Get the domain of an email address
     * @param theAddress
     * @return
     */
    ///CLOVER:OFF
    protected String getAddressDomain(InternetAddress theAddress)
    {
    	String retVal = "";
    	
    	// remove any extra information such as < and >
    	String address = theAddress.getAddress();
    	int index;
    	if ((index = address.indexOf('<')) > -1)
    		address = address.substring(index + 1);
    	
    	if ((index = address.indexOf('>')) > -1)
    		address = address.substring(0, index); 
    	
    	index = address.indexOf("@");
    	if (index >= 0)
    		retVal = address.substring(index + 1);
    	
    	return retVal;
    }
    ///CLOVER:ON
    
    /**
     * Converts a string to an enumeration of Internet headers.
     * @param asString The string to convert.
     * @return
     */
    ///CLOVER:OFF
    @SuppressWarnings("unchecked")
	protected Enumeration<Header> convertStringToHeaders(String asString)
    {
    	final InputStream str = IOUtils.toInputStream(asString);	
    	try
    	{
    	   	InternetHeaders headers = new InternetHeaders(str);
    	
    	   	return headers.getAllHeaders();
    	}
    	catch (Exception e)
    	{
    		// log warning
    	}
    	finally
    	{
        	IOUtils.closeQuietly(str);    		
    	}
    	
    	return null;
    }    
    ///CLOVER:ON
}
